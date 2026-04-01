package com.contoso.messaging;

import com.azure.messaging.servicebus.ServiceBusMessage;

import java.util.concurrent.CompletableFuture;

/**
 * Wrapper interface for Service Bus send operations.
 * Replaces the legacy IMessageSender interface for testability, since
 * ServiceBusSenderClient in the modern SDK is a final class.
 */
public interface MessageSender {

    void send(ServiceBusMessage message);

    CompletableFuture<Void> sendAsync(ServiceBusMessage message);

    void close();
}
