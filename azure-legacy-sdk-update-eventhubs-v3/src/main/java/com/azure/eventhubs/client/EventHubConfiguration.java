package com.azure.eventhubs.client;

/**
 * Configuration class for Event Hub connection details.
 */
public class EventHubConfiguration {
    
    private static final String DEFAULT_CONSUMER_GROUP = "$Default";
    
    private final String eventHubConnectionString;
    private final String eventHubName;
    private final String consumerGroupName;
    private final String storageConnectionString;
    private final String storageContainerName;

    /**
     * Creates configuration from environment variables.
     */
    public EventHubConfiguration() {
        this.eventHubConnectionString = getEnvOrDefault("EVENT_HUB_CONNECTION_STRING", 
                "Endpoint=sb://namespace.servicebus.windows.net/;SharedAccessKeyName=RootManageSharedAccessKey;SharedAccessKey=key");
        this.eventHubName = getEnvOrDefault("EVENT_HUB_NAME", "eventhub-instance");
        this.consumerGroupName = getEnvOrDefault("EVENT_HUB_CONSUMER_GROUP", DEFAULT_CONSUMER_GROUP);
        this.storageConnectionString = getEnvOrDefault("STORAGE_CONNECTION_STRING",
                "DefaultEndpointsProtocol=https;AccountName=storageaccount;AccountKey=key;EndpointSuffix=core.windows.net");
        this.storageContainerName = getEnvOrDefault("STORAGE_CONTAINER_NAME", "eventhub-checkpoints");
    }

    /**
     * Creates configuration with explicit values.
     */
    public EventHubConfiguration(String eventHubConnectionString, String eventHubName,
                                String consumerGroupName, String storageConnectionString,
                                String storageContainerName) {
        this.eventHubConnectionString = eventHubConnectionString;
        this.eventHubName = eventHubName;
        this.consumerGroupName = consumerGroupName;
        this.storageConnectionString = storageConnectionString;
        this.storageContainerName = storageContainerName;
    }

    public String getEventHubConnectionString() {
        return eventHubConnectionString;
    }

    public String getEventHubName() {
        return eventHubName;
    }

    public String getConsumerGroupName() {
        return consumerGroupName;
    }

    public String getStorageConnectionString() {
        return storageConnectionString;
    }

    public String getStorageContainerName() {
        return storageContainerName;
    }

    /**
     * Validates that all required configuration values are present.
     */
    public boolean isValid() {
        return eventHubConnectionString != null && !eventHubConnectionString.isEmpty()
                && eventHubName != null && !eventHubName.isEmpty()
                && storageConnectionString != null && !storageConnectionString.isEmpty()
                && storageContainerName != null && !storageContainerName.isEmpty();
    }

    private String getEnvOrDefault(String key, String defaultValue) {
        String value = System.getenv(key);
        return value != null ? value : defaultValue;
    }
}
