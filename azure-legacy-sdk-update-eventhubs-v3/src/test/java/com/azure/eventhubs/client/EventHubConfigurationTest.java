package com.azure.eventhubs.client;

import org.junit.Test;

import static org.junit.Assert.*;

/**
 * Unit tests for EventHubConfiguration.
 */
public class EventHubConfigurationTest {

    @Test
    public void testDefaultConstructor() {
        EventHubConfiguration config = new EventHubConfiguration();
        assertNotNull(config);
        assertNotNull(config.getEventHubConnectionString());
        assertNotNull(config.getEventHubName());
        assertNotNull(config.getConsumerGroupName());
        assertNotNull(config.getStorageConnectionString());
        assertNotNull(config.getStorageContainerName());
    }

    @Test
    public void testParameterizedConstructor() {
        EventHubConfiguration config = new EventHubConfiguration(
                "Endpoint=sb://test.servicebus.windows.net/;SharedAccessKeyName=test;SharedAccessKey=key",
                "test-hub",
                "$Default",
                "DefaultEndpointsProtocol=https;AccountName=test;AccountKey=key;EndpointSuffix=core.windows.net",
                "test-container");

        assertEquals("Endpoint=sb://test.servicebus.windows.net/;SharedAccessKeyName=test;SharedAccessKey=key",
                config.getEventHubConnectionString());
        assertEquals("test-hub", config.getEventHubName());
        assertEquals("$Default", config.getConsumerGroupName());
        assertEquals("DefaultEndpointsProtocol=https;AccountName=test;AccountKey=key;EndpointSuffix=core.windows.net",
                config.getStorageConnectionString());
        assertEquals("test-container", config.getStorageContainerName());
    }

    @Test
    public void testIsValidWithValidConfiguration() {
        EventHubConfiguration config = new EventHubConfiguration(
                "valid-connection-string",
                "valid-hub-name",
                "$Default",
                "valid-storage-string",
                "valid-container");

        assertTrue(config.isValid());
    }

    @Test
    public void testIsValidWithEmptyEventHubConnectionString() {
        EventHubConfiguration config = new EventHubConfiguration(
                "",
                "valid-hub-name",
                "$Default",
                "valid-storage-string",
                "valid-container");

        assertFalse(config.isValid());
    }

    @Test
    public void testIsValidWithNullEventHubName() {
        EventHubConfiguration config = new EventHubConfiguration(
                "valid-connection-string",
                null,
                "$Default",
                "valid-storage-string",
                "valid-container");

        assertFalse(config.isValid());
    }

    @Test
    public void testIsValidWithEmptyStorageConnectionString() {
        EventHubConfiguration config = new EventHubConfiguration(
                "valid-connection-string",
                "valid-hub-name",
                "$Default",
                "",
                "valid-container");

        assertFalse(config.isValid());
    }

    @Test
    public void testIsValidWithNullStorageContainerName() {
        EventHubConfiguration config = new EventHubConfiguration(
                "valid-connection-string",
                "valid-hub-name",
                "$Default",
                "valid-storage-string",
                null);

        assertFalse(config.isValid());
    }

    @Test
    public void testGetters() {
        String connectionString = "test-connection";
        String hubName = "test-hub";
        String consumerGroup = "test-group";
        String storageConnection = "test-storage";
        String container = "test-container";

        EventHubConfiguration config = new EventHubConfiguration(
                connectionString,
                hubName,
                consumerGroup,
                storageConnection,
                container);

        assertEquals(connectionString, config.getEventHubConnectionString());
        assertEquals(hubName, config.getEventHubName());
        assertEquals(consumerGroup, config.getConsumerGroupName());
        assertEquals(storageConnection, config.getStorageConnectionString());
        assertEquals(container, config.getStorageContainerName());
    }

    @Test
    public void testDefaultConsumerGroup() {
        EventHubConfiguration config = new EventHubConfiguration();
        assertEquals("$Default", config.getConsumerGroupName());
    }
}
