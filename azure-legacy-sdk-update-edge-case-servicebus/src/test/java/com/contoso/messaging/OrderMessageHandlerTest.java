package com.contoso.messaging;

import com.azure.core.util.BinaryData;
import com.azure.messaging.servicebus.ServiceBusException;
import com.azure.messaging.servicebus.ServiceBusReceivedMessage;
import com.azure.messaging.servicebus.ServiceBusSenderClient;
import com.azure.messaging.servicebus.ServiceBusMessage;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.lang.reflect.Constructor;
import java.util.HashMap;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

public class OrderMessageHandlerTest {

    @Mock
    private ServiceBusSenderClient mockSender;

    private ErrorClassifier errorClassifier;
    private OrderMessageHandler handler;

    @Before
    public void setUp() {
        MockitoAnnotations.openMocks(this);
        errorClassifier = new ErrorClassifier();
        handler = new OrderMessageHandler(mockSender, errorClassifier);
    }

    @Test
    public void testAcceptForwardsTransformedMessage() {
        ServiceBusReceivedMessage mockInput = mock(ServiceBusReceivedMessage.class);
        when(mockInput.getBody()).thenReturn(BinaryData.fromString("order-123"));
        when(mockInput.getMessageId()).thenReturn("msg-001");
        when(mockInput.getSubject()).thenReturn("new-order");

        MessageTransformer enricher = msg -> {
            ServiceBusMessage enriched = new ServiceBusMessage(msg.getBody());
            enriched.setMessageId(msg.getMessageId());
            enriched.setSubject("processed-" + msg.getSubject());
            return enriched;
        };
        handler.addTransformer(enricher);

        handler.accept(mockInput);

        ArgumentCaptor<ServiceBusMessage> captor = ArgumentCaptor.forClass(ServiceBusMessage.class);
        verify(mockSender).sendMessage(captor.capture());
        ServiceBusMessage forwarded = captor.getValue();
        assertEquals("processed-new-order", forwarded.getSubject());
        assertEquals("msg-001", forwarded.getMessageId());
    }

    @Test
    public void testAcceptWithMultipleTransformers() {
        ServiceBusReceivedMessage mockInput = mock(ServiceBusReceivedMessage.class);
        when(mockInput.getBody()).thenReturn(BinaryData.fromString("data"));
        when(mockInput.getSubject()).thenReturn("raw");
        when(mockInput.getApplicationProperties()).thenReturn(new HashMap<String, Object>());

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

        handler.accept(mockInput);

        ArgumentCaptor<ServiceBusMessage> captor = ArgumentCaptor.forClass(ServiceBusMessage.class);
        verify(mockSender).sendMessage(captor.capture());
        ServiceBusMessage result = captor.getValue();
        assertEquals("raw-enriched", result.getSubject());
        assertTrue((Boolean) result.getApplicationProperties().get("step1"));
        assertTrue((Boolean) result.getApplicationProperties().get("step2"));
    }

    @Test
    public void testAcceptTracksProcessedMessages() {
        ServiceBusReceivedMessage mock1 = mock(ServiceBusReceivedMessage.class);
        when(mock1.getBody()).thenReturn(BinaryData.fromString("order-1"));
        
        ServiceBusReceivedMessage mock2 = mock(ServiceBusReceivedMessage.class);
        when(mock2.getBody()).thenReturn(BinaryData.fromString("order-2"));

        handler.accept(mock1);
        handler.accept(mock2);

        assertEquals(2, handler.getProcessedCount());
        assertEquals(2, handler.getProcessedMessages().size());
    }

    @Test
    public void testHandleErrorClassifiesException() {
        ServiceBusException ex = new ServiceBusException(
            new RuntimeException("Test error"),
            com.azure.messaging.servicebus.ServiceBusErrorSource.UNKNOWN);
        
        handler.handleError(ex);
        // Exception was classified (exact category depends on runtime behavior)
        assertTrue(errorClassifier.getSnapshot().values().stream().anyMatch(count -> count > 0));
    }
}
