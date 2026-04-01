package com.contoso.messaging;

import com.azure.core.util.BinaryData;
import com.azure.messaging.servicebus.ServiceBusMessage;
import org.junit.Before;
import org.junit.Test;

import java.nio.charset.StandardCharsets;
import java.util.Map;

import static org.junit.Assert.*;

public class MessageInspectorTest {

    private MessageInspector inspector;

    @Before
    public void setUp() {
        inspector = new MessageInspector();
    }

    @Test
    public void testExtractTextFromMessage() {
        ServiceBusMessage msg = new ServiceBusMessage(BinaryData.fromString("hello-world"));
        assertEquals("hello-world", inspector.extractTextContent(msg));
    }

    @Test
    public void testGetInternalState() {
        ServiceBusMessage msg = new ServiceBusMessage(BinaryData.fromString("test"));
        msg.setSubject("orders");
        msg.setMessageId("msg-42");

        Map<String, Object> state = inspector.getInternalState(msg);
        assertEquals("msg-42", state.get("messageId"));
        assertEquals("orders", state.get("subject"));
    }

    @Test
    public void testCloneMessage() {
        ServiceBusMessage original = new ServiceBusMessage(BinaryData.fromString("original-content"));
        original.setMessageId("clone-test");
        original.setSubject("test-label");

        ServiceBusMessage cloned = inspector.cloneMessage(original);

        assertEquals("clone-test", cloned.getMessageId());
        assertEquals("test-label", cloned.getSubject());
        assertEquals("original-content", cloned.getBody().toString());
    }

    @Test
    public void testClonePreservesProperties() {
        ServiceBusMessage original = new ServiceBusMessage(BinaryData.fromString("data"));
        original.setMessageId("props-test");
        original.setCorrelationId("corr-1");
        original.getApplicationProperties().put("key", "value");

        ServiceBusMessage cloned = inspector.cloneMessage(original);

        assertEquals("props-test", cloned.getMessageId());
        assertEquals("corr-1", cloned.getCorrelationId());
        assertEquals("value", cloned.getApplicationProperties().get("key"));
    }
}
