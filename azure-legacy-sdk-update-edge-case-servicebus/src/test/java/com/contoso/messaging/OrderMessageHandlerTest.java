package com.contoso.messaging;

import com.azure.core.amqp.exception.AmqpErrorCondition;
import com.azure.core.amqp.exception.AmqpException;
import com.azure.messaging.servicebus.ServiceBusErrorSource;
import com.azure.messaging.servicebus.ServiceBusException;
import com.azure.messaging.servicebus.ServiceBusFailureReason;
import com.azure.messaging.servicebus.ServiceBusMessage;
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
    private MessageSender mockSender;

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
        doNothing().when(mockSender).send(any(ServiceBusMessage.class));
        when(mockSender.sendAsync(any(ServiceBusMessage.class)))
            .thenReturn(CompletableFuture.completedFuture(null));

        ServiceBusMessage input = new ServiceBusMessage("order-123".getBytes(StandardCharsets.UTF_8));
        input.setMessageId("msg-001");
        input.setSubject("new-order");

        MessageTransformer enricher = msg -> {
            ServiceBusMessage enriched = new ServiceBusMessage(msg.getBody());
            enriched.setMessageId(msg.getMessageId());
            enriched.setSubject("processed-" + msg.getSubject());
            return enriched;
        };
        handler.addTransformer(enricher);

        handler.onMessageAsync(input).join();

        ArgumentCaptor<ServiceBusMessage> captor = ArgumentCaptor.forClass(ServiceBusMessage.class);
        verify(mockSender).sendAsync(captor.capture());
        ServiceBusMessage forwarded = captor.getValue();
        assertEquals("processed-new-order", forwarded.getSubject());
        assertEquals("msg-001", forwarded.getMessageId());
    }

    @Test
    public void testOnMessageWithMultipleTransformers() throws Exception {
        doNothing().when(mockSender).send(any(ServiceBusMessage.class));
        when(mockSender.sendAsync(any(ServiceBusMessage.class)))
            .thenReturn(CompletableFuture.completedFuture(null));

        ServiceBusMessage input = new ServiceBusMessage("data".getBytes(StandardCharsets.UTF_8));
        input.setSubject("raw");

        handler.addTransformer(msg -> {
            ServiceBusMessage m = new ServiceBusMessage(msg.getBody());
            m.setSubject(msg.getSubject());
            m.getApplicationProperties().putAll(msg.getApplicationProperties());
            m.getApplicationProperties().put("step1", true);
            return m;
        });
        handler.addTransformer(msg -> {
            ServiceBusMessage m = new ServiceBusMessage(msg.getBody());
            m.setSubject(msg.getSubject() + "-enriched");
            m.getApplicationProperties().putAll(msg.getApplicationProperties());
            m.getApplicationProperties().put("step2", true);
            return m;
        });

        handler.onMessageAsync(input).join();

        ArgumentCaptor<ServiceBusMessage> captor = ArgumentCaptor.forClass(ServiceBusMessage.class);
        verify(mockSender).sendAsync(captor.capture());
        ServiceBusMessage result = captor.getValue();
        assertEquals("raw-enriched", result.getSubject());
        assertTrue((Boolean) result.getApplicationProperties().get("step1"));
        assertTrue((Boolean) result.getApplicationProperties().get("step2"));
    }

    @Test
    public void testOnMessageTracksProcessedMessages() throws Exception {
        doNothing().when(mockSender).send(any(ServiceBusMessage.class));
        when(mockSender.sendAsync(any(ServiceBusMessage.class)))
            .thenReturn(CompletableFuture.completedFuture(null));

        ServiceBusMessage msg1 = new ServiceBusMessage("order-1".getBytes(StandardCharsets.UTF_8));
        ServiceBusMessage msg2 = new ServiceBusMessage("order-2".getBytes(StandardCharsets.UTF_8));

        handler.onMessageAsync(msg1).join();
        handler.onMessageAsync(msg2).join();

        assertEquals(2, handler.getProcessedCount());
        assertEquals(2, handler.getProcessedMessages().size());
    }

    @Test
    public void testOnMessageHandlesSendFailure() throws Exception {
        CompletableFuture<Void> failedFuture = new CompletableFuture<>();
        failedFuture.completeExceptionally(
            new ServiceBusException(
                new AmqpException(true, AmqpErrorCondition.SERVER_BUSY_ERROR, "Server throttled", null),
                ServiceBusErrorSource.RECEIVE));
        when(mockSender.sendAsync(any(ServiceBusMessage.class))).thenReturn(failedFuture);

        ServiceBusMessage input = new ServiceBusMessage("data".getBytes(StandardCharsets.UTF_8));
        handler.onMessageAsync(input).join();

        assertEquals(1, errorClassifier.getErrorCount(ErrorClassifier.ErrorCategory.THROTTLED));
    }

    @Test
    public void testNotifyExceptionClassifiesTransient() {
        ServiceBusException transientEx = new ServiceBusException(
            new AmqpException(true, AmqpErrorCondition.TIMEOUT_ERROR, "Transient", null),
            ServiceBusErrorSource.RECEIVE);
        handler.notifyException(transientEx, ErrorPhase.RECEIVE);
        assertTrue(errorClassifier.hasTransientErrors());
    }

    @Test
    public void testNotifyExceptionClassifiesEntityNotFound() {
        ServiceBusException notFoundEx = new ServiceBusException(
            new AmqpException(false, AmqpErrorCondition.NOT_FOUND, "Queue not found", null),
            ServiceBusErrorSource.RECEIVE);
        handler.notifyException(notFoundEx, ErrorPhase.RECEIVE);
        assertEquals(1, errorClassifier.getErrorCount(ErrorClassifier.ErrorCategory.ENTITY_NOT_FOUND));
    }

    @Test
    public void testNotifyExceptionClassifiesLockLost() {
        ServiceBusException lockEx = new ServiceBusException(
            new AmqpException(false, AmqpErrorCondition.MESSAGE_LOCK_LOST, "Lock expired", null),
            ServiceBusErrorSource.RECEIVE);
        handler.notifyException(lockEx, ErrorPhase.RECEIVE);
        assertEquals(1, errorClassifier.getErrorCount(ErrorClassifier.ErrorCategory.LOCK_LOST));
    }
}
