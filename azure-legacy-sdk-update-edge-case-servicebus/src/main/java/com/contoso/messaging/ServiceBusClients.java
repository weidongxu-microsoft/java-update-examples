package com.contoso.messaging;

import com.azure.messaging.servicebus.ServiceBusClientBuilder;
import com.azure.messaging.servicebus.ServiceBusSenderClient;
import com.azure.messaging.servicebus.ServiceBusReceiverClient;
import com.azure.messaging.servicebus.models.ServiceBusReceiveMode;

public final class ServiceBusClients {

    private ServiceBusClients() {
    }

    public static ServiceBusSenderClient createSender(String connectionString, String queueName) {
        return new ServiceBusClientBuilder()
            .connectionString(connectionString)
            .sender()
            .queueName(queueName)
            .buildClient();
    }

    public static ServiceBusReceiverClient createReceiver(
            String connectionString, String queueName, ServiceBusReceiveMode mode) {
        return new ServiceBusClientBuilder()
            .connectionString(connectionString)
            .receiver()
            .queueName(queueName)
            .receiveMode(mode)
            .buildClient();
    }

    public static String extractEntityPath(String connectionString) {
        // Extract EntityPath from connection string
        String[] parts = connectionString.split(";");
        for (String part : parts) {
            if (part.trim().startsWith("EntityPath=")) {
                return part.trim().substring("EntityPath=".length());
            }
        }
        return null;
    }

    public static String extractEndpoint(String connectionString) {
        // Extract Endpoint from connection string
        String[] parts = connectionString.split(";");
        for (String part : parts) {
            if (part.trim().startsWith("Endpoint=")) {
                return part.trim().substring("Endpoint=".length());
            }
        }
        return null;
    }

    public static String withEntityPath(String connectionString, String newEntityPath) {
        // Replace or add EntityPath in connection string
        StringBuilder result = new StringBuilder();
        String[] parts = connectionString.split(";");
        boolean foundEntityPath = false;
        
        for (String part : parts) {
            if (part.trim().startsWith("EntityPath=")) {
                result.append("EntityPath=").append(newEntityPath).append(";");
                foundEntityPath = true;
            } else if (!part.trim().isEmpty()) {
                result.append(part).append(";");
            }
        }
        
        if (!foundEntityPath) {
            result.append("EntityPath=").append(newEntityPath);
        }
        
        return result.toString().replaceAll(";+$", "");
    }
}
