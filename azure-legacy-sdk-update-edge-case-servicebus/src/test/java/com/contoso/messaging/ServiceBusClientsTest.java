package com.contoso.messaging;

import org.junit.Test;

import static org.junit.Assert.*;

public class ServiceBusClientsTest {

    private static final String CONNECTION_STRING =
        "Endpoint=sb://contoso-orders.servicebus.windows.net/;" +
        "SharedAccessKeyName=RootManageSharedAccessKey;" +
        "SharedAccessKey=dGVzdGtleQ==;" +
        "EntityPath=orders-queue";

    @Test
    public void testExtractEntityPath() {
        String entityPath = ServiceBusClients.extractEntityPath(CONNECTION_STRING);
        assertEquals("orders-queue", entityPath);
    }

    @Test
    public void testExtractEndpoint() {
        String endpoint = ServiceBusClients.extractEndpoint(CONNECTION_STRING);
        assertNotNull(endpoint);
        assertTrue(endpoint.contains("contoso-orders"));
    }

    @Test
    public void testWithEntityPath() {
        String modified = ServiceBusClients.withEntityPath(CONNECTION_STRING, "new-queue");
        assertTrue(modified.contains("EntityPath=new-queue"));
        assertTrue(modified.contains("SharedAccessKeyName=RootManageSharedAccessKey"));
        assertTrue(modified.contains("SharedAccessKey=dGVzdGtleQ=="));
    }

    @Test
    public void testWithEntityPathAddsIfMissing() {
        String connStrWithoutEntity = "Endpoint=sb://test.servicebus.windows.net/;" +
            "SharedAccessKeyName=RootManageSharedAccessKey;" +
            "SharedAccessKey=dGVzdGtleQ==";
        
        String modified = ServiceBusClients.withEntityPath(connStrWithoutEntity, "added-queue");
        assertTrue(modified.contains("EntityPath=added-queue"));
    }

    @Test
    public void testWithEntityPathReplacesExisting() {
        String original = ServiceBusClients.withEntityPath(CONNECTION_STRING, "first-queue");
        String modified = ServiceBusClients.withEntityPath(original, "second-queue");
        
        assertTrue(modified.contains("EntityPath=second-queue"));
        assertFalse(modified.contains("EntityPath=first-queue"));
    }
}
