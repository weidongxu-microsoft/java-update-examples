package com.contoso.messaging;

import com.azure.core.util.BinaryData;
import com.azure.messaging.servicebus.ServiceBusMessage;
import org.junit.Before;
import org.junit.Test;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.HashMap;

import static org.junit.Assert.*;

public class MessageSerializerTest {

    private MessageSerializer serializer;

    @Before
    public void setUp() {
        serializer = new MessageSerializer();
    }

    @Test
    public void testSerializeAndDeserialize() throws Exception {
        ServiceBusMessage original = new ServiceBusMessage(BinaryData.fromString("test-body"));
        original.setMessageId("id-001");
        original.setSubject("orders");
        original.setContentType("application/json");
        original.setCorrelationId("corr-001");
        original.setTimeToLive(Duration.ofMinutes(30));

        original.getApplicationProperties().put("priority", 1);
        original.getApplicationProperties().put("source", "web");

        String json = serializer.serialize(original);
        ServiceBusMessage restored = serializer.deserialize(json);

        assertEquals("id-001", restored.getMessageId());
        assertEquals("orders", restored.getSubject());
        assertEquals("application/json", restored.getContentType());
        assertEquals("corr-001", restored.getCorrelationId());
        assertEquals("test-body", restored.getBody().toString());
        assertEquals(1, restored.getApplicationProperties().get("priority"));
        assertEquals("web", restored.getApplicationProperties().get("source"));
    }

    @Test
    public void testSerializeMinimalMessage() throws Exception {
        ServiceBusMessage minimal = new ServiceBusMessage(BinaryData.fromString("hello"));
        String json = serializer.serialize(minimal);
        assertNotNull(json);
        assertTrue(json.contains("hello"));

        ServiceBusMessage restored = serializer.deserialize(json);
        assertEquals("hello", restored.getBody().toString());
    }

    @Test
    public void testRoundTripPreservesProperties() throws Exception {
        ServiceBusMessage msg = new ServiceBusMessage(BinaryData.fromString("data"));
        msg.setSubject("test-label");

        msg.getApplicationProperties().put("intProp", 42);
        msg.getApplicationProperties().put("strProp", "value");
        msg.getApplicationProperties().put("boolProp", true);

        String json = serializer.serialize(msg);
        ServiceBusMessage restored = serializer.deserialize(json);

        assertEquals("test-label", restored.getSubject());
        assertEquals(42, restored.getApplicationProperties().get("intProp"));
        assertEquals("value", restored.getApplicationProperties().get("strProp"));
        assertEquals(true, restored.getApplicationProperties().get("boolProp"));
    }

    @Test
    public void testDeserializeWithTimeToLive() throws Exception {
        ServiceBusMessage msg = new ServiceBusMessage(BinaryData.fromString("ttl-test"));
        msg.setTimeToLive(Duration.ofHours(2));

        String json = serializer.serialize(msg);
        ServiceBusMessage restored = serializer.deserialize(json);

        assertEquals(Duration.ofHours(2).getSeconds(), restored.getTimeToLive().getSeconds());
    }

    @Test
    public void testSerializerModuleRegistered() {
        assertNotNull(serializer.getMapper());
    }
}
