package com.contoso.messaging;

import com.azure.core.util.BinaryData;
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
        ServiceBusMessage msg = new ServiceBusMessage(BinaryData.fromString("payload"));
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
        ServiceBusMessage first = new ServiceBusMessage(BinaryData.fromString("first"));
        first.setMessageId("dup-id");
        first.setSubject("first-label");

        ServiceBusMessage second = new ServiceBusMessage(BinaryData.fromString("second"));
        second.setMessageId("dup-id");
        second.setSubject("second-label");

        cache.getOrStore(first);
        ServiceBusMessage result = cache.getOrStore(second);

        assertSame(first, result);
        assertEquals("first-label", result.getSubject());
    }

    @Test
    public void testTransformUpdatesCachedMessage() {
        ServiceBusMessage msg = new ServiceBusMessage(BinaryData.fromString("original"));
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
    public void testTransformReturnsNullForMissingKey() {
        MessageTransformer noop = source -> source;
        assertNull(cache.transform("nonexistent", noop));
    }

    @Test
    public void testGetBodyAsText() {
        ServiceBusMessage msg = new ServiceBusMessage(BinaryData.fromString("hello world"));
        String text = cache.getBodyAsText(msg);
        assertEquals("hello world", text);
    }

    @Test
    public void testGetSubjectOrDefault() {
        ServiceBusMessage withSubject = new ServiceBusMessage(BinaryData.fromString("data"));
        withSubject.setSubject("orders");
        assertEquals("orders", cache.getSubjectOrDefault(withSubject, "default"));

        ServiceBusMessage noSubject = new ServiceBusMessage(BinaryData.fromString("data"));
        assertEquals("default", cache.getSubjectOrDefault(noSubject, "default"));
    }

    @Test
    public void testContains() {
        ServiceBusMessage msg = new ServiceBusMessage(BinaryData.fromString("data"));
        msg.setMessageId("check-id");
        cache.getOrStore(msg);

        assertTrue(cache.contains("check-id"));
        assertFalse(cache.contains("other-id"));
    }

    @Test
    public void testClear() {
        ServiceBusMessage msg = new ServiceBusMessage(BinaryData.fromString("data"));
        msg.setMessageId("clear-test");
        cache.getOrStore(msg);

        assertEquals(1, cache.size());
        cache.clear();
        assertEquals(0, cache.size());
    }

    @Test
    public void testCustomKeyExtractor() {
        MessageCache<ServiceBusMessage> labelCache = new MessageCache<>(msg -> msg.getSubject());
        ServiceBusMessage msg1 = new ServiceBusMessage(BinaryData.fromString("data1"));
        msg1.setSubject("unique-label");

        labelCache.getOrStore(msg1);
        assertTrue(labelCache.contains("unique-label"));
    }
}
