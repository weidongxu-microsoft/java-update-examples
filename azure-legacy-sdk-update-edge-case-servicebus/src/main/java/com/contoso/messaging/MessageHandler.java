package com.contoso.messaging;

import com.azure.messaging.servicebus.ServiceBusMessage;

import java.util.concurrent.CompletableFuture;

/**
 * Application-level interface replacing the removed com.microsoft.azure.servicebus.IMessageHandler.
 * Uses ServiceBusMessage instead of IMessage and ErrorPhase instead of ExceptionPhase.
 */
public interface MessageHandler {

    CompletableFuture<Void> onMessageAsync(ServiceBusMessage message);

    void notifyException(Throwable exception, ErrorPhase phase);
}
