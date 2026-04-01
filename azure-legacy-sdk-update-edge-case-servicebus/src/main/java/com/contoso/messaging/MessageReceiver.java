package com.contoso.messaging;

import com.azure.messaging.servicebus.ServiceBusReceivedMessage;

import java.util.Collection;
import java.util.concurrent.CompletableFuture;

/**
 * Wrapper interface for Service Bus receive operations.
 * Replaces the legacy IMessageReceiver interface for testability, since
 * ServiceBusReceiverClient in the modern SDK is a final class.
 */
public interface MessageReceiver {

    ServiceBusReceivedMessage receive();

    CompletableFuture<ServiceBusReceivedMessage> receiveAsync();

    Collection<ServiceBusReceivedMessage> receiveBatch(int maxMessages);

    CompletableFuture<Collection<ServiceBusReceivedMessage>> receiveBatchAsync(int maxMessages);

    void complete(ServiceBusReceivedMessage message);

    CompletableFuture<Void> completeAsync(ServiceBusReceivedMessage message);

    void abandon(ServiceBusReceivedMessage message);

    CompletableFuture<Void> abandonAsync(ServiceBusReceivedMessage message);

    void deadLetter(ServiceBusReceivedMessage message, String reason, String description);

    void close();
}
