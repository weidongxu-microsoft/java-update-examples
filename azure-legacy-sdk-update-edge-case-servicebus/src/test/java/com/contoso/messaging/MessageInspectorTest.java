package com.contoso.messaging;

import com.microsoft.azure.servicebus.IMessage;
import com.microsoft.azure.servicebus.Message;
import com.microsoft.azure.servicebus.MessageBody;
import com.microsoft.azure.servicebus.MessageBodyType;
import org.junit.Before;
import org.junit.Test;

import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.Map;

import static org.junit.Assert.*;

public class MessageInspectorTest {

    private MessageInspector inspector;

    @Before
    public void setUp() {
        inspector = new MessageInspector();
    }

    @Test
    public void testBinaryMessageBodyType() {
        Message msg = new Message("binary-data".getBytes(StandardCharsets.UTF_8));
        assertEquals(MessageBodyType.BINARY, inspector.getBodyType(msg));
    }

    @Test
    public void testValueMessageBodyType() {
        Message msg = new Message(MessageBody.fromValueData("value-data"));
        assertEquals(MessageBodyType.VALUE, inspector.getBodyType(msg));
    }

    @Test
    public void testSequenceMessageBodyType() {
        Message msg = new Message(MessageBody.fromSequenceData(
            Collections.singletonList(Collections.<Object>singletonList("seq-item"))));
        assertEquals(MessageBodyType.SEQUENCE, inspector.getBodyType(msg));
    }

    @Test
    public void testExtractTextFromBinary() {
        Message msg = new Message("hello-world".getBytes(StandardCharsets.UTF_8));
        assertEquals("hello-world", inspector.extractTextContent(msg));
    }

    @Test
    public void testExtractTextFromValue() {
        Message msg = new Message(MessageBody.fromValueData("value-content"));
        assertEquals("value-content", inspector.extractTextContent(msg));
    }

    @Test
    public void testExtractTextFromSequence() {
        Message msg = new Message(MessageBody.fromSequenceData(
            Collections.singletonList(Collections.<Object>singletonList("seq-item"))));
        String result = inspector.extractTextContent(msg);
        assertNotNull(result);
        assertTrue(result.contains("seq-item"));
    }

    @Test
    public void testGetInternalStateIncludesBodyType() {
        Message msg = new Message("test".getBytes(StandardCharsets.UTF_8));
        msg.setLabel("orders");
        msg.setMessageId("msg-42");

        Map<String, Object> state = inspector.getInternalState(msg);
        assertEquals("msg-42", state.get("messageId"));
        assertEquals("orders", state.get("label"));
        assertEquals(MessageBodyType.BINARY, state.get("bodyType"));
        assertEquals("BINARY", state.get("internalBodyType"));
    }

    @Test
    public void testGetInternalStateReflectionAccessesPrivateField() {
        Message msg = new Message(MessageBody.fromValueData("reflected"));

        Map<String, Object> state = inspector.getInternalState(msg);
        assertEquals("VALUE", state.get("internalBodyType"));
        assertFalse(state.containsKey("reflectionError"));
    }

    @Test
    public void testCloneWithBodyTypeBinaryToValue() {
        Message original = new Message("original-content".getBytes(StandardCharsets.UTF_8));
        original.setMessageId("clone-test");
        original.setLabel("test-label");

        IMessage cloned = inspector.cloneWithBodyType(original, MessageBodyType.VALUE);

        assertEquals("clone-test", cloned.getMessageId());
        assertEquals("test-label", cloned.getLabel());
        assertEquals(MessageBodyType.VALUE, cloned.getMessageBody().getBodyType());
        assertEquals("original-content", cloned.getMessageBody().getValueData().toString());
    }

    @Test
    public void testCloneWithBodyTypeValueToBinary() {
        Message original = new Message(MessageBody.fromValueData("value-content"));
        original.setMessageId("v2d-test");

        IMessage cloned = inspector.cloneWithBodyType(original, MessageBodyType.BINARY);

        assertEquals("v2d-test", cloned.getMessageId());
        assertEquals(MessageBodyType.BINARY, cloned.getMessageBody().getBodyType());
        byte[] data = cloned.getMessageBody().getBinaryData().get(0);
        assertEquals("value-content", new String(data, StandardCharsets.UTF_8));
    }

    @Test
    public void testClonePreservesProperties() {
        Message original = new Message("data".getBytes(StandardCharsets.UTF_8));
        original.setMessageId("props-test");
        original.setCorrelationId("corr-1");
        java.util.HashMap<String, Object> props = new java.util.HashMap<>();
        props.put("key", "value");
        original.setProperties(props);

        IMessage cloned = inspector.cloneWithBodyType(original, MessageBodyType.BINARY);

        assertEquals("props-test", cloned.getMessageId());
        assertEquals("corr-1", cloned.getCorrelationId());
        assertEquals("value", cloned.getProperties().get("key"));
    }
}
