package com.contoso.messaging;

import com.microsoft.azure.servicebus.IMessage;
import com.microsoft.azure.servicebus.Message;
import com.microsoft.azure.servicebus.MessageBody;
import com.microsoft.azure.servicebus.MessageBodyType;

import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MessageInspector {

    public MessageBodyType getBodyType(IMessage message) {
        MessageBody body = message.getMessageBody();
        if (body == null) {
            return null;
        }
        return body.getBodyType();
    }

    public String extractTextContent(IMessage message) {
        MessageBody body = message.getMessageBody();
        if (body == null) {
            return null;
        }

        switch (body.getBodyType()) {
            case BINARY:
                List<byte[]> binaryData = body.getBinaryData();
                if (binaryData != null && !binaryData.isEmpty()) {
                    return new String(binaryData.get(0), StandardCharsets.UTF_8);
                }
                return null;
            case VALUE:
                Object value = body.getValueData();
                return value != null ? value.toString() : null;
            case SEQUENCE:
                List<List<Object>> sequences = body.getSequenceData();
                if (sequences != null && !sequences.isEmpty()) {
                    StringBuilder sb = new StringBuilder();
                    for (List<Object> seq : sequences) {
                        for (Object item : seq) {
                            if (sb.length() > 0) {
                                sb.append(",");
                            }
                            sb.append(item);
                        }
                    }
                    return sb.toString();
                }
                return null;
            default:
                return null;
        }
    }

    public Map<String, Object> getInternalState(IMessage message) {
        Map<String, Object> state = new HashMap<>();
        state.put("messageId", message.getMessageId());
        state.put("label", message.getLabel());
        state.put("bodyType", getBodyType(message));
        state.put("contentType", message.getContentType());
        state.put("deliveryCount", message.getDeliveryCount());
        state.put("sequenceNumber", message.getSequenceNumber());

        try {
            Field bodyField = Message.class.getDeclaredField("messageBody");
            bodyField.setAccessible(true);
            MessageBody internalBody = (MessageBody) bodyField.get(message);
            if (internalBody != null) {
                state.put("internalBodyType", internalBody.getBodyType().name());
            }
        } catch (NoSuchFieldException | IllegalAccessException e) {
            state.put("reflectionError", e.getMessage());
        }

        return state;
    }

    public IMessage cloneWithBodyType(IMessage source, MessageBodyType targetType) {
        String content = extractTextContent(source);
        if (content == null) {
            content = "";
        }

        MessageBody newBody;
        switch (targetType) {
            case BINARY:
                newBody = MessageBody.fromBinaryData(
                    Collections.singletonList(content.getBytes(StandardCharsets.UTF_8)));
                break;
            case VALUE:
                newBody = MessageBody.fromValueData(content);
                break;
            case SEQUENCE:
                newBody = MessageBody.fromSequenceData(
                    Collections.singletonList(Collections.<Object>singletonList(content)));
                break;
            default:
                throw new IllegalArgumentException("Unsupported body type: " + targetType);
        }

        Message clone = new Message(newBody);
        clone.setMessageId(source.getMessageId());
        clone.setLabel(source.getLabel());
        clone.setContentType(source.getContentType());
        clone.setCorrelationId(source.getCorrelationId());
        if (source.getProperties() != null) {
            clone.setProperties(new HashMap<>(source.getProperties()));
        }
        return clone;
    }
}
