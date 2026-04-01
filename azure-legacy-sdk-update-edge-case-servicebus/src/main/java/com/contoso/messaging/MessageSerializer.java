package com.contoso.messaging;

import com.azure.messaging.servicebus.ServiceBusMessage;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.module.SimpleModule;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public class MessageSerializer {

    private final ObjectMapper mapper;

    public MessageSerializer() {
        this.mapper = new ObjectMapper();
        SimpleModule module = new SimpleModule("ServiceBusModule");
        module.addSerializer(ServiceBusMessage.class, new ServiceBusMessageJsonSerializer());
        module.addDeserializer(ServiceBusMessage.class, new ServiceBusMessageJsonDeserializer());
        mapper.registerModule(module);
    }

    public String serialize(ServiceBusMessage message) throws IOException {
        return mapper.writeValueAsString(message);
    }

    public ServiceBusMessage deserialize(String json) throws IOException {
        return mapper.readValue(json, ServiceBusMessage.class);
    }

    public ObjectMapper getMapper() {
        return mapper;
    }

    static class ServiceBusMessageJsonSerializer extends JsonSerializer<ServiceBusMessage> {
        @Override
        public void serialize(ServiceBusMessage value, JsonGenerator gen, SerializerProvider prov)
                throws IOException {
            gen.writeStartObject();
            gen.writeStringField("messageId", value.getMessageId());

            MessageBody msgBody = MessageInspector.getMessageBody(value);
            if (msgBody != null) {
                gen.writeStringField("bodyType", msgBody.getBodyType().name());
                switch (msgBody.getBodyType()) {
                    case BINARY:
                        java.util.List<byte[]> binaryData = msgBody.getBinaryData();
                        if (binaryData != null && !binaryData.isEmpty()) {
                            gen.writeStringField("body",
                                new String(binaryData.get(0), StandardCharsets.UTF_8));
                        } else {
                            gen.writeStringField("body", "");
                        }
                        break;
                    case VALUE:
                        Object valData = msgBody.getValueData();
                        gen.writeStringField("body", valData != null ? valData.toString() : "");
                        break;
                    default:
                        gen.writeStringField("body", "");
                        break;
                }
            } else {
                gen.writeStringField("body", "");
            }
            gen.writeStringField("label", value.getSubject());
            gen.writeStringField("contentType", value.getContentType());
            gen.writeStringField("correlationId", value.getCorrelationId());
            gen.writeStringField("sessionId", value.getSessionId());
            gen.writeStringField("replyTo", value.getReplyTo());

            if (value.getTimeToLive() != null) {
                gen.writeNumberField("timeToLiveSeconds", value.getTimeToLive().getSeconds());
            }

            Map<String, Object> properties = value.getApplicationProperties();
            if (properties != null && !properties.isEmpty()) {
                // Filter out internal body type property
                gen.writeObjectFieldStart("properties");
                for (Map.Entry<String, Object> entry : properties.entrySet()) {
                    if (!"x-body-type".equals(entry.getKey())) {
                        gen.writeObjectField(entry.getKey(), entry.getValue());
                    }
                }
                gen.writeEndObject();
            }
            gen.writeEndObject();
        }
    }

    static class ServiceBusMessageJsonDeserializer extends JsonDeserializer<ServiceBusMessage> {
        @Override
        public ServiceBusMessage deserialize(JsonParser p, DeserializationContext ctxt)
                throws IOException {
            JsonNode node = p.getCodec().readTree(p);

            String body = node.has("body") ? node.get("body").asText() : "";

            MessageBodyType bodyType = MessageBodyType.BINARY;
            if (node.has("bodyType") && !node.get("bodyType").isNull()) {
                bodyType = MessageBodyType.valueOf(node.get("bodyType").asText());
            }

            ServiceBusMessage message;
            switch (bodyType) {
                case VALUE:
                    MessageBody valueBody = MessageBody.fromValueData(body);
                    message = MessageInspector.createMessage(valueBody);
                    break;
                case SEQUENCE:
                    MessageBody seqBody = MessageBody.fromSequenceData(
                        java.util.Collections.singletonList(
                            java.util.Collections.<Object>singletonList(body)));
                    message = MessageInspector.createMessage(seqBody);
                    break;
                default:
                    message = new ServiceBusMessage(body.getBytes(StandardCharsets.UTF_8));
                    break;
            }

            if (node.has("messageId") && !node.get("messageId").isNull()) {
                message.setMessageId(node.get("messageId").asText());
            }
            if (node.has("label") && !node.get("label").isNull()) {
                message.setSubject(node.get("label").asText());
            }
            if (node.has("contentType") && !node.get("contentType").isNull()) {
                message.setContentType(node.get("contentType").asText());
            }
            if (node.has("correlationId") && !node.get("correlationId").isNull()) {
                message.setCorrelationId(node.get("correlationId").asText());
            }
            if (node.has("sessionId") && !node.get("sessionId").isNull()) {
                message.setSessionId(node.get("sessionId").asText());
            }
            if (node.has("replyTo") && !node.get("replyTo").isNull()) {
                message.setReplyTo(node.get("replyTo").asText());
            }
            if (node.has("timeToLiveSeconds")) {
                message.setTimeToLive(Duration.ofSeconds(node.get("timeToLiveSeconds").asLong()));
            }
            if (node.has("properties")) {
                JsonNode propsNode = node.get("properties");
                Iterator<Map.Entry<String, JsonNode>> fields = propsNode.fields();
                while (fields.hasNext()) {
                    Map.Entry<String, JsonNode> entry = fields.next();
                    JsonNode val = entry.getValue();
                    if (val.isTextual()) {
                        message.getApplicationProperties().put(entry.getKey(), val.asText());
                    } else if (val.isInt()) {
                        message.getApplicationProperties().put(entry.getKey(), val.asInt());
                    } else if (val.isLong()) {
                        message.getApplicationProperties().put(entry.getKey(), val.asLong());
                    } else if (val.isDouble()) {
                        message.getApplicationProperties().put(entry.getKey(), val.asDouble());
                    } else if (val.isBoolean()) {
                        message.getApplicationProperties().put(entry.getKey(), val.asBoolean());
                    }
                }
            }

            return message;
        }
    }
}
