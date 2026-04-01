package com.contoso.messaging;

import com.microsoft.azure.servicebus.ExceptionPhase;
import com.microsoft.azure.servicebus.IMessage;
import com.microsoft.azure.servicebus.IMessageHandler;
import com.microsoft.azure.servicebus.IMessageSender;
import com.microsoft.azure.servicebus.primitives.ServiceBusException;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public class OrderMessageHandler implements IMessageHandler {

    private final IMessageSender forwardSender;
    private final ErrorClassifier errorClassifier;
    private final List<MessageTransformer> transformers;
    private final List<IMessage> processedMessages;

    public OrderMessageHandler(IMessageSender forwardSender, ErrorClassifier errorClassifier) {
        this.forwardSender = forwardSender;
        this.errorClassifier = errorClassifier;
        this.transformers = new ArrayList<>();
        this.processedMessages = Collections.synchronizedList(new ArrayList<IMessage>());
    }

    public void addTransformer(MessageTransformer transformer) {
        transformers.add(transformer);
    }

    @Override
    public CompletableFuture<Void> onMessageAsync(IMessage message) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                IMessage current = message;
                for (MessageTransformer transformer : transformers) {
                    current = transformer.transform(current);
                }
                return current;
            } catch (ServiceBusException e) {
                throw new RuntimeException(e);
            }
        }).thenCompose(transformed -> {
            processedMessages.add(transformed);
            return forwardSender.sendAsync(transformed);
        }).exceptionally(ex -> {
            errorClassifier.classify(ex);
            return null;
        });
    }

    @Override
    public void notifyException(Throwable exception, ExceptionPhase phase) {
        ErrorClassifier.ErrorCategory category = errorClassifier.classify(exception);
        if (phase == ExceptionPhase.RECEIVE
                && category == ErrorClassifier.ErrorCategory.TRANSIENT) {
            return;
        }
        if (category == ErrorClassifier.ErrorCategory.ENTITY_NOT_FOUND) {
            System.err.println("Entity not found during " + phase.name());
        }
    }

    public List<IMessage> getProcessedMessages() {
        return Collections.unmodifiableList(processedMessages);
    }

    public int getProcessedCount() {
        return processedMessages.size();
    }
}
