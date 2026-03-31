package com.azure.datapipeline;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.microsoft.azure.keyvault.models.SecretBundle;
import org.junit.Before;
import org.junit.Test;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;

public class ResourceCacheTest {

    private ResourceCache<SecretBundle> cache;
    private ObjectMapper objectMapper;

    @Before
    public void setUp() {
        objectMapper = new ObjectMapper();
        cache = new ResourceCache<SecretBundle>(objectMapper, SecretBundle.class, 60_000L);
    }

    @Test
    public void putAndGetRoundTripsSecretBundle() throws Exception {
        SecretBundle bundle = new SecretBundle().withValue("my-database-connection-string");

        cache.put("db-connection", bundle);
        SecretBundle retrieved = cache.get("db-connection");

        assertThat(retrieved, is(notNullValue()));
        assertThat(retrieved.value(), is("my-database-connection-string"));
    }

    @Test
    public void getReturnsNullForMissingKey() throws Exception {
        assertThat(cache.get("nonexistent"), is(nullValue()));
    }

    @Test
    public void evictRemovesEntry() throws Exception {
        SecretBundle bundle = new SecretBundle().withValue("secret-value");
        cache.put("temp-secret", bundle);

        cache.evict("temp-secret");

        assertThat(cache.get("temp-secret"), is(nullValue()));
    }

    @Test
    public void clearRemovesAllEntries() throws Exception {
        cache.put("secret-1", new SecretBundle().withValue("value-1"));
        cache.put("secret-2", new SecretBundle().withValue("value-2"));

        cache.clear();

        assertThat(cache.size(), is(0));
    }

    @Test
    public void sizeReflectsNumberOfEntries() throws Exception {
        assertThat(cache.size(), is(0));

        cache.put("secret-1", new SecretBundle().withValue("v1"));
        assertThat(cache.size(), is(1));

        cache.put("secret-2", new SecretBundle().withValue("v2"));
        assertThat(cache.size(), is(2));
    }

    @Test
    public void getResourceTypeReturnsConfiguredClass() {
        assertThat(cache.getResourceType().getName(),
                is("com.microsoft.azure.keyvault.models.SecretBundle"));
    }

    @Test
    public void getRawJsonReturnsSerializedForm() throws Exception {
        SecretBundle bundle = new SecretBundle().withValue("raw-test");

        cache.put("raw-key", bundle);
        String json = cache.getRawJson("raw-key");

        assertThat(json, is(notNullValue()));
        assertThat(json, containsString("raw-test"));
    }

    @Test
    public void getRawJsonReturnsNullForMissingKey() {
        assertThat(cache.getRawJson("missing"), is(nullValue()));
    }

    @Test
    public void expiredEntryReturnsNull() throws Exception {
        // Create cache with 1ms TTL to test expiration
        ResourceCache<SecretBundle> shortLivedCache =
                new ResourceCache<SecretBundle>(objectMapper, SecretBundle.class, 1L);

        shortLivedCache.put("expiring", new SecretBundle().withValue("temp"));
        Thread.sleep(10);

        assertThat(shortLivedCache.get("expiring"), is(nullValue()));
    }

    @Test
    public void putOverwritesPreviousEntry() throws Exception {
        cache.put("key", new SecretBundle().withValue("original"));
        cache.put("key", new SecretBundle().withValue("updated"));

        SecretBundle retrieved = cache.get("key");

        assertThat(retrieved.value(), is("updated"));
        assertThat(cache.size(), is(1));
    }
}
