package com.contoso.messaging;

import com.microsoft.azure.servicebus.IMessage;
import com.microsoft.azure.servicebus.Message;
import com.microsoft.azure.servicebus.primitives.ServiceBusException;

import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

public class MessageCache<T extends IMessage> {

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

    public T transform(String key, MessageTransformer transformer) throws ServiceBusException {
        T original = cache.get(key);
        if (original == null) {
            return null;
        }
        IMessage transformed = transformer.transform(original);
        @SuppressWarnings("unchecked")
        T result = (T) transformed;
        cache.put(key, result);
        return result;
    }

    public String getBodyAsText(T message) {
        byte[] body = message.getBody();
        return body != null ? new String(body, StandardCharsets.UTF_8) : null;
    }

    public String getLabelOrDefault(T message, String defaultLabel) {
        String label = message.getLabel();
        return label != null ? label : defaultLabel;
    }

    public void clear() {
        cache.clear();
    }
}
