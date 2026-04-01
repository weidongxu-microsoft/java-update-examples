package com.contoso.messaging;

import com.azure.core.util.BinaryData;
import com.azure.core.util.IterableStream;
import com.azure.messaging.servicebus.ServiceBusReceivedMessage;
import com.azure.messaging.servicebus.ServiceBusReceiverClient;
import com.azure.messaging.servicebus.ServiceBusSenderClient;
import com.azure.messaging.servicebus.ServiceBusMessage;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.time.Duration;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

public class MessageRouterTest {

    @Mock
    private ServiceBusReceiverClient mockReceiver;

    @Mock
    private ServiceBusSenderClient mockOrdersSender;

    @Mock
    private ServiceBusSenderClient mockAlertsSender;

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
    public void testReceiveAndRouteToCorrectSender() {
        ServiceBusReceivedMessage mockMsg = mock(ServiceBusReceivedMessage.class);
        when(mockMsg.getSubject()).thenReturn("orders");
        when(mockMsg.getBody()).thenReturn(BinaryData.fromString("order-data"));
        when(mockMsg.getMessageId()).thenReturn("msg-1");

        when(mockReceiver.receiveMessages(1, Duration.ofSeconds(5)))
            .thenReturn(new IterableStream<>(Arrays.asList(mockMsg)));

        ServiceBusReceivedMessage result = router.receiveAndRoute();

        verify(mockOrdersSender).sendMessage(any(ServiceBusMessage.class));
        verify(mockReceiver).complete(mockMsg);
        assertNotNull(result);
    }

    @Test
    public void testReceiveAndRouteAbandonsUnmatchedLabel() {
        ServiceBusReceivedMessage mockMsg = mock(ServiceBusReceivedMessage.class);
        when(mockMsg.getSubject()).thenReturn("unknown-label");

        when(mockReceiver.receiveMessages(1, Duration.ofSeconds(5)))
            .thenReturn(new IterableStream<>(Arrays.asList(mockMsg)));

        ServiceBusReceivedMessage result = router.receiveAndRoute();

        verify(mockReceiver).abandon(mockMsg);
        verifyNoInteractions(mockOrdersSender, mockAlertsSender);
        assertNotNull(result);
    }

    @Test
    public void testReceiveAndRouteHandlesNullMessage() {
        when(mockReceiver.receiveMessages(1, Duration.ofSeconds(5)))
            .thenReturn(new IterableStream<>(Collections.emptyList()));

        ServiceBusReceivedMessage result = router.receiveAndRoute();

        assertNull(result);
        verifyNoInteractions(mockOrdersSender);
    }

    @Test
    public void testReceiveAndRouteBatchWithTransformer() {
        ServiceBusReceivedMessage mockMsg1 = mock(ServiceBusReceivedMessage.class);
        when(mockMsg1.getSubject()).thenReturn("raw");
        when(mockMsg1.getBody()).thenReturn(BinaryData.fromString("data1"));
        when(mockMsg1.getMessageId()).thenReturn("msg-1");

        ServiceBusReceivedMessage mockMsg2 = mock(ServiceBusReceivedMessage.class);
        when(mockMsg2.getSubject()).thenReturn("raw");
        when(mockMsg2.getBody()).thenReturn(BinaryData.fromString("data2"));
        when(mockMsg2.getMessageId()).thenReturn("msg-2");

        when(mockReceiver.receiveMessages(10, Duration.ofSeconds(10)))
            .thenReturn(new IterableStream<>(Arrays.asList(mockMsg1, mockMsg2)));

        MessageTransformer labeler = msg -> {
            ServiceBusMessage m = new ServiceBusMessage(msg.getBody());
            m.setSubject("orders");
            return m;
        };

        List<ServiceBusReceivedMessage> results = router.receiveAndRouteBatch(10, labeler);

        assertEquals(2, results.size());
        verify(mockOrdersSender, times(2)).sendMessage(any(ServiceBusMessage.class));
    }

    @Test
    public void testReceiveAndTransformRoutesToCorrectSender() {
        ServiceBusReceivedMessage mockMsg = mock(ServiceBusReceivedMessage.class);
        when(mockMsg.getSubject()).thenReturn("raw");
        when(mockMsg.getBody()).thenReturn(BinaryData.fromString("payload"));
        when(mockMsg.getMessageId()).thenReturn("msg-1");

        when(mockReceiver.receiveMessages(1, Duration.ofSeconds(5)))
            .thenReturn(new IterableStream<>(Arrays.asList(mockMsg)));

        MessageTransformer relabeler = msg -> {
            ServiceBusMessage m = new ServiceBusMessage(msg.getBody());
            m.setSubject("orders");
            return m;
        };

        ServiceBusReceivedMessage result = router.receiveAndTransform(relabeler);

        assertNotNull(result);
        verify(mockOrdersSender).sendMessage(any(ServiceBusMessage.class));
        verify(mockReceiver).complete(mockMsg);
    }
}
