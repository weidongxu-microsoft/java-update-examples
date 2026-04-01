package com.contoso.messaging;

import com.azure.messaging.servicebus.ServiceBusMessage;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;

import static org.junit.Assert.*;

public class QueueSessionManagerTest {

    @Mock
    private MessageSender mockSender;

    private ErrorClassifier errorClassifier;
    private QueueSessionManager manager;

    @Before
    public void setUp() {
        MockitoAnnotations.openMocks(this);
        errorClassifier = new ErrorClassifier();
        manager = new QueueSessionManager(errorClassifier);
    }

    @Test
    public void testGetActiveQueueCountStartsAtZero() {
        assertEquals(0, manager.getActiveQueueCount());
    }

    @Test
    public void testGetQueueReturnsNullForUnknown() {
        assertNull(manager.getQueue("nonexistent"));
    }

    @Test
    public void testConnectionStringPropertiesParsesEntityPath() {
        ConnectionStringProperties props = new ConnectionStringProperties(
            "Endpoint=sb://test.servicebus.windows.net/;" +
            "SharedAccessKeyName=RootManageSharedAccessKey;" +
            "SharedAccessKey=dGVzdGtleQ==;" +
            "EntityPath=test-queue");
        assertEquals("test-queue", props.getEntityPath());
    }

    @Test
    public void testMessageTransformerAsLambda() throws Exception {
        MessageTransformer upper = msg -> {
            String body = new String(msg.getBody().toBytes(), StandardCharsets.UTF_8);
            ServiceBusMessage transformed = new ServiceBusMessage(body.toUpperCase().getBytes(StandardCharsets.UTF_8));
            transformed.setSubject(msg.getSubject());
            transformed.setMessageId(msg.getMessageId());
            return transformed;
        };

        ServiceBusMessage input = new ServiceBusMessage("hello".getBytes(StandardCharsets.UTF_8));
        input.setSubject("test");
        input.setMessageId("id-1");

        ServiceBusMessage result = upper.transform(input);
        assertEquals("HELLO", new String(result.getBody().toBytes(), StandardCharsets.UTF_8));
        assertEquals("test", result.getSubject());
    }

    @Test
    public void testChainedTransformers() throws Exception {
        MessageTransformer addLabel = msg -> {
            ServiceBusMessage m = new ServiceBusMessage(msg.getBody());
            m.setSubject("processed");
            return m;
        };

        MessageTransformer addProperty = msg -> {
            ServiceBusMessage m = new ServiceBusMessage(msg.getBody());
            m.setSubject(msg.getSubject());
            m.getApplicationProperties().put("transformed", true);
            return m;
        };

        ServiceBusMessage input = new ServiceBusMessage("data".getBytes(StandardCharsets.UTF_8));
        ServiceBusMessage step1 = addLabel.transform(input);
        ServiceBusMessage step2 = addProperty.transform(step1);

        assertEquals("processed", step2.getSubject());
        assertTrue((Boolean) step2.getApplicationProperties().get("transformed"));
    }

    @Test
    public void testConnectionStringPropertiesEndpoint() {
        ConnectionStringProperties props = new ConnectionStringProperties(
            "Endpoint=sb://contoso.servicebus.windows.net/;" +
            "SharedAccessKeyName=send-key;" +
            "SharedAccessKey=dGVzdGtleQ==;" +
            "EntityPath=orders");
        assertEquals("orders", props.getEntityPath());
        assertNotNull(props.getEndpoint());
    }
}
