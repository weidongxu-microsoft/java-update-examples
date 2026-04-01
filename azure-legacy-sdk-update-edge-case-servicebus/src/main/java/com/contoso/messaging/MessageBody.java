package com.contoso.messaging;

import java.util.List;

/**
 * Application-level class replacing the removed com.microsoft.azure.servicebus.MessageBody.
 * Provides factory methods and accessors for BINARY, VALUE, and SEQUENCE body types.
 */
public class MessageBody {

    private final MessageBodyType bodyType;
    private final List<byte[]> binaryData;
    private final Object valueData;
    private final List<List<Object>> sequenceData;

    private MessageBody(MessageBodyType bodyType, List<byte[]> binaryData,
                        Object valueData, List<List<Object>> sequenceData) {
        this.bodyType = bodyType;
        this.binaryData = binaryData;
        this.valueData = valueData;
        this.sequenceData = sequenceData;
    }

    public static MessageBody fromBinaryData(List<byte[]> data) {
        return new MessageBody(MessageBodyType.BINARY, data, null, null);
    }

    public static MessageBody fromValueData(Object value) {
        return new MessageBody(MessageBodyType.VALUE, null, value, null);
    }

    public static MessageBody fromSequenceData(List<List<Object>> sequences) {
        return new MessageBody(MessageBodyType.SEQUENCE, null, null, sequences);
    }

    public MessageBodyType getBodyType() {
        return bodyType;
    }

    public List<byte[]> getBinaryData() {
        return binaryData;
    }

    public Object getValueData() {
        return valueData;
    }

    public List<List<Object>> getSequenceData() {
        return sequenceData;
    }
}
