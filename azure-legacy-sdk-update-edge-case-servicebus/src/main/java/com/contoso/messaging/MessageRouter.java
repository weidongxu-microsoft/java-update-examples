package com.contoso.messaging;

import com.azure.messaging.servicebus.ServiceBusReceivedMessage;
import com.azure.messaging.servicebus.ServiceBusReceiverClient;
import com.azure.messaging.servicebus.ServiceBusSenderClient;
import com.azure.messaging.servicebus.ServiceBusMessage;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class MessageRouter {

    private final ServiceBusReceiverClient receiver;
    private final Map<String, ServiceBusSenderClient> routeTable;
    private final ErrorClassifier errorClassifier;

    public MessageRouter(ServiceBusReceiverClient receiver, ErrorClassifier errorClassifier) {
        this.receiver = receiver;
        this.routeTable = new ConcurrentHashMap<>();
        this.errorClassifier = errorClassifier;
    }

    public void addRoute(String subject, ServiceBusSenderClient sender) {
        routeTable.put(subject, sender);
    }

    public ServiceBusReceivedMessage receiveAndRoute() {
        try {
            ServiceBusReceivedMessage msg = receiver.receiveMessages(1, Duration.ofSeconds(5))
                .stream().findFirst().orElse(null);
            
            if (msg == null) {
                return null;
            }
            
            String subject = msg.getSubject();
            ServiceBusSenderClient target = routeTable.get(subject);
            
            if (target == null) {
                receiver.abandon(msg);
                return msg;
            }
            
            ServiceBusMessage forwardMsg = new ServiceBusMessage(msg.getBody());
            forwardMsg.setMessageId(msg.getMessageId());
            forwardMsg.setSubject(msg.getSubject());
            forwardMsg.setContentType(msg.getContentType());
            forwardMsg.setCorrelationId(msg.getCorrelationId());
            if (msg.getApplicationProperties() != null) {
                forwardMsg.getApplicationProperties().putAll(msg.getApplicationProperties());
            }
            
            target.sendMessage(forwardMsg);
            receiver.complete(msg);
            return msg;
        } catch (Exception ex) {
            errorClassifier.classify(ex);
            return null;
        }
    }

    public List<ServiceBusReceivedMessage> receiveAndRouteBatch(
            int maxMessages, MessageTransformer transformer) {
        try {
            Iterable<ServiceBusReceivedMessage> messages = receiver.receiveMessages(
                maxMessages, Duration.ofSeconds(10));
            
            List<ServiceBusReceivedMessage> results = new ArrayList<>();
            for (ServiceBusReceivedMessage msg : messages) {
                try {
                    ServiceBusMessage temp = convertToMessage(msg);
                    ServiceBusMessage transformed = transformer.transform(temp);
                    
                    String subject = transformed.getSubject();
                    ServiceBusSenderClient target = routeTable.get(subject);
                    
                    if (target != null) {
                        target.sendMessage(transformed);
                        receiver.complete(msg);
                    } else {
                        receiver.abandon(msg);
                    }
                    results.add(msg);
                } catch (Exception e) {
                    errorClassifier.classify(e);
                }
            }
            return results;
        } catch (Exception ex) {
            errorClassifier.classify(ex);
            return Collections.emptyList();
        }
    }

    public ServiceBusReceivedMessage receiveAndTransform(MessageTransformer transformer) {
        try {
            ServiceBusReceivedMessage message = receiver.receiveMessages(1, Duration.ofSeconds(5))
                .stream().findFirst().orElse(null);
            
            if (message == null) {
                return null;
            }
            
            try {
                ServiceBusMessage temp = convertToMessage(message);
                ServiceBusMessage transformed = transformer.transform(temp);
                String subject = transformed.getSubject();
                ServiceBusSenderClient target = routeTable.get(subject);
                
                if (target != null) {
                    target.sendMessage(transformed);
                    receiver.complete(message);
                } else {
                    receiver.abandon(message);
                }
                return message;
            } catch (Exception e) {
                receiver.deadLetter(message, new com.azure.messaging.servicebus.models.DeadLetterOptions()
                    .setDeadLetterReason("TransformFailed")
                    .setDeadLetterErrorDescription(e.getMessage()));
                throw e;
            }
        } catch (Exception ex) {
            errorClassifier.classify(ex);
            return null;
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
}
