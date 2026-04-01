package com.contoso.messaging;

import com.microsoft.azure.servicebus.ClientFactory;
import com.microsoft.azure.servicebus.IMessageReceiver;
import com.microsoft.azure.servicebus.IMessageSender;
import com.microsoft.azure.servicebus.ReceiveMode;
import com.microsoft.azure.servicebus.primitives.ConnectionStringBuilder;
import com.microsoft.azure.servicebus.primitives.ServiceBusException;

import java.net.URI;

public final class ServiceBusClients {

    private ServiceBusClients() {
    }

    public static ConnectionStringBuilder buildConnection(
            String endpoint, String entityPath, String sasKeyName, String sasKey) {
        return new ConnectionStringBuilder(endpoint, entityPath, sasKeyName, sasKey);
    }

    public static ConnectionStringBuilder parseConnection(String connectionString) {
        return new ConnectionStringBuilder(connectionString);
    }

    public static IMessageSender createSender(ConnectionStringBuilder builder)
            throws InterruptedException, ServiceBusException {
        return ClientFactory.createMessageSenderFromConnectionStringBuilder(builder);
    }

    public static IMessageReceiver createReceiver(
            ConnectionStringBuilder builder, ReceiveMode mode)
            throws InterruptedException, ServiceBusException {
        return ClientFactory.createMessageReceiverFromConnectionStringBuilder(builder, mode);
    }

    public static String extractEntityPath(ConnectionStringBuilder builder) {
        return builder.getEntityPath();
    }

    public static URI extractEndpoint(ConnectionStringBuilder builder) {
        return builder.getEndpoint();
    }

    public static String extractSasKeyName(ConnectionStringBuilder builder) {
        return builder.getSasKeyName();
    }

    public static String extractSasKey(ConnectionStringBuilder builder) {
        return builder.getSasKey();
    }

    public static ConnectionStringBuilder withEntityPath(
            ConnectionStringBuilder original, String newEntityPath) {
        return new ConnectionStringBuilder(
            original.getEndpoint().toString(),
            newEntityPath,
            original.getSasKeyName(),
            original.getSasKey());
    }
}
