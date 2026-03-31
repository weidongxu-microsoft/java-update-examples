package com.microsoft.azure.storage.advanced;

import com.microsoft.azure.storage.StorageException;
import com.microsoft.azure.storage.blob.CloudBlob;

import java.net.URI;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

public class BlobResourceCache<T extends CloudBlob> {

    private final Map<String, T> cache = new ConcurrentHashMap<>();
    private final Map<String, Long> accessTimestamps = new ConcurrentHashMap<>();

    public T getOrFetch(String key, Supplier<T> fetcher) {
        T blob = cache.computeIfAbsent(key, k -> fetcher.get());
        accessTimestamps.put(key, System.currentTimeMillis());
        return blob;
    }

    public URI getBlobUri(T blob) {
        return blob.getUri();
    }

    public String getBlobName(T blob) {
        return blob.getName();
    }

    public Map<String, String> getBlobMetadata(T blob) throws StorageException {
        blob.downloadAttributes();
        return blob.getMetadata();
    }

    public long getBlobSize(T blob) throws StorageException {
        blob.downloadAttributes();
        return blob.getProperties().getLength();
    }

    public void invalidate(String key) {
        cache.remove(key);
        accessTimestamps.remove(key);
    }

    public void invalidateAll() {
        cache.clear();
        accessTimestamps.clear();
    }

    public int size() {
        return cache.size();
    }

    public boolean contains(String key) {
        return cache.containsKey(key);
    }

    public T getCached(String key) {
        T blob = cache.get(key);
        if (blob != null) {
            accessTimestamps.put(key, System.currentTimeMillis());
        }
        return blob;
    }

    public void evictOlderThan(long maxAgeMillis) {
        long now = System.currentTimeMillis();
        accessTimestamps.entrySet().removeIf(entry -> {
            if (now - entry.getValue() > maxAgeMillis) {
                cache.remove(entry.getKey());
                return true;
            }
            return false;
        });
    }
}
