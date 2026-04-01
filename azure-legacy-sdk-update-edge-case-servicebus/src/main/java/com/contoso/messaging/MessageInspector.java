package com.contoso.messaging;

import com.azure.core.util.BinaryData;
import com.azure.messaging.servicebus.ServiceBusMessage;

import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MessageInspector {

    private static final String BODY_TYPE_PROPERTY = "x-body-type";

    public MessageBodyType getBodyType(ServiceBusMessage message) {
        MessageBody body = getMessageBody(message);
        if (body == null) {
            return null;
        }
        return body.getBodyType();
    }

    public String extractTextContent(ServiceBusMessage message) {
        MessageBody body = getMessageBody(message);
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

    public Map<String, Object> getInternalState(ServiceBusMessage message) {
        Map<String, Object> state = new HashMap<>();
        state.put("messageId", message.getMessageId());
        state.put("label", message.getSubject());
        state.put("bodyType", getBodyType(message));
        state.put("contentType", message.getContentType());

        MessageBody body = getMessageBody(message);
        if (body != null) {
            state.put("internalBodyType", body.getBodyType().name());
        }

        return state;
    }

    public ServiceBusMessage cloneWithBodyType(ServiceBusMessage source, MessageBodyType targetType) {
        String content = extractTextContent(source);
        if (content == null) {
            content = "";
        }

        ServiceBusMessage clone;
        switch (targetType) {
            case BINARY:
                clone = new ServiceBusMessage(content.getBytes(StandardCharsets.UTF_8));
                break;
            case VALUE:
                clone = new ServiceBusMessage(content.getBytes(StandardCharsets.UTF_8));
                break;
            case SEQUENCE:
                clone = new ServiceBusMessage(content.getBytes(StandardCharsets.UTF_8));
                break;
            default:
                throw new IllegalArgumentException("Unsupported body type: " + targetType);
        }

        clone.setMessageId(source.getMessageId());
        clone.setSubject(source.getSubject());
        clone.setContentType(source.getContentType());
        clone.setCorrelationId(source.getCorrelationId());
        if (source.getApplicationProperties() != null) {
            clone.getApplicationProperties().putAll(source.getApplicationProperties());
        }
        // Set the body type metadata
        clone.getApplicationProperties().put(BODY_TYPE_PROPERTY, targetType.name());

        return clone;
    }

    /**
     * Creates a ServiceBusMessage from a MessageBody, storing body type metadata
     * in application properties.
     */
    public static ServiceBusMessage createMessage(MessageBody body) {
        ServiceBusMessage message;
        switch (body.getBodyType()) {
            case BINARY:
                List<byte[]> binaryData = body.getBinaryData();
                byte[] data = (binaryData != null && !binaryData.isEmpty())
                    ? binaryData.get(0) : new byte[0];
                message = new ServiceBusMessage(data);
                break;
            case VALUE:
                Object valueData = body.getValueData();
                String valStr = valueData != null ? valueData.toString() : "";
                message = new ServiceBusMessage(valStr.getBytes(StandardCharsets.UTF_8));
                break;
            case SEQUENCE:
                List<List<Object>> seqData = body.getSequenceData();
                StringBuilder sb = new StringBuilder();
                if (seqData != null) {
                    for (List<Object> seq : seqData) {
                        for (Object item : seq) {
                            if (sb.length() > 0) sb.append(",");
                            sb.append(item);
                        }
                    }
                }
                message = new ServiceBusMessage(sb.toString().getBytes(StandardCharsets.UTF_8));
                break;
            default:
                throw new IllegalArgumentException("Unsupported body type: " + body.getBodyType());
        }
        message.getApplicationProperties().put(BODY_TYPE_PROPERTY, body.getBodyType().name());
        return message;
    }

    /**
     * Extracts a MessageBody from a ServiceBusMessage based on body type metadata.
     */
    public static MessageBody getMessageBody(ServiceBusMessage message) {
        if (message.getBody() == null) {
            return null;
        }
        String bodyTypeName = (String) message.getApplicationProperties().get(BODY_TYPE_PROPERTY);
        MessageBodyType bodyType = bodyTypeName != null
            ? MessageBodyType.valueOf(bodyTypeName) : MessageBodyType.BINARY;

        byte[] bodyBytes = message.getBody().toBytes();

        switch (bodyType) {
            case BINARY:
                return MessageBody.fromBinaryData(Collections.singletonList(bodyBytes));
            case VALUE:
                return MessageBody.fromValueData(new String(bodyBytes, StandardCharsets.UTF_8));
            case SEQUENCE:
                String seqStr = new String(bodyBytes, StandardCharsets.UTF_8);
                return MessageBody.fromSequenceData(
                    Collections.singletonList(Collections.<Object>singletonList(seqStr)));
            default:
                return MessageBody.fromBinaryData(Collections.singletonList(bodyBytes));
        }
    }
}
