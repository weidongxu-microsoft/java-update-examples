package com.contoso.messaging;

import com.azure.messaging.servicebus.ServiceBusReceivedMessage;
import com.azure.messaging.servicebus.ServiceBusSenderClient;
import com.azure.messaging.servicebus.ServiceBusMessage;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;

public class OrderMessageHandler implements Consumer<ServiceBusReceivedMessage> {

    private final ServiceBusSenderClient forwardSender;
    private final ErrorClassifier errorClassifier;
    private final List<MessageTransformer> transformers;
    private final List<ServiceBusMessage> processedMessages;

    public OrderMessageHandler(ServiceBusSenderClient forwardSender, ErrorClassifier errorClassifier) {
        this.forwardSender = forwardSender;
        this.errorClassifier = errorClassifier;
        this.transformers = new ArrayList<>();
        this.processedMessages = Collections.synchronizedList(new ArrayList<ServiceBusMessage>());
    }

    public void addTransformer(MessageTransformer transformer) {
        transformers.add(transformer);
    }

    @Override
    public void accept(ServiceBusReceivedMessage receivedMessage) {
        try {
            // Convert received message to regular message
            ServiceBusMessage current = convertToMessage(receivedMessage);
            
            // Apply transformers
            for (MessageTransformer transformer : transformers) {
                current = transformer.transform(current);
            }
            
            processedMessages.add(current);
            forwardSender.sendMessage(current);
        } catch (Exception ex) {
            errorClassifier.classify(ex);
        }
    }

    private ServiceBusMessage convertToMessage(ServiceBusReceivedMessage received) {
        ServiceBusMessage message = new ServiceBusMessage(received.getBody());
        message.setMessageId(received.getMessageId());
        message.setSubject(received.getSubject());
        message.setContentType(received.getContentType());
        message.setCorrelationId(received.getCorrelationId());
        message.setSessionId(received.getSessionId());
        message.setReplyTo(received.getReplyTo());
        message.setTimeToLive(received.getTimeToLive());
        if (received.getApplicationProperties() != null) {
            message.getApplicationProperties().putAll(received.getApplicationProperties());
        }
        return message;
    }

    public void handleError(Throwable exception) {
        ErrorClassifier.ErrorCategory category = errorClassifier.classify(exception);
        if (category == ErrorClassifier.ErrorCategory.TRANSIENT) {
            return;
        }
        if (category == ErrorClassifier.ErrorCategory.ENTITY_NOT_FOUND) {
            System.err.println("Entity not found during message processing");
        }
    }

    public List<ServiceBusMessage> getProcessedMessages() {
        return Collections.unmodifiableList(processedMessages);
    }

    public int getProcessedCount() {
        return processedMessages.size();
    }
}
