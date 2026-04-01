package com.contoso.messaging;

import com.microsoft.azure.servicebus.IMessage;
import com.microsoft.azure.servicebus.Message;
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
        Message original = new Message("test-body".getBytes(StandardCharsets.UTF_8));
        original.setMessageId("id-001");
        original.setLabel("orders");
        original.setContentType("application/json");
        original.setCorrelationId("corr-001");
        original.setTimeToLive(Duration.ofMinutes(30));

        HashMap<String, Object> props = new HashMap<>();
        props.put("priority", 1);
        props.put("source", "web");
        original.setProperties(props);

        String json = serializer.serialize(original);
        IMessage restored = serializer.deserialize(json);

        assertEquals("id-001", restored.getMessageId());
        assertEquals("orders", restored.getLabel());
        assertEquals("application/json", restored.getContentType());
        assertEquals("corr-001", restored.getCorrelationId());
        assertEquals("test-body", new String(restored.getBody(), StandardCharsets.UTF_8));
        assertEquals(1, restored.getProperties().get("priority"));
        assertEquals("web", restored.getProperties().get("source"));
    }

    @Test
    public void testSerializeMinimalMessage() throws Exception {
        Message minimal = new Message("hello".getBytes(StandardCharsets.UTF_8));
        String json = serializer.serialize(minimal);
        assertNotNull(json);
        assertTrue(json.contains("hello"));

        IMessage restored = serializer.deserialize(json);
        assertEquals("hello", new String(restored.getBody(), StandardCharsets.UTF_8));
    }

    @Test
    public void testRoundTripPreservesProperties() throws Exception {
        Message msg = new Message("data".getBytes(StandardCharsets.UTF_8));
        msg.setLabel("test-label");

        HashMap<String, Object> props = new HashMap<>();
        props.put("intProp", 42);
        props.put("strProp", "value");
        props.put("boolProp", true);
        msg.setProperties(props);

        String json = serializer.serialize(msg);
        IMessage restored = serializer.deserialize(json);

        assertEquals("test-label", restored.getLabel());
        assertEquals(42, restored.getProperties().get("intProp"));
        assertEquals("value", restored.getProperties().get("strProp"));
        assertEquals(true, restored.getProperties().get("boolProp"));
    }

    @Test
    public void testDeserializeWithTimeToLive() throws Exception {
        Message msg = new Message("ttl-test".getBytes(StandardCharsets.UTF_8));
        msg.setTimeToLive(Duration.ofHours(2));

        String json = serializer.serialize(msg);
        IMessage restored = serializer.deserialize(json);

        assertEquals(Duration.ofHours(2).getSeconds(), restored.getTimeToLive().getSeconds());
    }

    @Test
    public void testSerializerModuleRegistered() {
        assertNotNull(serializer.getMapper());
    }
}
