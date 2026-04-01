package com.contoso.messaging;

import com.azure.messaging.servicebus.ServiceBusException;
import com.azure.messaging.servicebus.ServiceBusMessage;
import org.junit.Before;
import org.junit.Test;

import java.nio.charset.StandardCharsets;

import static org.junit.Assert.*;

public class MessageCacheTest {

    private MessageCache<ServiceBusMessage> cache;

    @Before
    public void setUp() {
        cache = new MessageCache<>(msg -> msg.getMessageId());
    }

    @Test
    public void testGetOrStoreAndRetrieve() {
        ServiceBusMessage msg = new ServiceBusMessage("payload".getBytes(StandardCharsets.UTF_8));
        msg.setMessageId("cache-1");
        msg.setSubject("orders");

        ServiceBusMessage stored = cache.getOrStore(msg);
        assertSame(msg, stored);
        assertEquals(1, cache.size());

        ServiceBusMessage retrieved = cache.get("cache-1");
        assertSame(msg, retrieved);
    }

    @Test
    public void testGetOrStoreReturnsCachedOnDuplicate() {
        ServiceBusMessage first = new ServiceBusMessage("first".getBytes(StandardCharsets.UTF_8));
        first.setMessageId("dup-id");
        first.setSubject("first-label");

        ServiceBusMessage second = new ServiceBusMessage("second".getBytes(StandardCharsets.UTF_8));
        second.setMessageId("dup-id");
        second.setSubject("second-label");

        cache.getOrStore(first);
        ServiceBusMessage result = cache.getOrStore(second);

        assertSame(first, result);
        assertEquals("first-label", result.getSubject());
    }

    @Test
    public void testTransformUpdatesCachedMessage() throws ServiceBusException {
        ServiceBusMessage msg = new ServiceBusMessage("original".getBytes(StandardCharsets.UTF_8));
        msg.setMessageId("transform-test");
        msg.setSubject("raw");
        cache.getOrStore(msg);

        MessageTransformer enricher = source -> {
            ServiceBusMessage enriched = new ServiceBusMessage(source.getBody());
            enriched.setMessageId(source.getMessageId());
            enriched.setSubject("enriched-" + source.getSubject());
            return enriched;
        };

        ServiceBusMessage transformed = cache.transform("transform-test", enricher);
        assertNotNull(transformed);
        assertEquals("enriched-raw", transformed.getSubject());

        ServiceBusMessage retrieved = cache.get("transform-test");
        assertEquals("enriched-raw", retrieved.getSubject());
    }

    @Test
    public void testTransformReturnsNullForMissingKey() throws ServiceBusException {
        MessageTransformer noop = source -> source;
        assertNull(cache.transform("nonexistent", noop));
    }

    @Test
    public void testGetBodyAsText() {
        ServiceBusMessage msg = new ServiceBusMessage("hello world".getBytes(StandardCharsets.UTF_8));
        String text = cache.getBodyAsText(msg);
        assertEquals("hello world", text);
    }

    @Test
    public void testGetLabelOrDefault() {
        ServiceBusMessage withLabel = new ServiceBusMessage("data".getBytes(StandardCharsets.UTF_8));
        withLabel.setSubject("orders");
        assertEquals("orders", cache.getLabelOrDefault(withLabel, "default"));

        ServiceBusMessage noLabel = new ServiceBusMessage("data".getBytes(StandardCharsets.UTF_8));
        assertEquals("default", cache.getLabelOrDefault(noLabel, "default"));
    }

    @Test
    public void testContains() {
        ServiceBusMessage msg = new ServiceBusMessage("data".getBytes(StandardCharsets.UTF_8));
        msg.setMessageId("check-id");
        cache.getOrStore(msg);

        assertTrue(cache.contains("check-id"));
        assertFalse(cache.contains("other-id"));
    }

    @Test
    public void testClear() {
        ServiceBusMessage msg = new ServiceBusMessage("data".getBytes(StandardCharsets.UTF_8));
        msg.setMessageId("clear-test");
        cache.getOrStore(msg);

        assertEquals(1, cache.size());
        cache.clear();
        assertEquals(0, cache.size());
    }

    @Test
    public void testCacheWithServiceBusMessageType() {
        MessageCache<ServiceBusMessage> msgCache = new MessageCache<>(ServiceBusMessage::getMessageId);
        ServiceBusMessage msg = new ServiceBusMessage("typed".getBytes(StandardCharsets.UTF_8));
        msg.setMessageId("interface-test");

        ServiceBusMessage stored = msgCache.getOrStore(msg);
        assertNotNull(stored);
        assertEquals("interface-test", stored.getMessageId());
    }

    @Test
    public void testCustomKeyExtractor() {
        MessageCache<ServiceBusMessage> labelCache = new MessageCache<>(msg -> msg.getSubject());
        ServiceBusMessage msg1 = new ServiceBusMessage("data1".getBytes(StandardCharsets.UTF_8));
        msg1.setSubject("unique-label");

        labelCache.getOrStore(msg1);
        assertTrue(labelCache.contains("unique-label"));
    }
}
