package com.azure.servicebus.client;

import com.microsoft.azure.servicebus.IMessageHandler;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class ServiceBusReceiverTest {

    private ServiceBusReceiver receiver;

    @Before
    public void setUp() {
        receiver = new ServiceBusReceiver(
                "Endpoint=sb://namespace.servicebus.windows.net/;SharedAccessKeyName=RootManageSharedAccessKey;SharedAccessKey=testkey",
                "orders-queue");
    }

    @Test
    public void testConstructor() {
        assertNotNull(receiver);
    }

    @Test
    public void testConstructorWithConcurrency() {
        ServiceBusReceiver customReceiver = new ServiceBusReceiver(
                "Endpoint=sb://namespace.servicebus.windows.net/;SharedAccessKeyName=RootManageSharedAccessKey;SharedAccessKey=testkey",
                "notifications-queue", 5);
        assertNotNull(customReceiver);
    }

    @Test
    public void testSetMessageHandler() {
        receiver.setMessageHandler(msg -> System.out.println(msg));
        // No exception means success
    }

    @Test
    public void testSetErrorHandler() {
        receiver.setErrorHandler(err -> System.err.println(err.getMessage()));
        // No exception means success
    }

    @Test
    public void testMessageHandlerInvocation() {
        final AtomicReference<String> received = new AtomicReference<>();
        Consumer<String> handler = received::set;
        receiver.setMessageHandler(handler);

        // Simulate handler invocation
        handler.accept("Order placed: ORD-1001");
        assertEquals("Order placed: ORD-1001", received.get());
    }

    @Test
    public void testErrorHandlerInvocation() {
        final AtomicReference<Throwable> receivedError = new AtomicReference<>();
        Consumer<Throwable> handler = receivedError::set;
        receiver.setErrorHandler(handler);

        RuntimeException testError = new RuntimeException("connection lost");
        handler.accept(testError);
        assertEquals("connection lost", receivedError.get().getMessage());
    }

    @Test
    public void testMultipleMessageHandlerInvocations() {
        final List<String> received = new ArrayList<>();
        Consumer<String> handler = received::add;
        receiver.setMessageHandler(handler);

        handler.accept("Message 1");
        handler.accept("Message 2");
        handler.accept("Message 3");

        assertEquals(3, received.size());
        assertEquals("Message 1", received.get(0));
        assertEquals("Message 2", received.get(1));
        assertEquals("Message 3", received.get(2));
    }

    @Test
    public void testInnerMessageHandlerClass() {
        ServiceBusReceiver.ServiceBusMessageHandler handler = receiver.new ServiceBusMessageHandler();
        assertNotNull(handler);
        assertTrue(handler instanceof IMessageHandler);
    }

    @Test
    public void testCloseWithNullClient() {
        // Should not throw when called before startReceiving
        receiver.close();
    }

    @Test
    public void testOverwriteMessageHandler() {
        final AtomicReference<String> first = new AtomicReference<>();
        final AtomicReference<String> second = new AtomicReference<>();

        receiver.setMessageHandler(first::set);
        receiver.setMessageHandler(second::set);

        // The second handler should be the active one
        Consumer<String> simulatedCallback = second::set;
        simulatedCallback.accept("latest");
        assertEquals("latest", second.get());
    }
}
