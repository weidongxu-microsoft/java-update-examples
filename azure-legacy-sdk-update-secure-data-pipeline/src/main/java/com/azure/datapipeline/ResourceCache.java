package com.azure.datapipeline;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.concurrent.ConcurrentHashMap;

/**
 * Generic cache that serializes Azure SDK response objects to JSON for
 * in-memory caching with TTL expiration. Parameterized on the SDK resource type.
 *
 * <p>Migration challenge: The cache stores JSON representations of Track 1
 * SDK model types (e.g., {@code SecretBundle}, {@code KeyBundle}). These types
 * have specific Jackson-annotated field names. Track 2 equivalents have different
 * field names and structures, making cached data incompatible across SDK versions.
 * The {@code Class<T>} parameter also references the SDK type by class object,
 * not just by import.</p>
 *
 * @param <T> the Azure SDK resource type to cache
 */
public class ResourceCache<T> {

    private final ObjectMapper objectMapper;
    private final Class<T> resourceType;
    private final ConcurrentHashMap<String, String> jsonCache;
    private final ConcurrentHashMap<String, Long> timestampCache;
    private final long ttlMillis;

    /**
     * Creates a cache with the specified TTL.
     *
     * @param objectMapper the Jackson object mapper
     * @param resourceType the SDK resource class (e.g., {@code SecretBundle.class})
     * @param ttlMillis    time-to-live in milliseconds
     */
    public ResourceCache(ObjectMapper objectMapper, Class<T> resourceType, long ttlMillis) {
        this.objectMapper = objectMapper;
        this.resourceType = resourceType;
        this.jsonCache = new ConcurrentHashMap<String, String>();
        this.timestampCache = new ConcurrentHashMap<String, Long>();
        this.ttlMillis = ttlMillis;
        this.objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    }

    /**
     * Caches an Azure SDK resource by serializing it to JSON.
     *
     * @param key      the cache key
     * @param resource the SDK resource to cache
     */
    public void put(String key, T resource) throws JsonProcessingException {
        String json = objectMapper.writeValueAsString(resource);
        jsonCache.put(key, json);
        timestampCache.put(key, System.currentTimeMillis());
    }

    /**
     * Retrieves a cached resource, deserializing from JSON.
     * Returns null if the entry is missing or expired.
     *
     * @param key the cache key
     * @return the deserialized resource, or null
     */
    public T get(String key) throws JsonProcessingException {
        String json = jsonCache.get(key);
        if (json == null) {
            return null;
        }
        Long timestamp = timestampCache.get(key);
        if (timestamp != null && (System.currentTimeMillis() - timestamp) > ttlMillis) {
            evict(key);
            return null;
        }
        return objectMapper.readValue(json, resourceType);
    }

    /**
     * Removes an entry from the cache.
     *
     * @param key the cache key
     */
    public void evict(String key) {
        jsonCache.remove(key);
        timestampCache.remove(key);
    }

    /**
     * Clears all entries from the cache.
     */
    public void clear() {
        jsonCache.clear();
        timestampCache.clear();
    }

    /**
     * Returns the number of entries in the cache (including potentially expired ones).
     *
     * @return the cache size
     */
    public int size() {
        return jsonCache.size();
    }

    /**
     * Returns the SDK resource class this cache operates on.
     *
     * @return the resource class
     */
    public Class<T> getResourceType() {
        return resourceType;
    }

    /**
     * Returns the raw JSON for a cached entry (useful for debugging and audit).
     *
     * @param key the cache key
     * @return the JSON string, or null if not cached
     */
    public String getRawJson(String key) {
        return jsonCache.get(key);
    }
}
