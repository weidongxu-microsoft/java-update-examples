package com.contoso.messaging;

import com.azure.core.util.BinaryData;
import com.azure.messaging.servicebus.ServiceBusMessage;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.HashMap;

import static org.junit.Assert.*;

public class QueueSessionManagerTest {

    @Mock
    private com.azure.messaging.servicebus.ServiceBusSenderClient mockSender;

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
    public void testConnectionStringParsesEntityPath() {
        String connectionString = "Endpoint=sb://test.servicebus.windows.net/;" +
            "SharedAccessKeyName=RootManageSharedAccessKey;" +
            "SharedAccessKey=dGVzdGtleQ==;" +
            "EntityPath=test-queue";
        
        String entityPath = ServiceBusClients.extractEntityPath(connectionString);
        assertEquals("test-queue", entityPath);
    }

    @Test
    public void testMessageTransformerAsLambda() {
        MessageTransformer upper = msg -> {
            String body = msg.getBody().toString();
            ServiceBusMessage transformed = new ServiceBusMessage(BinaryData.fromString(body.toUpperCase()));
            transformed.setSubject(msg.getSubject());
            transformed.setMessageId(msg.getMessageId());
            return transformed;
        };

        ServiceBusMessage input = new ServiceBusMessage(BinaryData.fromString("hello"));
        input.setSubject("test");
        input.setMessageId("id-1");

        ServiceBusMessage result = upper.transform(input);
        assertEquals("HELLO", result.getBody().toString());
        assertEquals("test", result.getSubject());
    }

    @Test
    public void testChainedTransformers() {
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

        ServiceBusMessage input = new ServiceBusMessage(BinaryData.fromString("data"));
        ServiceBusMessage step1 = addLabel.transform(input);
        ServiceBusMessage step2 = addProperty.transform(step1);

        assertEquals("processed", step2.getSubject());
        assertTrue((Boolean) step2.getApplicationProperties().get("transformed"));
    }

    @Test
    public void testConnectionStringEndpoint() {
        String connectionString = "Endpoint=sb://contoso.servicebus.windows.net/;" +
            "SharedAccessKeyName=send-key;" +
            "SharedAccessKey=dGVzdGtleQ==;" +
            "EntityPath=orders";
        
        String entityPath = ServiceBusClients.extractEntityPath(connectionString);
        String endpoint = ServiceBusClients.extractEndpoint(connectionString);
        
        assertEquals("orders", entityPath);
        assertNotNull(endpoint);
    }
}
