package com.azure.servicebus.client;

public class ServiceBusConfiguration {

    private static final String DEFAULT_QUEUE_NAME = "orders-queue";

    private final String connectionString;
    private final String queueName;

    public ServiceBusConfiguration() {
        this.connectionString = getEnvOrDefault("SERVICE_BUS_CONNECTION_STRING",
                "Endpoint=sb://servicebus-namespace.servicebus.windows.net/;SharedAccessKeyName=RootManageSharedAccessKey;SharedAccessKey=your-key");
        this.queueName = getEnvOrDefault("SERVICE_BUS_QUEUE_NAME", DEFAULT_QUEUE_NAME);
    }

    public ServiceBusConfiguration(String connectionString, String queueName) {
        this.connectionString = connectionString;
        this.queueName = queueName;
    }

    public String getConnectionString() {
        return connectionString;
    }

    public String getQueueName() {
        return queueName;
    }

    public boolean isValid() {
        return connectionString != null && !connectionString.isEmpty()
                && queueName != null && !queueName.isEmpty();
    }

    private String getEnvOrDefault(String key, String defaultValue) {
        String value = System.getenv(key);
        return value != null ? value : defaultValue;
    }
}
