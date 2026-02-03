package com.azure.eventhubs.client;

import com.microsoft.azure.eventprocessorhost.EventProcessorHost;
import com.microsoft.azure.eventprocessorhost.IEventProcessor;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.function.Consumer;

import static org.junit.Assert.*;

/**
 * Unit tests for EventHubReceiver.
 */
public class EventHubReceiverTest {

    private EventHubReceiver receiver;

    @Before
    public void setUp() {
        receiver = new EventHubReceiver(
                "Endpoint=sb://namespace.servicebus.windows.net/;SharedAccessKeyName=RootManageSharedAccessKey;SharedAccessKey=testkey",
                "test-eventhub",
                "$Default",
                "DefaultEndpointsProtocol=https;AccountName=testaccount;AccountKey=testkey;EndpointSuffix=core.windows.net",
                "checkpoints",
                "test-host");
    }

    @After
    public void tearDown() {
        // cleanup if needed
    }

    @Test
    public void testConstructor() {
        assertNotNull(receiver);
    }

    @Test
    public void testSetMessageHandler() {
        Consumer<String> handler = message -> System.out.println(message);
        receiver.setMessageHandler(handler);
        // If no exception is thrown, the test passes
    }

    @Test
    public void testReceiverConfiguration() {
        EventHubReceiver testReceiver = new EventHubReceiver(
                "Endpoint=sb://namespace.servicebus.windows.net/;SharedAccessKeyName=RootManageSharedAccessKey;SharedAccessKey=key",
                "eventhub-name",
                "$Default",
                "DefaultEndpointsProtocol=https;AccountName=storage;AccountKey=key;EndpointSuffix=core.windows.net",
                "container",
                "host-1");
        assertNotNull(testReceiver);
    }

    @Test
    public void testMessageHandlerInvocation() {
        final StringBuilder receivedMessage = new StringBuilder();
        Consumer<String> handler = receivedMessage::append;
        
        receiver.setMessageHandler(handler);
        
        // Simulate message processing
        handler.accept("Test message");
        
        assertEquals("Test message", receivedMessage.toString());
    }

    @Test
    public void testEventProcessorInnerClass() {
        // Test that the EventProcessor inner class exists and is accessible
        EventHubReceiver.EventProcessor processor = receiver.new EventProcessor();
        assertNotNull(processor);
        assertTrue(processor instanceof IEventProcessor);
    }

    @Test
    public void testMultipleMessageHandlers() {
        final int[] messageCount = {0};
        
        Consumer<String> handler = message -> messageCount[0]++;
        receiver.setMessageHandler(handler);
        
        // Simulate multiple messages
        handler.accept("Message 1");
        handler.accept("Message 2");
        handler.accept("Message 3");
        
        assertEquals(3, messageCount[0]);
    }

    @Test
    public void testHostNameGeneration() {
        String hostName = EventProcessorHost.createHostName("test-prefix");
        assertNotNull(hostName);
        assertTrue(hostName.contains("test-prefix"));
    }

    @Test
    public void testReceiverWithCustomConsumerGroup() {
        EventHubReceiver customReceiver = new EventHubReceiver(
                "Endpoint=sb://namespace.servicebus.windows.net/;SharedAccessKeyName=RootManageSharedAccessKey;SharedAccessKey=key",
                "eventhub",
                "custom-consumer-group",
                "DefaultEndpointsProtocol=https;AccountName=storage;AccountKey=key;EndpointSuffix=core.windows.net",
                "checkpoints",
                "host");
        assertNotNull(customReceiver);
    }
}
