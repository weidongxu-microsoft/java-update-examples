package com.contoso.messaging;

import com.azure.messaging.servicebus.ServiceBusMessage;
import com.azure.messaging.servicebus.ServiceBusReceivedMessage;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

public class MessageRouter {

    private final MessageReceiver receiver;
    private final Map<String, MessageSender> routeTable;
    private final ErrorClassifier errorClassifier;

    public MessageRouter(MessageReceiver receiver, ErrorClassifier errorClassifier) {
        this.receiver = receiver;
        this.routeTable = new ConcurrentHashMap<>();
        this.errorClassifier = errorClassifier;
    }

    public void addRoute(String label, MessageSender sender) {
        routeTable.put(label, sender);
    }

    public CompletableFuture<ServiceBusMessage> receiveAndRoute() {
        return receiver.receiveAsync()
            .thenCompose(msg -> {
                if (msg == null) {
                    return CompletableFuture.completedFuture((ServiceBusMessage) null);
                }
                String label = msg.getSubject();
                MessageSender target = routeTable.get(label);
                ServiceBusMessage sendMsg = toSendMessage(msg);
                if (target == null) {
                    return receiver.abandonAsync(msg)
                        .thenApply(v -> sendMsg);
                }
                return target.sendAsync(sendMsg)
                    .thenCompose(v -> receiver.completeAsync(msg))
                    .thenApply(v -> sendMsg);
            })
            .exceptionally(ex -> {
                errorClassifier.classify(ex);
                return null;
            });
    }

    public CompletableFuture<List<ServiceBusMessage>> receiveAndRouteBatch(
            int maxMessages, MessageTransformer transformer) {
        return receiver.receiveBatchAsync(maxMessages)
            .thenCompose(messages -> {
                if (messages == null || messages.isEmpty()) {
                    return CompletableFuture.completedFuture(Collections.<ServiceBusMessage>emptyList());
                }
                List<CompletableFuture<ServiceBusMessage>> futures = new ArrayList<>();
                for (final ServiceBusReceivedMessage msg : messages) {
                    CompletableFuture<ServiceBusMessage> pipeline = CompletableFuture
                        .supplyAsync(() -> {
                            try {
                                return transformer.transform(toSendMessage(msg));
                            } catch (Exception e) {
                                throw new RuntimeException(e);
                            }
                        })
                        .thenCompose(transformed -> {
                            String label = transformed.getSubject();
                            MessageSender target = routeTable.get(label);
                            if (target != null) {
                                return target.sendAsync(transformed)
                                    .thenCompose(v -> receiver.completeAsync(msg))
                                    .thenApply(v -> transformed);
                            }
                            return receiver.abandonAsync(msg)
                                .thenApply(v -> transformed);
                        });
                    futures.add(pipeline);
                }
                return CompletableFuture
                    .allOf(futures.toArray(new CompletableFuture[0]))
                    .thenApply(v -> {
                        List<ServiceBusMessage> results = new ArrayList<>();
                        for (CompletableFuture<ServiceBusMessage> f : futures) {
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

    public ServiceBusMessage receiveAndTransform(MessageTransformer transformer)
            throws Exception {
        ServiceBusReceivedMessage message = receiver.receive();
        if (message == null) {
            return null;
        }
        try {
            ServiceBusMessage sendMsg = toSendMessage(message);
            ServiceBusMessage transformed = transformer.transform(sendMsg);
            String label = transformed.getSubject();
            MessageSender target = routeTable.get(label);
            if (target != null) {
                target.send(transformed);
                receiver.complete(message);
            } else {
                receiver.abandon(message);
            }
            return transformed;
        } catch (Exception e) {
            receiver.deadLetter(message, "TransformFailed", e.getMessage());
            throw e;
        }
    }

    /**
     * Converts a ServiceBusReceivedMessage to a ServiceBusMessage for sending.
     */
    public static ServiceBusMessage toSendMessage(ServiceBusReceivedMessage received) {
        ServiceBusMessage msg = new ServiceBusMessage(received.getBody());
        if (received.getMessageId() != null) {
            msg.setMessageId(received.getMessageId());
        }
        msg.setSubject(received.getSubject());
        msg.setContentType(received.getContentType());
        msg.setCorrelationId(received.getCorrelationId());
        if (received.getSessionId() != null) {
            msg.setSessionId(received.getSessionId());
        }
        if (received.getReplyTo() != null) {
            msg.setReplyTo(received.getReplyTo());
        }
        if (received.getTimeToLive() != null) {
            msg.setTimeToLive(received.getTimeToLive());
        }
        Map<String, Object> props = received.getApplicationProperties();
        if (props != null && !props.isEmpty()) {
            msg.getApplicationProperties().putAll(props);
        }
        return msg;
    }
}
