package com.contoso.messaging;

import com.microsoft.azure.servicebus.IMessage;
import com.microsoft.azure.servicebus.Message;
import com.microsoft.azure.servicebus.primitives.ServiceBusException;
import org.junit.Before;
import org.junit.Test;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;

import static org.junit.Assert.*;

public class MessageCacheTest {

    private MessageCache<Message> cache;

    @Before
    public void setUp() {
        cache = new MessageCache<>(msg -> msg.getMessageId());
    }

    @Test
    public void testGetOrStoreAndRetrieve() {
        Message msg = new Message("payload".getBytes(StandardCharsets.UTF_8));
        msg.setMessageId("cache-1");
        msg.setLabel("orders");

        Message stored = cache.getOrStore(msg);
        assertSame(msg, stored);
        assertEquals(1, cache.size());

        Message retrieved = cache.get("cache-1");
        assertSame(msg, retrieved);
    }

    @Test
    public void testGetOrStoreReturnsCachedOnDuplicate() {
        Message first = new Message("first".getBytes(StandardCharsets.UTF_8));
        first.setMessageId("dup-id");
        first.setLabel("first-label");

        Message second = new Message("second".getBytes(StandardCharsets.UTF_8));
        second.setMessageId("dup-id");
        second.setLabel("second-label");

        cache.getOrStore(first);
        Message result = cache.getOrStore(second);

        assertSame(first, result);
        assertEquals("first-label", result.getLabel());
    }

    @Test
    public void testTransformUpdatesCachedMessage() throws ServiceBusException {
        Message msg = new Message("original".getBytes(StandardCharsets.UTF_8));
        msg.setMessageId("transform-test");
        msg.setLabel("raw");
        cache.getOrStore(msg);

        MessageTransformer enricher = source -> {
            Message enriched = new Message(source.getBody());
            enriched.setMessageId(source.getMessageId());
            enriched.setLabel("enriched-" + source.getLabel());
            return enriched;
        };

        Message transformed = cache.transform("transform-test", enricher);
        assertNotNull(transformed);
        assertEquals("enriched-raw", transformed.getLabel());

        Message retrieved = cache.get("transform-test");
        assertEquals("enriched-raw", retrieved.getLabel());
    }

    @Test
    public void testTransformReturnsNullForMissingKey() throws ServiceBusException {
        MessageTransformer noop = source -> source;
        assertNull(cache.transform("nonexistent", noop));
    }

    @Test
    public void testGetBodyAsText() {
        Message msg = new Message("hello world".getBytes(StandardCharsets.UTF_8));
        String text = cache.getBodyAsText(msg);
        assertEquals("hello world", text);
    }

    @Test
    public void testGetLabelOrDefault() {
        Message withLabel = new Message("data".getBytes(StandardCharsets.UTF_8));
        withLabel.setLabel("orders");
        assertEquals("orders", cache.getLabelOrDefault(withLabel, "default"));

        Message noLabel = new Message("data".getBytes(StandardCharsets.UTF_8));
        assertEquals("default", cache.getLabelOrDefault(noLabel, "default"));
    }

    @Test
    public void testContains() {
        Message msg = new Message("data".getBytes(StandardCharsets.UTF_8));
        msg.setMessageId("check-id");
        cache.getOrStore(msg);

        assertTrue(cache.contains("check-id"));
        assertFalse(cache.contains("other-id"));
    }

    @Test
    public void testClear() {
        Message msg = new Message("data".getBytes(StandardCharsets.UTF_8));
        msg.setMessageId("clear-test");
        cache.getOrStore(msg);

        assertEquals(1, cache.size());
        cache.clear();
        assertEquals(0, cache.size());
    }

    @Test
    public void testCacheWithIMessageInterface() {
        MessageCache<IMessage> interfaceCache = new MessageCache<>(IMessage::getMessageId);
        Message msg = new Message("typed".getBytes(StandardCharsets.UTF_8));
        msg.setMessageId("interface-test");

        IMessage stored = interfaceCache.getOrStore(msg);
        assertNotNull(stored);
        assertEquals("interface-test", stored.getMessageId());
    }

    @Test
    public void testCustomKeyExtractor() {
        MessageCache<Message> labelCache = new MessageCache<>(msg -> msg.getLabel());
        Message msg1 = new Message("data1".getBytes(StandardCharsets.UTF_8));
        msg1.setLabel("unique-label");

        labelCache.getOrStore(msg1);
        assertTrue(labelCache.contains("unique-label"));
    }
}
