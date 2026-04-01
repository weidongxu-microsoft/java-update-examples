package com.contoso.messaging;

import com.azure.core.amqp.exception.AmqpErrorCondition;
import com.azure.core.amqp.exception.AmqpException;
import com.azure.messaging.servicebus.ServiceBusErrorSource;
import com.azure.messaging.servicebus.ServiceBusMessage;
import com.azure.messaging.servicebus.ServiceBusReceivedMessage;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

public class MessageRouterTest {

    @Mock
    private MessageReceiver mockReceiver;

    @Mock
    private MessageSender mockOrdersSender;

    @Mock
    private MessageSender mockAlertsSender;

    private ErrorClassifier errorClassifier;
    private MessageRouter router;

    @Before
    public void setUp() {
        MockitoAnnotations.openMocks(this);
        errorClassifier = new ErrorClassifier();
        router = new MessageRouter(mockReceiver, errorClassifier);
        router.addRoute("orders", mockOrdersSender);
        router.addRoute("alerts", mockAlertsSender);
    }

    @Test
    public void testReceiveAndRouteToCorrectSender() throws Exception {
        ServiceBusReceivedMessage mockMsg = mock(ServiceBusReceivedMessage.class);
        when(mockMsg.getSubject()).thenReturn("orders");
        when(mockMsg.getBody()).thenReturn(com.azure.core.util.BinaryData.fromBytes(
            "order-data".getBytes(StandardCharsets.UTF_8)));
        when(mockMsg.getApplicationProperties()).thenReturn(new java.util.HashMap<>());

        when(mockReceiver.receiveAsync())
            .thenReturn(CompletableFuture.completedFuture(mockMsg));
        doNothing().when(mockOrdersSender).send(any(ServiceBusMessage.class));
        when(mockOrdersSender.sendAsync(any(ServiceBusMessage.class)))
            .thenReturn(CompletableFuture.completedFuture(null));
        when(mockReceiver.completeAsync(mockMsg))
            .thenReturn(CompletableFuture.completedFuture(null));

        ServiceBusMessage result = router.receiveAndRoute().join();

        verify(mockOrdersSender).sendAsync(any(ServiceBusMessage.class));
        verify(mockReceiver).completeAsync(mockMsg);
        assertNotNull(result);
    }

    @Test
    public void testReceiveAndRouteAbandonsUnmatchedLabel() throws Exception {
        ServiceBusReceivedMessage mockMsg = mock(ServiceBusReceivedMessage.class);
        when(mockMsg.getSubject()).thenReturn("unknown-label");
        when(mockMsg.getBody()).thenReturn(com.azure.core.util.BinaryData.fromBytes(new byte[0]));
        when(mockMsg.getApplicationProperties()).thenReturn(new java.util.HashMap<>());

        when(mockReceiver.receiveAsync())
            .thenReturn(CompletableFuture.completedFuture(mockMsg));
        when(mockReceiver.abandonAsync(mockMsg))
            .thenReturn(CompletableFuture.completedFuture(null));

        ServiceBusMessage result = router.receiveAndRoute().join();

        verify(mockReceiver).abandonAsync(mockMsg);
        verifyNoInteractions(mockOrdersSender, mockAlertsSender);
        assertNotNull(result);
    }

    @Test
    public void testReceiveAndRouteHandlesNullMessage() throws Exception {
        when(mockReceiver.receiveAsync())
            .thenReturn(CompletableFuture.completedFuture(null));

        ServiceBusMessage result = router.receiveAndRoute().join();

        assertNull(result);
        verifyNoInteractions(mockOrdersSender);
    }

    @Test
    public void testReceiveAndRouteBatchWithTransformer() throws Exception {
        ServiceBusReceivedMessage mockMsg1 = mock(ServiceBusReceivedMessage.class);
        when(mockMsg1.getSubject()).thenReturn("raw");
        when(mockMsg1.getBody()).thenReturn(com.azure.core.util.BinaryData.fromBytes(
            "data1".getBytes(StandardCharsets.UTF_8)));
        when(mockMsg1.getApplicationProperties()).thenReturn(new java.util.HashMap<>());

        ServiceBusReceivedMessage mockMsg2 = mock(ServiceBusReceivedMessage.class);
        when(mockMsg2.getSubject()).thenReturn("raw");
        when(mockMsg2.getBody()).thenReturn(com.azure.core.util.BinaryData.fromBytes(
            "data2".getBytes(StandardCharsets.UTF_8)));
        when(mockMsg2.getApplicationProperties()).thenReturn(new java.util.HashMap<>());

        Collection<ServiceBusReceivedMessage> batch = Arrays.asList(mockMsg1, mockMsg2);
        when(mockReceiver.receiveBatchAsync(10))
            .thenReturn(CompletableFuture.completedFuture(batch));
        doNothing().when(mockOrdersSender).send(any(ServiceBusMessage.class));
        when(mockOrdersSender.sendAsync(any(ServiceBusMessage.class)))
            .thenReturn(CompletableFuture.completedFuture(null));
        when(mockReceiver.completeAsync(any(ServiceBusReceivedMessage.class)))
            .thenReturn(CompletableFuture.completedFuture(null));

        MessageTransformer labeler = msg -> {
            ServiceBusMessage m = new ServiceBusMessage(msg.getBody());
            m.setSubject("orders");
            return m;
        };

        List<ServiceBusMessage> results = router.receiveAndRouteBatch(10, labeler).join();

        assertEquals(2, results.size());
        verify(mockOrdersSender, times(2)).sendAsync(any(ServiceBusMessage.class));
    }

    @Test
    public void testReceiveAndTransformDeadLettersOnFailure() throws Exception {
        ServiceBusReceivedMessage mockMsg = mock(ServiceBusReceivedMessage.class);
        when(mockMsg.getSubject()).thenReturn("orders");
        when(mockMsg.getBody()).thenReturn(com.azure.core.util.BinaryData.fromBytes(new byte[0]));
        when(mockMsg.getApplicationProperties()).thenReturn(new java.util.HashMap<>());

        when(mockReceiver.receive()).thenReturn(mockMsg);

        MessageTransformer failingTransformer = msg -> {
            throw new com.azure.messaging.servicebus.ServiceBusException(
                new AmqpException(false, AmqpErrorCondition.INTERNAL_ERROR, "Transform failed", null),
                ServiceBusErrorSource.RECEIVE);
        };

        try {
            router.receiveAndTransform(failingTransformer);
            fail("Expected Exception");
        } catch (Exception e) {
            assertTrue(e.getMessage().contains("Transform failed"));
        }

        verify(mockReceiver).deadLetter(eq(mockMsg), eq("TransformFailed"), anyString());
    }

    @Test
    public void testReceiveAndTransformRoutesToCorrectSender() throws Exception {
        ServiceBusReceivedMessage mockMsg = mock(ServiceBusReceivedMessage.class);
        when(mockMsg.getSubject()).thenReturn("raw");
        when(mockMsg.getBody()).thenReturn(com.azure.core.util.BinaryData.fromBytes(
            "payload".getBytes(StandardCharsets.UTF_8)));
        when(mockMsg.getApplicationProperties()).thenReturn(new java.util.HashMap<>());

        when(mockReceiver.receive()).thenReturn(mockMsg);

        MessageTransformer relabeler = msg -> {
            ServiceBusMessage m = new ServiceBusMessage(msg.getBody());
            m.setSubject("orders");
            return m;
        };

        ServiceBusMessage result = router.receiveAndTransform(relabeler);

        assertNotNull(result);
        verify(mockOrdersSender).send(any(ServiceBusMessage.class));
        verify(mockReceiver).complete(mockMsg);
    }
}
