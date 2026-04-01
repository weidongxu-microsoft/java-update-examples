package com.contoso.messaging;

import com.microsoft.azure.servicebus.IMessage;
import com.microsoft.azure.servicebus.IMessageReceiver;
import com.microsoft.azure.servicebus.IMessageSender;
import com.microsoft.azure.servicebus.Message;
import com.microsoft.azure.servicebus.primitives.ServiceBusException;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

public class MessageRouterTest {

    @Mock
    private IMessageReceiver mockReceiver;

    @Mock
    private IMessageSender mockOrdersSender;

    @Mock
    private IMessageSender mockAlertsSender;

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
        UUID lockToken = UUID.randomUUID();
        IMessage mockMsg = mock(IMessage.class);
        when(mockMsg.getLabel()).thenReturn("orders");
        when(mockMsg.getLockToken()).thenReturn(lockToken);
        when(mockMsg.getBody()).thenReturn("order-data".getBytes(StandardCharsets.UTF_8));

        when(mockReceiver.receiveAsync())
            .thenReturn(CompletableFuture.completedFuture(mockMsg));
        when(mockOrdersSender.sendAsync(any(IMessage.class)))
            .thenReturn(CompletableFuture.completedFuture(null));
        when(mockReceiver.completeAsync(lockToken))
            .thenReturn(CompletableFuture.completedFuture(null));

        IMessage result = router.receiveAndRoute().join();

        verify(mockOrdersSender).sendAsync(mockMsg);
        verify(mockReceiver).completeAsync(lockToken);
        assertNotNull(result);
    }

    @Test
    public void testReceiveAndRouteAbandonsUnmatchedLabel() throws Exception {
        UUID lockToken = UUID.randomUUID();
        IMessage mockMsg = mock(IMessage.class);
        when(mockMsg.getLabel()).thenReturn("unknown-label");
        when(mockMsg.getLockToken()).thenReturn(lockToken);

        when(mockReceiver.receiveAsync())
            .thenReturn(CompletableFuture.completedFuture(mockMsg));
        when(mockReceiver.abandonAsync(lockToken))
            .thenReturn(CompletableFuture.completedFuture(null));

        IMessage result = router.receiveAndRoute().join();

        verify(mockReceiver).abandonAsync(lockToken);
        verifyNoInteractions(mockOrdersSender, mockAlertsSender);
        assertNotNull(result);
    }

    @Test
    public void testReceiveAndRouteHandlesNullMessage() throws Exception {
        when(mockReceiver.receiveAsync())
            .thenReturn(CompletableFuture.completedFuture(null));

        IMessage result = router.receiveAndRoute().join();

        assertNull(result);
        verifyNoInteractions(mockOrdersSender);
    }

    @Test
    public void testReceiveAndRouteBatchWithTransformer() throws Exception {
        UUID lock1 = UUID.randomUUID();
        UUID lock2 = UUID.randomUUID();

        IMessage mockMsg1 = mock(IMessage.class);
        when(mockMsg1.getLabel()).thenReturn("raw");
        when(mockMsg1.getLockToken()).thenReturn(lock1);
        when(mockMsg1.getBody()).thenReturn("data1".getBytes(StandardCharsets.UTF_8));

        IMessage mockMsg2 = mock(IMessage.class);
        when(mockMsg2.getLabel()).thenReturn("raw");
        when(mockMsg2.getLockToken()).thenReturn(lock2);
        when(mockMsg2.getBody()).thenReturn("data2".getBytes(StandardCharsets.UTF_8));

        Collection<IMessage> batch = Arrays.<IMessage>asList(mockMsg1, mockMsg2);
        when(mockReceiver.receiveBatchAsync(10))
            .thenReturn(CompletableFuture.completedFuture(batch));
        when(mockOrdersSender.sendAsync(any(IMessage.class)))
            .thenReturn(CompletableFuture.completedFuture(null));
        when(mockReceiver.completeAsync(any(UUID.class)))
            .thenReturn(CompletableFuture.completedFuture(null));

        MessageTransformer labeler = msg -> {
            Message m = new Message(msg.getBody());
            m.setLabel("orders");
            return m;
        };

        List<IMessage> results = router.receiveAndRouteBatch(10, labeler).join();

        assertEquals(2, results.size());
        verify(mockOrdersSender, times(2)).sendAsync(any(IMessage.class));
    }

    @Test
    public void testReceiveAndTransformDeadLettersOnFailure() throws Exception {
        UUID lockToken = UUID.randomUUID();
        IMessage mockMsg = mock(IMessage.class);
        when(mockMsg.getLabel()).thenReturn("orders");
        when(mockMsg.getLockToken()).thenReturn(lockToken);

        when(mockReceiver.receive()).thenReturn(mockMsg);

        MessageTransformer failingTransformer = msg -> {
            throw new ServiceBusException(false, "Transform failed");
        };

        try {
            router.receiveAndTransform(failingTransformer);
            fail("Expected ServiceBusException");
        } catch (ServiceBusException e) {
            assertEquals("Transform failed", e.getMessage());
        }

        verify(mockReceiver).deadLetter(eq(lockToken), eq("TransformFailed"), eq("Transform failed"));
    }

    @Test
    public void testReceiveAndTransformRoutesToCorrectSender() throws Exception {
        UUID lockToken = UUID.randomUUID();
        IMessage mockMsg = mock(IMessage.class);
        when(mockMsg.getLabel()).thenReturn("raw");
        when(mockMsg.getLockToken()).thenReturn(lockToken);
        when(mockMsg.getBody()).thenReturn("payload".getBytes(StandardCharsets.UTF_8));

        when(mockReceiver.receive()).thenReturn(mockMsg);

        MessageTransformer relabeler = msg -> {
            Message m = new Message(msg.getBody());
            m.setLabel("orders");
            return m;
        };

        IMessage result = router.receiveAndTransform(relabeler);

        assertNotNull(result);
        verify(mockOrdersSender).send(any(IMessage.class));
        verify(mockReceiver).complete(lockToken);
    }
}
