package com.contoso.messaging;

import com.azure.messaging.servicebus.ServiceBusClientBuilder;
import com.azure.messaging.servicebus.ServiceBusReceiverClient;
import com.azure.messaging.servicebus.ServiceBusSenderClient;
import com.azure.messaging.servicebus.models.ServiceBusReceiveMode;

import java.net.URI;

public final class ServiceBusClients {

    private ServiceBusClients() {
    }

    public static ConnectionStringProperties buildConnection(
            String endpoint, String entityPath, String sasKeyName, String sasKey) {
        return new ConnectionStringProperties(endpoint, entityPath, sasKeyName, sasKey);
    }

    public static ConnectionStringProperties parseConnection(String connectionString) {
        return new ConnectionStringProperties(connectionString);
    }

    public static MessageSender createSender(ConnectionStringProperties properties) {
        ServiceBusSenderClient client = new ServiceBusClientBuilder()
            .connectionString(properties.toConnectionString())
            .sender()
            .queueName(properties.getEntityPath())
            .buildClient();
        return new MessageSender() {
            @Override
            public void send(com.azure.messaging.servicebus.ServiceBusMessage message) {
                client.sendMessage(message);
            }

            @Override
            public java.util.concurrent.CompletableFuture<Void> sendAsync(
                    com.azure.messaging.servicebus.ServiceBusMessage message) {
                return java.util.concurrent.CompletableFuture.runAsync(() -> client.sendMessage(message));
            }

            @Override
            public void close() {
                client.close();
            }
        };
    }

    public static MessageReceiver createReceiver(
            ConnectionStringProperties properties, ServiceBusReceiveMode mode) {
        ServiceBusReceiverClient client = new ServiceBusClientBuilder()
            .connectionString(properties.toConnectionString())
            .receiver()
            .queueName(properties.getEntityPath())
            .receiveMode(mode)
            .buildClient();
        return new MessageReceiver() {
            @Override
            public com.azure.messaging.servicebus.ServiceBusReceivedMessage receive() {
                java.util.Iterator<com.azure.messaging.servicebus.ServiceBusReceivedMessage> iter =
                    client.receiveMessages(1).iterator();
                return iter.hasNext() ? iter.next() : null;
            }

            @Override
            public java.util.concurrent.CompletableFuture<com.azure.messaging.servicebus.ServiceBusReceivedMessage> receiveAsync() {
                return java.util.concurrent.CompletableFuture.supplyAsync(this::receive);
            }

            @Override
            public java.util.Collection<com.azure.messaging.servicebus.ServiceBusReceivedMessage> receiveBatch(int maxMessages) {
                java.util.List<com.azure.messaging.servicebus.ServiceBusReceivedMessage> list = new java.util.ArrayList<>();
                client.receiveMessages(maxMessages).forEach(list::add);
                return list;
            }

            @Override
            public java.util.concurrent.CompletableFuture<java.util.Collection<com.azure.messaging.servicebus.ServiceBusReceivedMessage>> receiveBatchAsync(int maxMessages) {
                return java.util.concurrent.CompletableFuture.supplyAsync(() -> receiveBatch(maxMessages));
            }

            @Override
            public void complete(com.azure.messaging.servicebus.ServiceBusReceivedMessage message) {
                client.complete(message);
            }

            @Override
            public java.util.concurrent.CompletableFuture<Void> completeAsync(com.azure.messaging.servicebus.ServiceBusReceivedMessage message) {
                return java.util.concurrent.CompletableFuture.runAsync(() -> client.complete(message));
            }

            @Override
            public void abandon(com.azure.messaging.servicebus.ServiceBusReceivedMessage message) {
                client.abandon(message);
            }

            @Override
            public java.util.concurrent.CompletableFuture<Void> abandonAsync(com.azure.messaging.servicebus.ServiceBusReceivedMessage message) {
                return java.util.concurrent.CompletableFuture.runAsync(() -> client.abandon(message));
            }

            @Override
            public void deadLetter(com.azure.messaging.servicebus.ServiceBusReceivedMessage message,
                                   String reason, String description) {
                client.deadLetter(message, new com.azure.messaging.servicebus.models.DeadLetterOptions()
                    .setDeadLetterReason(reason)
                    .setDeadLetterErrorDescription(description));
            }

            @Override
            public void close() {
                client.close();
            }
        };
    }

    public static String extractEntityPath(ConnectionStringProperties properties) {
        return properties.getEntityPath();
    }

    public static URI extractEndpoint(ConnectionStringProperties properties) {
        return properties.getEndpoint();
    }

    public static String extractSasKeyName(ConnectionStringProperties properties) {
        return properties.getSasKeyName();
    }

    public static String extractSasKey(ConnectionStringProperties properties) {
        return properties.getSasKey();
    }

    public static ConnectionStringProperties withEntityPath(
            ConnectionStringProperties original, String newEntityPath) {
        return new ConnectionStringProperties(
            original.getEndpoint().toString(),
            newEntityPath,
            original.getSasKeyName(),
            original.getSasKey());
    }
}
