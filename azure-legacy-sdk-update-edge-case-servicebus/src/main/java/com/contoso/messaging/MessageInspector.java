package com.contoso.messaging;

import com.azure.core.util.BinaryData;
import com.azure.messaging.servicebus.ServiceBusMessage;
import com.azure.messaging.servicebus.ServiceBusReceivedMessage;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

public class MessageInspector {

    public String extractTextContent(ServiceBusMessage message) {
        BinaryData body = message.getBody();
        return body != null ? body.toString() : null;
    }

    public String extractTextContent(ServiceBusReceivedMessage message) {
        BinaryData body = message.getBody();
        return body != null ? body.toString() : null;
    }

    public Map<String, Object> getInternalState(ServiceBusMessage message) {
        Map<String, Object> state = new HashMap<>();
        state.put("messageId", message.getMessageId());
        state.put("subject", message.getSubject());
        state.put("contentType", message.getContentType());
        return state;
    }

    public Map<String, Object> getInternalState(ServiceBusReceivedMessage message) {
        Map<String, Object> state = new HashMap<>();
        state.put("messageId", message.getMessageId());
        state.put("subject", message.getSubject());
        state.put("contentType", message.getContentType());
        state.put("deliveryCount", message.getDeliveryCount());
        state.put("sequenceNumber", message.getSequenceNumber());
        return state;
    }

    public ServiceBusMessage cloneMessage(ServiceBusMessage source) {
        String content = extractTextContent(source);
        if (content == null) {
            content = "";
        }

        ServiceBusMessage clone = new ServiceBusMessage(BinaryData.fromString(content));
        clone.setMessageId(source.getMessageId());
        clone.setSubject(source.getSubject());
        clone.setContentType(source.getContentType());
        clone.setCorrelationId(source.getCorrelationId());
        clone.setSessionId(source.getSessionId());
        clone.setReplyTo(source.getReplyTo());
        clone.setTimeToLive(source.getTimeToLive());
        
        if (source.getApplicationProperties() != null) {
            clone.getApplicationProperties().putAll(source.getApplicationProperties());
        }
        return clone;
    }

    public ServiceBusMessage cloneMessage(ServiceBusReceivedMessage source) {
        String content = extractTextContent(source);
        if (content == null) {
            content = "";
        }

        ServiceBusMessage clone = new ServiceBusMessage(BinaryData.fromString(content));
        clone.setMessageId(source.getMessageId());
        clone.setSubject(source.getSubject());
        clone.setContentType(source.getContentType());
        clone.setCorrelationId(source.getCorrelationId());
        clone.setSessionId(source.getSessionId());
        clone.setReplyTo(source.getReplyTo());
        clone.setTimeToLive(source.getTimeToLive());
        
        if (source.getApplicationProperties() != null) {
            clone.getApplicationProperties().putAll(source.getApplicationProperties());
        }
        return clone;
    }
}
