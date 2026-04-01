package com.contoso.messaging;

import com.microsoft.azure.servicebus.ExceptionPhase;
import com.microsoft.azure.servicebus.IMessage;
import com.microsoft.azure.servicebus.IMessageSender;
import com.microsoft.azure.servicebus.Message;
import com.microsoft.azure.servicebus.primitives.ServiceBusException;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

public class OrderMessageHandlerTest {

    @Mock
    private IMessageSender mockSender;

    private ErrorClassifier errorClassifier;
    private OrderMessageHandler handler;

    @Before
    public void setUp() {
        MockitoAnnotations.openMocks(this);
        errorClassifier = new ErrorClassifier();
        handler = new OrderMessageHandler(mockSender, errorClassifier);
    }

    @Test
    public void testOnMessageForwardsTransformedMessage() throws Exception {
        when(mockSender.sendAsync(any(IMessage.class)))
            .thenReturn(CompletableFuture.completedFuture(null));

        Message input = new Message("order-123".getBytes(StandardCharsets.UTF_8));
        input.setMessageId("msg-001");
        input.setLabel("new-order");

        MessageTransformer enricher = msg -> {
            Message enriched = new Message(msg.getBody());
            enriched.setMessageId(msg.getMessageId());
            enriched.setLabel("processed-" + msg.getLabel());
            return enriched;
        };
        handler.addTransformer(enricher);

        handler.onMessageAsync(input).join();

        ArgumentCaptor<IMessage> captor = ArgumentCaptor.forClass(IMessage.class);
        verify(mockSender).sendAsync(captor.capture());
        IMessage forwarded = captor.getValue();
        assertEquals("processed-new-order", forwarded.getLabel());
        assertEquals("msg-001", forwarded.getMessageId());
    }

    @Test
    public void testOnMessageWithMultipleTransformers() throws Exception {
        when(mockSender.sendAsync(any(IMessage.class)))
            .thenReturn(CompletableFuture.completedFuture(null));

        Message input = new Message("data".getBytes(StandardCharsets.UTF_8));
        input.setLabel("raw");
        input.setProperties(new HashMap<String, Object>());

        handler.addTransformer(msg -> {
            Message m = new Message(msg.getBody());
            m.setLabel(msg.getLabel());
            Map<String, Object> props = new HashMap<>(msg.getProperties());
            props.put("step1", true);
            m.setProperties(props);
            return m;
        });
        handler.addTransformer(msg -> {
            Message m = new Message(msg.getBody());
            m.setLabel(msg.getLabel() + "-enriched");
            Map<String, Object> props = new HashMap<>(msg.getProperties());
            props.put("step2", true);
            m.setProperties(props);
            return m;
        });

        handler.onMessageAsync(input).join();

        ArgumentCaptor<IMessage> captor = ArgumentCaptor.forClass(IMessage.class);
        verify(mockSender).sendAsync(captor.capture());
        IMessage result = captor.getValue();
        assertEquals("raw-enriched", result.getLabel());
        assertTrue((Boolean) result.getProperties().get("step1"));
        assertTrue((Boolean) result.getProperties().get("step2"));
    }

    @Test
    public void testOnMessageTracksProcessedMessages() throws Exception {
        when(mockSender.sendAsync(any(IMessage.class)))
            .thenReturn(CompletableFuture.completedFuture(null));

        Message msg1 = new Message("order-1".getBytes(StandardCharsets.UTF_8));
        Message msg2 = new Message("order-2".getBytes(StandardCharsets.UTF_8));

        handler.onMessageAsync(msg1).join();
        handler.onMessageAsync(msg2).join();

        assertEquals(2, handler.getProcessedCount());
        assertEquals(2, handler.getProcessedMessages().size());
    }

    @Test
    public void testOnMessageHandlesSendFailure() throws Exception {
        CompletableFuture<Void> failedFuture = new CompletableFuture<>();
        failedFuture.completeExceptionally(
            new com.microsoft.azure.servicebus.primitives.ServerBusyException("Server throttled"));
        when(mockSender.sendAsync(any(IMessage.class))).thenReturn(failedFuture);

        Message input = new Message("data".getBytes(StandardCharsets.UTF_8));
        handler.onMessageAsync(input).join();

        assertEquals(1, errorClassifier.getErrorCount(ErrorClassifier.ErrorCategory.THROTTLED));
    }

    @Test
    public void testNotifyExceptionClassifiesTransient() {
        ServiceBusException transientEx = new ServiceBusException(true, "Transient");
        handler.notifyException(transientEx, ExceptionPhase.RECEIVE);
        assertTrue(errorClassifier.hasTransientErrors());
    }

    @Test
    public void testNotifyExceptionClassifiesEntityNotFound() {
        com.microsoft.azure.servicebus.primitives.MessagingEntityNotFoundException notFoundEx =
            new com.microsoft.azure.servicebus.primitives.MessagingEntityNotFoundException("Queue not found");
        handler.notifyException(notFoundEx, ExceptionPhase.RECEIVE);
        assertEquals(1, errorClassifier.getErrorCount(ErrorClassifier.ErrorCategory.ENTITY_NOT_FOUND));
    }

    @Test
    public void testNotifyExceptionClassifiesLockLost() {
        com.microsoft.azure.servicebus.primitives.MessageLockLostException lockEx =
            new com.microsoft.azure.servicebus.primitives.MessageLockLostException("Lock expired");
        handler.notifyException(lockEx, ExceptionPhase.RECEIVE);
        assertEquals(1, errorClassifier.getErrorCount(ErrorClassifier.ErrorCategory.LOCK_LOST));
    }
}
