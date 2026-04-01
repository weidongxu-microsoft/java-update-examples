package com.contoso.messaging;

import com.microsoft.azure.servicebus.IMessage;
import com.microsoft.azure.servicebus.IMessageReceiver;
import com.microsoft.azure.servicebus.IMessageSender;
import com.microsoft.azure.servicebus.primitives.ServiceBusException;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

public class MessageRouter {

    private final IMessageReceiver receiver;
    private final Map<String, IMessageSender> routeTable;
    private final ErrorClassifier errorClassifier;

    public MessageRouter(IMessageReceiver receiver, ErrorClassifier errorClassifier) {
        this.receiver = receiver;
        this.routeTable = new ConcurrentHashMap<>();
        this.errorClassifier = errorClassifier;
    }

    public void addRoute(String label, IMessageSender sender) {
        routeTable.put(label, sender);
    }

    public CompletableFuture<IMessage> receiveAndRoute() {
        return receiver.receiveAsync()
            .thenCompose(msg -> {
                if (msg == null) {
                    return CompletableFuture.completedFuture((IMessage) null);
                }
                String label = msg.getLabel();
                IMessageSender target = routeTable.get(label);
                if (target == null) {
                    return receiver.abandonAsync(msg.getLockToken())
                        .thenApply(v -> msg);
                }
                return target.sendAsync(msg)
                    .thenCompose(v -> receiver.completeAsync(msg.getLockToken()))
                    .thenApply(v -> msg);
            })
            .exceptionally(ex -> {
                errorClassifier.classify(ex);
                return null;
            });
    }

    public CompletableFuture<List<IMessage>> receiveAndRouteBatch(
            int maxMessages, MessageTransformer transformer) {
        return receiver.receiveBatchAsync(maxMessages)
            .thenCompose(messages -> {
                if (messages == null || messages.isEmpty()) {
                    return CompletableFuture.completedFuture(Collections.<IMessage>emptyList());
                }
                List<CompletableFuture<IMessage>> futures = new ArrayList<>();
                for (final IMessage msg : messages) {
                    CompletableFuture<IMessage> pipeline = CompletableFuture
                        .supplyAsync(() -> {
                            try {
                                return transformer.transform(msg);
                            } catch (ServiceBusException e) {
                                throw new RuntimeException(e);
                            }
                        })
                        .thenCompose(transformed -> {
                            String label = transformed.getLabel();
                            IMessageSender target = routeTable.get(label);
                            if (target != null) {
                                return target.sendAsync(transformed)
                                    .thenCompose(v -> receiver.completeAsync(msg.getLockToken()))
                                    .thenApply(v -> transformed);
                            }
                            return receiver.abandonAsync(msg.getLockToken())
                                .thenApply(v -> transformed);
                        });
                    futures.add(pipeline);
                }
                return CompletableFuture
                    .allOf(futures.toArray(new CompletableFuture[0]))
                    .thenApply(v -> {
                        List<IMessage> results = new ArrayList<>();
                        for (CompletableFuture<IMessage> f : futures) {
                            results.add(f.join());
                        }
                        return results;
                    });
            })
            .exceptionally(ex -> {
                errorClassifier.classify(ex);
                return Collections.emptyList();
            });
    }

    public IMessage receiveAndTransform(MessageTransformer transformer)
            throws ServiceBusException, InterruptedException {
        IMessage message = receiver.receive();
        if (message == null) {
            return null;
        }
        try {
            IMessage transformed = transformer.transform(message);
            String label = transformed.getLabel();
            IMessageSender target = routeTable.get(label);
            if (target != null) {
                target.send(transformed);
                receiver.complete(message.getLockToken());
            } else {
                receiver.abandon(message.getLockToken());
            }
            return transformed;
        } catch (ServiceBusException e) {
            receiver.deadLetter(message.getLockToken(), "TransformFailed", e.getMessage());
            throw e;
        }
    }
}
