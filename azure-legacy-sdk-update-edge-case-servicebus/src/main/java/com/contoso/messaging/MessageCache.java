package com.contoso.messaging;

import com.azure.core.util.BinaryData;
import com.azure.messaging.servicebus.ServiceBusMessage;
import com.azure.messaging.servicebus.ServiceBusReceivedMessage;

import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

public class MessageCache<T> {

    private final Map<String, T> cache;
    private final Function<T, String> keyExtractor;

    public MessageCache(Function<T, String> keyExtractor) {
        this.cache = new ConcurrentHashMap<>();
        this.keyExtractor = keyExtractor;
    }

    public T getOrStore(T message) {
        String key = keyExtractor.apply(message);
        return cache.computeIfAbsent(key, k -> message);
    }

    public T get(String key) {
        return cache.get(key);
    }

    public boolean contains(String key) {
        return cache.containsKey(key);
    }

    public int size() {
        return cache.size();
    }

    public ServiceBusMessage transform(String key, MessageTransformer transformer) {
        T original = cache.get(key);
        if (original == null) {
            return null;
        }
        if (original instanceof ServiceBusMessage) {
            ServiceBusMessage transformed = transformer.transform((ServiceBusMessage) original);
            @SuppressWarnings("unchecked")
            T result = (T) transformed;
            cache.put(key, result);
            return transformed;
        } else if (original instanceof ServiceBusReceivedMessage) {
            // Convert received message to regular message for transformation
            ServiceBusReceivedMessage received = (ServiceBusReceivedMessage) original;
            ServiceBusMessage temp = convertToMessage(received);
            ServiceBusMessage transformed = transformer.transform(temp);
            // Can't store back as received message, so just return transformed
            return transformed;
        }
        return null;
    }

    private ServiceBusMessage convertToMessage(ServiceBusReceivedMessage received) {
        ServiceBusMessage message = new ServiceBusMessage(received.getBody());
        message.setMessageId(received.getMessageId());
        message.setSubject(received.getSubject());
        message.setContentType(received.getContentType());
        message.setCorrelationId(received.getCorrelationId());
        message.setSessionId(received.getSessionId());
        message.setReplyTo(received.getReplyTo());
        message.setTimeToLive(received.getTimeToLive());
        if (received.getApplicationProperties() != null) {
            message.getApplicationProperties().putAll(received.getApplicationProperties());
        }
        return message;
    }

    public String getBodyAsText(ServiceBusMessage message) {
        BinaryData body = message.getBody();
        return body != null ? body.toString() : null;
    }

    public String getBodyAsText(ServiceBusReceivedMessage message) {
        BinaryData body = message.getBody();
        return body != null ? body.toString() : null;
    }

    public String getSubjectOrDefault(ServiceBusMessage message, String defaultSubject) {
        String subject = message.getSubject();
        return subject != null ? subject : defaultSubject;
    }

    public String getSubjectOrDefault(ServiceBusReceivedMessage message, String defaultSubject) {
        String subject = message.getSubject();
        return subject != null ? subject : defaultSubject;
    }

    public void clear() {
        cache.clear();
    }
}
