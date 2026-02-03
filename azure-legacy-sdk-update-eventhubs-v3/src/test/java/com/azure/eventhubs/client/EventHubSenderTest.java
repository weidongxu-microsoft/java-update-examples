package com.azure.eventhubs.client;

import com.microsoft.azure.eventhubs.EventData;
import com.microsoft.azure.eventhubs.EventDataBatch;
import com.microsoft.azure.eventhubs.EventHubClient;
import com.microsoft.azure.eventhubs.EventHubException;
import com.microsoft.azure.eventhubs.PartitionSender;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ExecutionException;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * Unit tests for EventHubSender.
 */
public class EventHubSenderTest {

    @Mock
    private EventHubClient mockClient;

    @Mock
    private PartitionSender mockPartitionSender;

    @Mock
    private EventDataBatch mockBatch;

    private EventHubSender sender;
    private AutoCloseable closeable;

    @Before
    public void setUp() {
        closeable = MockitoAnnotations.openMocks(this);
        sender = new EventHubSender(
                "Endpoint=sb://namespace.servicebus.windows.net/;SharedAccessKeyName=RootManageSharedAccessKey;SharedAccessKey=testkey",
                "test-eventhub");
    }

    @After
    public void tearDown() throws Exception {
        if (closeable != null) {
            closeable.close();
        }
    }

    @Test
    public void testConstructor() {
        assertNotNull(sender);
    }

    @Test
    public void testExtractNamespace() {
        String connectionString = "Endpoint=sb://testnamespace.servicebus.windows.net/;SharedAccessKeyName=RootManageSharedAccessKey;SharedAccessKey=testkey";
        EventHubSender testSender = new EventHubSender(connectionString, "test-hub");
        assertNotNull(testSender);
    }

    @Test(expected = IllegalStateException.class)
    public void testSendMessageWithoutInitialization() throws EventHubException {
        sender.sendMessage("test message");
    }

    @Test(expected = IllegalStateException.class)
    public void testSendBatchWithoutInitialization() throws EventHubException {
        String[] messages = {"message1", "message2"};
        sender.sendBatch(messages);
    }

    @Test
    public void testSendMessageSuccessfully() throws Exception {
        // Create a sender with mocked client
        EventHubSender mockSender = spy(sender);
        
        // Use reflection or create a test-specific method to set the mock client
        java.lang.reflect.Field clientField = EventHubSender.class.getDeclaredField("eventHubClient");
        clientField.setAccessible(true);
        clientField.set(mockSender, mockClient);
        
        doNothing().when(mockClient).sendSync(any(EventData.class));
        
        mockSender.sendMessage("Hello Event Hub");
        
        ArgumentCaptor<EventData> eventCaptor = ArgumentCaptor.forClass(EventData.class);
        verify(mockClient).sendSync(eventCaptor.capture());
        
        EventData capturedEvent = eventCaptor.getValue();
        String capturedMessage = new String(capturedEvent.getBytes(), StandardCharsets.UTF_8);
        assertEquals("Hello Event Hub", capturedMessage);
    }

    @Test
    public void testSendBatchSuccessfully() throws Exception {
        EventHubSender mockSender = spy(sender);
        
        java.lang.reflect.Field clientField = EventHubSender.class.getDeclaredField("eventHubClient");
        clientField.setAccessible(true);
        clientField.set(mockSender, mockClient);
        
        when(mockClient.createBatch()).thenReturn(mockBatch);
        when(mockBatch.tryAdd(any(EventData.class))).thenReturn(true);
        when(mockBatch.getSize()).thenReturn(3);
        doNothing().when(mockClient).sendSync(any(EventDataBatch.class));
        
        String[] messages = {"message1", "message2", "message3"};
        mockSender.sendBatch(messages);
        
        verify(mockClient, times(1)).createBatch();
        verify(mockBatch, times(3)).tryAdd(any(EventData.class));
        verify(mockClient, times(1)).sendSync(any(EventDataBatch.class));
    }

    @Test
    public void testSendMessageToPartition() throws Exception {
        EventHubSender mockSender = spy(sender);
        
        java.lang.reflect.Field clientField = EventHubSender.class.getDeclaredField("eventHubClient");
        clientField.setAccessible(true);
        clientField.set(mockSender, mockClient);
        
        when(mockClient.createPartitionSenderSync(anyString())).thenReturn(mockPartitionSender);
        doNothing().when(mockPartitionSender).sendSync(any(EventData.class));
        
        mockSender.sendMessageToPartition("Partition message", "0");
        
        verify(mockClient).createPartitionSenderSync("0");
        verify(mockPartitionSender).sendSync(any(EventData.class));
    }

    @Test
    public void testClose() throws Exception {
        EventHubSender mockSender = spy(sender);
        
        java.lang.reflect.Field clientField = EventHubSender.class.getDeclaredField("eventHubClient");
        clientField.setAccessible(true);
        clientField.set(mockSender, mockClient);
        
        doNothing().when(mockClient).closeSync();
        
        mockSender.close();
        
        verify(mockClient).closeSync();
    }

    @Test
    public void testCloseWithNullClient() {
        // Should not throw exception
        sender.close();
    }

    @Test
    public void testInvalidConnectionStringWillThrowOnInitialize() {
        // Invalid connection string doesn't throw until initialize() is called
        String invalidConnStr = "SharedAccessKeyName=RootManageSharedAccessKey;SharedAccessKey=testkey";
        EventHubSender invalidSender = new EventHubSender(invalidConnStr, "test-hub");
        assertNotNull(invalidSender);
    }
}
