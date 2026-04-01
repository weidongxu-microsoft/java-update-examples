package com.contoso.messaging;

import org.junit.Test;

import java.net.URI;

import static org.junit.Assert.*;

public class ServiceBusClientsTest {

    private static final String CONNECTION_STRING =
        "Endpoint=sb://contoso-orders.servicebus.windows.net/;" +
        "SharedAccessKeyName=RootManageSharedAccessKey;" +
        "SharedAccessKey=dGVzdGtleQ==;" +
        "EntityPath=orders-queue";

    @Test
    public void testParseConnectionExtractsEntityPath() {
        ConnectionStringProperties props = ServiceBusClients.parseConnection(CONNECTION_STRING);
        assertEquals("orders-queue", ServiceBusClients.extractEntityPath(props));
    }

    @Test
    public void testParseConnectionExtractsEndpoint() {
        ConnectionStringProperties props = ServiceBusClients.parseConnection(CONNECTION_STRING);
        URI endpoint = ServiceBusClients.extractEndpoint(props);
        assertNotNull(endpoint);
        assertTrue(endpoint.toString().contains("contoso-orders"));
    }

    @Test
    public void testParseConnectionExtractsSasKeyName() {
        ConnectionStringProperties props = ServiceBusClients.parseConnection(CONNECTION_STRING);
        assertEquals("RootManageSharedAccessKey", ServiceBusClients.extractSasKeyName(props));
    }

    @Test
    public void testParseConnectionExtractsSasKey() {
        ConnectionStringProperties props = ServiceBusClients.parseConnection(CONNECTION_STRING);
        assertEquals("dGVzdGtleQ==", ServiceBusClients.extractSasKey(props));
    }

    @Test
    public void testBuildConnection() {
        ConnectionStringProperties props = ServiceBusClients.buildConnection(
            "sb://test.servicebus.windows.net/",
            "my-queue",
            "send-key",
            "c2VjcmV0");
        assertEquals("my-queue", ServiceBusClients.extractEntityPath(props));
        assertEquals("send-key", ServiceBusClients.extractSasKeyName(props));
        assertEquals("c2VjcmV0", ServiceBusClients.extractSasKey(props));
    }

    @Test
    public void testWithEntityPathCreatesNewProperties() {
        ConnectionStringProperties original = ServiceBusClients.parseConnection(CONNECTION_STRING);
        ConnectionStringProperties forked = ServiceBusClients.withEntityPath(original, "new-queue");

        assertEquals("orders-queue", ServiceBusClients.extractEntityPath(original));
        assertEquals("new-queue", ServiceBusClients.extractEntityPath(forked));
        assertEquals(
            ServiceBusClients.extractSasKeyName(original),
            ServiceBusClients.extractSasKeyName(forked));
        assertEquals(
            ServiceBusClients.extractSasKey(original),
            ServiceBusClients.extractSasKey(forked));
    }

    @Test
    public void testConnectionStringPropertiesRoundTrip() {
        ConnectionStringProperties props = ServiceBusClients.buildConnection(
            "sb://roundtrip.servicebus.windows.net/",
            "test-entity",
            "key-name",
            "a2V5dmFsdWU=");

        String connStr = props.toConnectionString();
        ConnectionStringProperties parsed = ServiceBusClients.parseConnection(connStr);

        assertEquals(
            ServiceBusClients.extractEntityPath(props),
            ServiceBusClients.extractEntityPath(parsed));
    }
}
