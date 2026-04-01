package com.contoso.messaging;

import com.microsoft.azure.servicebus.IMessage;
import com.microsoft.azure.servicebus.Message;
import com.microsoft.azure.servicebus.primitives.ConnectionStringBuilder;
import com.microsoft.azure.servicebus.primitives.ServiceBusException;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;

import static org.junit.Assert.*;

public class QueueSessionManagerTest {

    @Mock
    private com.microsoft.azure.servicebus.IMessageSender mockSender;

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
    public void testConnectionStringBuilderParsesEntityPath() {
        ConnectionStringBuilder builder = new ConnectionStringBuilder(
            "Endpoint=sb://test.servicebus.windows.net/;" +
            "SharedAccessKeyName=RootManageSharedAccessKey;" +
            "SharedAccessKey=dGVzdGtleQ==;" +
            "EntityPath=test-queue");
        assertEquals("test-queue", builder.getEntityPath());
    }

    @Test
    public void testMessageTransformerAsLambda() throws ServiceBusException {
        MessageTransformer upper = msg -> {
            String body = new String(msg.getBody(), StandardCharsets.UTF_8);
            Message transformed = new Message(body.toUpperCase().getBytes(StandardCharsets.UTF_8));
            transformed.setLabel(msg.getLabel());
            transformed.setMessageId(msg.getMessageId());
            return transformed;
        };

        Message input = new Message("hello".getBytes(StandardCharsets.UTF_8));
        input.setLabel("test");
        input.setMessageId("id-1");

        IMessage result = upper.transform(input);
        assertEquals("HELLO", new String(result.getBody(), StandardCharsets.UTF_8));
        assertEquals("test", result.getLabel());
    }

    @Test
    public void testChainedTransformers() throws ServiceBusException {
        MessageTransformer addLabel = msg -> {
            Message m = new Message(msg.getBody());
            m.setLabel("processed");
            return m;
        };

        MessageTransformer addProperty = msg -> {
            Message m = new Message(msg.getBody());
            m.setLabel(msg.getLabel());
            HashMap<String, Object> props = new HashMap<>();
            props.put("transformed", true);
            m.setProperties(props);
            return m;
        };

        Message input = new Message("data".getBytes(StandardCharsets.UTF_8));
        IMessage step1 = addLabel.transform(input);
        IMessage step2 = addProperty.transform(step1);

        assertEquals("processed", step2.getLabel());
        assertTrue((Boolean) step2.getProperties().get("transformed"));
    }

    @Test
    public void testConnectionStringBuilderEndpoint() {
        ConnectionStringBuilder builder = new ConnectionStringBuilder(
            "Endpoint=sb://contoso.servicebus.windows.net/;" +
            "SharedAccessKeyName=send-key;" +
            "SharedAccessKey=dGVzdGtleQ==;" +
            "EntityPath=orders");
        assertEquals("orders", builder.getEntityPath());
        assertNotNull(builder.getEndpoint());
    }
}
