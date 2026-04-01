package com.contoso.messaging;

import com.azure.messaging.servicebus.ServiceBusMessage;
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
        ServiceBusMessage msg = new ServiceBusMessage("binary-data".getBytes(StandardCharsets.UTF_8));
        assertEquals(MessageBodyType.BINARY, inspector.getBodyType(msg));
    }

    @Test
    public void testValueMessageBodyType() {
        ServiceBusMessage msg = MessageInspector.createMessage(
            MessageBody.fromValueData("value-data"));
        assertEquals(MessageBodyType.VALUE, inspector.getBodyType(msg));
    }

    @Test
    public void testSequenceMessageBodyType() {
        ServiceBusMessage msg = MessageInspector.createMessage(
            MessageBody.fromSequenceData(
                Collections.singletonList(Collections.<Object>singletonList("seq-item"))));
        assertEquals(MessageBodyType.SEQUENCE, inspector.getBodyType(msg));
    }

    @Test
    public void testExtractTextFromBinary() {
        ServiceBusMessage msg = new ServiceBusMessage("hello-world".getBytes(StandardCharsets.UTF_8));
        assertEquals("hello-world", inspector.extractTextContent(msg));
    }

    @Test
    public void testExtractTextFromValue() {
        ServiceBusMessage msg = MessageInspector.createMessage(
            MessageBody.fromValueData("value-content"));
        assertEquals("value-content", inspector.extractTextContent(msg));
    }

    @Test
    public void testExtractTextFromSequence() {
        ServiceBusMessage msg = MessageInspector.createMessage(
            MessageBody.fromSequenceData(
                Collections.singletonList(Collections.<Object>singletonList("seq-item"))));
        String result = inspector.extractTextContent(msg);
        assertNotNull(result);
        assertTrue(result.contains("seq-item"));
    }

    @Test
    public void testGetInternalStateIncludesBodyType() {
        ServiceBusMessage msg = new ServiceBusMessage("test".getBytes(StandardCharsets.UTF_8));
        msg.setSubject("orders");
        msg.setMessageId("msg-42");

        Map<String, Object> state = inspector.getInternalState(msg);
        assertEquals("msg-42", state.get("messageId"));
        assertEquals("orders", state.get("label"));
        assertEquals(MessageBodyType.BINARY, state.get("bodyType"));
        assertEquals("BINARY", state.get("internalBodyType"));
    }

    @Test
    public void testGetInternalStateAccessesBodyType() {
        ServiceBusMessage msg = MessageInspector.createMessage(
            MessageBody.fromValueData("reflected"));

        Map<String, Object> state = inspector.getInternalState(msg);
        assertEquals("VALUE", state.get("internalBodyType"));
    }

    @Test
    public void testCloneWithBodyTypeBinaryToValue() {
        ServiceBusMessage original = new ServiceBusMessage("original-content".getBytes(StandardCharsets.UTF_8));
        original.setMessageId("clone-test");
        original.setSubject("test-label");

        ServiceBusMessage cloned = inspector.cloneWithBodyType(original, MessageBodyType.VALUE);

        assertEquals("clone-test", cloned.getMessageId());
        assertEquals("test-label", cloned.getSubject());
        assertEquals(MessageBodyType.VALUE, inspector.getBodyType(cloned));
        assertEquals("original-content", inspector.extractTextContent(cloned));
    }

    @Test
    public void testCloneWithBodyTypeValueToBinary() {
        ServiceBusMessage original = MessageInspector.createMessage(
            MessageBody.fromValueData("value-content"));
        original.setMessageId("v2d-test");

        ServiceBusMessage cloned = inspector.cloneWithBodyType(original, MessageBodyType.BINARY);

        assertEquals("v2d-test", cloned.getMessageId());
        assertEquals(MessageBodyType.BINARY, inspector.getBodyType(cloned));
        assertEquals("value-content", new String(cloned.getBody().toBytes(), StandardCharsets.UTF_8));
    }

    @Test
    public void testClonePreservesProperties() {
        ServiceBusMessage original = new ServiceBusMessage("data".getBytes(StandardCharsets.UTF_8));
        original.setMessageId("props-test");
        original.setCorrelationId("corr-1");
        original.getApplicationProperties().put("key", "value");

        ServiceBusMessage cloned = inspector.cloneWithBodyType(original, MessageBodyType.BINARY);

        assertEquals("props-test", cloned.getMessageId());
        assertEquals("corr-1", cloned.getCorrelationId());
        assertEquals("value", cloned.getApplicationProperties().get("key"));
    }
}
