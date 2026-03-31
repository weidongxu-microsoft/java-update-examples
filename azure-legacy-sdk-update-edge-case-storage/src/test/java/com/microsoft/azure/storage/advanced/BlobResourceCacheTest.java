package com.microsoft.azure.storage.advanced;

import com.microsoft.azure.storage.StorageException;
import com.microsoft.azure.storage.blob.BlobProperties;
import com.microsoft.azure.storage.blob.CloudBlockBlob;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class BlobResourceCacheTest {

    @Mock private CloudBlockBlob mockBlob1;
    @Mock private CloudBlockBlob mockBlob2;
    @Mock private BlobProperties mockProperties;

    private BlobResourceCache<CloudBlockBlob> cache;

    @Before
    public void setUp() {
        cache = new BlobResourceCache<>();
    }

    @Test
    public void testGetOrFetch() {
        Supplier<CloudBlockBlob> fetcher = () -> mockBlob1;
        CloudBlockBlob result = cache.getOrFetch("key1", fetcher);
        assertSame(mockBlob1, result);
        assertEquals(1, cache.size());
        assertTrue(cache.contains("key1"));
    }

    @Test
    public void testGetOrFetchReturnsCachedOnSecondCall() {
        cache.getOrFetch("key1", () -> mockBlob1);
        CloudBlockBlob result = cache.getOrFetch("key1", () -> mockBlob2);
        assertSame(mockBlob1, result);
        assertEquals(1, cache.size());
    }

    @Test
    public void testGetBlobUri() throws URISyntaxException {
        URI expectedUri = new URI("https://devaccount.blob.core.windows.net/container/blob1");
        when(mockBlob1.getUri()).thenReturn(expectedUri);

        URI actualUri = cache.getBlobUri(mockBlob1);
        assertEquals(expectedUri, actualUri);
        verify(mockBlob1).getUri();
    }

    @Test
    public void testGetBlobName() {
        when(mockBlob1.getName()).thenReturn("testblob.txt");
        assertEquals("testblob.txt", cache.getBlobName(mockBlob1));
    }

    @Test
    public void testGetBlobMetadata() throws StorageException {
        Map<String, String> metadata = new HashMap<>();
        metadata.put("author", "test");
        doNothing().when(mockBlob1).downloadAttributes();
        when(mockBlob1.getMetadata()).thenReturn((HashMap<String, String>) metadata);

        Map<String, String> result = cache.getBlobMetadata(mockBlob1);
        assertEquals("test", result.get("author"));
        verify(mockBlob1).downloadAttributes();
    }

    @Test
    public void testGetBlobSize() throws StorageException {
        doNothing().when(mockBlob1).downloadAttributes();
        when(mockBlob1.getProperties()).thenReturn(mockProperties);
        when(mockProperties.getLength()).thenReturn(1024L);

        long size = cache.getBlobSize(mockBlob1);
        assertEquals(1024L, size);
    }

    @Test
    public void testInvalidate() {
        cache.getOrFetch("key1", () -> mockBlob1);
        cache.getOrFetch("key2", () -> mockBlob2);
        assertEquals(2, cache.size());

        cache.invalidate("key1");
        assertEquals(1, cache.size());
        assertFalse(cache.contains("key1"));
        assertTrue(cache.contains("key2"));
    }

    @Test
    public void testInvalidateAll() {
        cache.getOrFetch("key1", () -> mockBlob1);
        cache.getOrFetch("key2", () -> mockBlob2);
        cache.invalidateAll();
        assertEquals(0, cache.size());
    }

    @Test
    public void testGetCached() {
        assertNull(cache.getCached("key1"));
        cache.getOrFetch("key1", () -> mockBlob1);
        CloudBlockBlob result = cache.getCached("key1");
        assertSame(mockBlob1, result);
    }

    @Test
    public void testEvictOlderThan() throws InterruptedException {
        cache.getOrFetch("old", () -> mockBlob1);
        Thread.sleep(50);
        cache.getOrFetch("new", () -> mockBlob2);
        cache.evictOlderThan(25);
        assertFalse(cache.contains("old"));
        assertTrue(cache.contains("new"));
    }

    @Test
    public void testGenericBoundEnforced() throws URISyntaxException {
        URI uri = new URI("https://devaccount.blob.core.windows.net/c/b");
        when(mockBlob1.getUri()).thenReturn(uri);
        when(mockBlob1.getName()).thenReturn("b");

        cache.getOrFetch("b", () -> mockBlob1);
        CloudBlockBlob fetched = cache.getCached("b");
        assertEquals(uri, cache.getBlobUri(fetched));
        assertEquals("b", cache.getBlobName(fetched));
    }
}
