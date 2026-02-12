package com.azure.servicebus.client;

import com.microsoft.azure.servicebus.IMessage;
import com.microsoft.azure.servicebus.IMessageSender;
import com.microsoft.azure.servicebus.primitives.ServiceBusException;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import static org.junit.Assert.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class ServiceBusSenderTest {

    @Mock
    private IMessageSender mockMessageSender;

    private ServiceBusSender sender;
    private AutoCloseable closeable;

    @Before
    public void setUp() {
        closeable = MockitoAnnotations.openMocks(this);
        sender = new ServiceBusSender(
                "Endpoint=sb://namespace.servicebus.windows.net/;SharedAccessKeyName=RootManageSharedAccessKey;SharedAccessKey=testkey",
                "orders-queue");
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

    @Test(expected = IllegalStateException.class)
    public void testSendMessageWithoutInitialization() throws ServiceBusException, InterruptedException {
        sender.sendMessage("test message");
    }

    @Test(expected = IllegalStateException.class)
    public void testSendBatchWithoutInitialization() throws ServiceBusException, InterruptedException {
        sender.sendBatch(Arrays.asList("message1", "message2"));
    }

    @Test(expected = IllegalStateException.class)
    public void testSendMessageAsyncWithoutInitialization() {
        sender.sendMessageAsync("test message");
    }

    @Test
    public void testSendMessageSuccessfully() throws Exception {
        injectMockSender();
        doNothing().when(mockMessageSender).send(any(IMessage.class));

        sender.sendMessage("Hello Service Bus");

        verify(mockMessageSender).send(any(IMessage.class));
    }

    @Test
    public void testSendMessageAsyncSuccessfully() throws Exception {
        injectMockSender();
        when(mockMessageSender.sendAsync(any(IMessage.class)))
                .thenReturn(CompletableFuture.completedFuture(null));

        CompletableFuture<Void> future = sender.sendMessageAsync("Hello async");
        assertNotNull(future);
        future.get();

        verify(mockMessageSender).sendAsync(any(IMessage.class));
    }

    @Test
    public void testSendMessageWithProperties() throws Exception {
        injectMockSender();
        doNothing().when(mockMessageSender).send(any(IMessage.class));

        sender.sendMessageWithProperties("Priority order", "high-priority", Duration.ofMinutes(30));

        verify(mockMessageSender).send(any(IMessage.class));
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testSendBatchSuccessfully() throws Exception {
        injectMockSender();
        doNothing().when(mockMessageSender).sendBatch(any(Collection.class));

        List<String> messages = Arrays.asList("Batch msg 1", "Batch msg 2", "Batch msg 3");
        sender.sendBatch(messages);

        verify(mockMessageSender).sendBatch(any(Collection.class));
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testSendBatchAsyncSuccessfully() throws Exception {
        injectMockSender();
        when(mockMessageSender.sendBatchAsync(any(Collection.class)))
                .thenReturn(CompletableFuture.completedFuture(null));

        List<String> messages = Arrays.asList("Async batch 1", "Async batch 2");
        CompletableFuture<Void> future = sender.sendBatchAsync(messages);
        assertNotNull(future);
        future.get();

        verify(mockMessageSender).sendBatchAsync(any(Collection.class));
    }

    @Test
    public void testScheduleMessageAsync() throws Exception {
        injectMockSender();
        when(mockMessageSender.scheduleMessageAsync(any(IMessage.class), any(Instant.class)))
                .thenReturn(CompletableFuture.completedFuture(42L));

        CompletableFuture<Long> future = sender.scheduleMessageAsync("Scheduled msg", Instant.now().plusSeconds(60));
        assertNotNull(future);
        Long sequenceNumber = future.get();
        assertNotNull(sequenceNumber);

        verify(mockMessageSender).scheduleMessageAsync(any(IMessage.class), any(Instant.class));
    }

    @Test
    public void testCancelScheduledMessageAsync() throws Exception {
        injectMockSender();
        when(mockMessageSender.cancelScheduledMessageAsync(anyLong()))
                .thenReturn(CompletableFuture.completedFuture(null));

        CompletableFuture<Void> future = sender.cancelScheduledMessageAsync(42L);
        assertNotNull(future);
        future.get();

        verify(mockMessageSender).cancelScheduledMessageAsync(42L);
    }

    @Test
    public void testCloseWithMockSender() throws Exception {
        injectMockSender();
        doNothing().when(mockMessageSender).close();

        sender.close();

        verify(mockMessageSender).close();
    }

    @Test
    public void testCloseWithNullSender() {
        // Should not throw
        sender.close();
    }

    @Test
    public void testMultipleSendCalls() throws Exception {
        injectMockSender();
        doNothing().when(mockMessageSender).send(any(IMessage.class));

        sender.sendMessage("Message 1");
        sender.sendMessage("Message 2");
        sender.sendMessage("Message 3");

        verify(mockMessageSender, times(3)).send(any(IMessage.class));
    }

    private void injectMockSender() throws Exception {
        java.lang.reflect.Field senderField = ServiceBusSender.class.getDeclaredField("messageSender");
        senderField.setAccessible(true);
        senderField.set(sender, mockMessageSender);
    }
}
