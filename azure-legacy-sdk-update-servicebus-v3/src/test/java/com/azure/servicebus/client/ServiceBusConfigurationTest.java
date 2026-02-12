package com.azure.servicebus.client;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class ServiceBusConfigurationTest {

    @Test
    public void testDefaultConstructor() {
        ServiceBusConfiguration config = new ServiceBusConfiguration();
        assertNotNull(config.getConnectionString());
        assertNotNull(config.getQueueName());
    }

    @Test
    public void testParameterizedConstructor() {
        ServiceBusConfiguration config = new ServiceBusConfiguration(
                "Endpoint=sb://test.servicebus.windows.net/;SharedAccessKeyName=test;SharedAccessKey=key",
                "notifications-queue");
        assertEquals("Endpoint=sb://test.servicebus.windows.net/;SharedAccessKeyName=test;SharedAccessKey=key",
                config.getConnectionString());
        assertEquals("notifications-queue", config.getQueueName());
    }

    @Test
    public void testIsValidWithValidConfiguration() {
        ServiceBusConfiguration config = new ServiceBusConfiguration(
                "Endpoint=sb://ns.servicebus.windows.net/;SharedAccessKeyName=name;SharedAccessKey=key",
                "orders-queue");
        assertTrue(config.isValid());
    }

    @Test
    public void testIsValidWithEmptyConnectionString() {
        ServiceBusConfiguration config = new ServiceBusConfiguration("", "orders-queue");
        assertFalse(config.isValid());
    }

    @Test
    public void testIsValidWithNullConnectionString() {
        ServiceBusConfiguration config = new ServiceBusConfiguration(null, "orders-queue");
        assertFalse(config.isValid());
    }

    @Test
    public void testIsValidWithEmptyQueueName() {
        ServiceBusConfiguration config = new ServiceBusConfiguration(
                "Endpoint=sb://ns.servicebus.windows.net/;SharedAccessKeyName=name;SharedAccessKey=key", "");
        assertFalse(config.isValid());
    }

    @Test
    public void testIsValidWithNullQueueName() {
        ServiceBusConfiguration config = new ServiceBusConfiguration(
                "Endpoint=sb://ns.servicebus.windows.net/;SharedAccessKeyName=name;SharedAccessKey=key", null);
        assertFalse(config.isValid());
    }

    @Test
    public void testDefaultQueueName() {
        ServiceBusConfiguration config = new ServiceBusConfiguration();
        assertEquals("orders-queue", config.getQueueName());
    }

    @Test
    public void testGetters() {
        ServiceBusConfiguration config = new ServiceBusConfiguration("conn-str", "my-queue");
        assertEquals("conn-str", config.getConnectionString());
        assertEquals("my-queue", config.getQueueName());
    }
}
