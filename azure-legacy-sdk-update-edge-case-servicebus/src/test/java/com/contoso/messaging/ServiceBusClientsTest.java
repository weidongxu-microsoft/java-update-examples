package com.contoso.messaging;

import com.microsoft.azure.servicebus.primitives.ConnectionStringBuilder;
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
        ConnectionStringBuilder builder = ServiceBusClients.parseConnection(CONNECTION_STRING);
        assertEquals("orders-queue", ServiceBusClients.extractEntityPath(builder));
    }

    @Test
    public void testParseConnectionExtractsEndpoint() {
        ConnectionStringBuilder builder = ServiceBusClients.parseConnection(CONNECTION_STRING);
        URI endpoint = ServiceBusClients.extractEndpoint(builder);
        assertNotNull(endpoint);
        assertTrue(endpoint.toString().contains("contoso-orders"));
    }

    @Test
    public void testParseConnectionExtractsSasKeyName() {
        ConnectionStringBuilder builder = ServiceBusClients.parseConnection(CONNECTION_STRING);
        assertEquals("RootManageSharedAccessKey", ServiceBusClients.extractSasKeyName(builder));
    }

    @Test
    public void testParseConnectionExtractsSasKey() {
        ConnectionStringBuilder builder = ServiceBusClients.parseConnection(CONNECTION_STRING);
        assertEquals("dGVzdGtleQ==", ServiceBusClients.extractSasKey(builder));
    }

    @Test
    public void testBuildConnection() {
        ConnectionStringBuilder builder = ServiceBusClients.buildConnection(
            "sb://test.servicebus.windows.net/",
            "my-queue",
            "send-key",
            "c2VjcmV0");
        assertEquals("my-queue", ServiceBusClients.extractEntityPath(builder));
        assertEquals("send-key", ServiceBusClients.extractSasKeyName(builder));
        assertEquals("c2VjcmV0", ServiceBusClients.extractSasKey(builder));
    }

    @Test
    public void testWithEntityPathCreatesNewBuilder() {
        ConnectionStringBuilder original = ServiceBusClients.parseConnection(CONNECTION_STRING);
        ConnectionStringBuilder forked = ServiceBusClients.withEntityPath(original, "new-queue");

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
    public void testConnectionStringBuilderRoundTrip() {
        ConnectionStringBuilder builder = ServiceBusClients.buildConnection(
            "sb://roundtrip.servicebus.windows.net/",
            "test-entity",
            "key-name",
            "a2V5dmFsdWU=");

        String connStr = builder.toString();
        ConnectionStringBuilder parsed = ServiceBusClients.parseConnection(connStr);

        assertEquals(
            ServiceBusClients.extractEntityPath(builder),
            ServiceBusClients.extractEntityPath(parsed));
    }
}
