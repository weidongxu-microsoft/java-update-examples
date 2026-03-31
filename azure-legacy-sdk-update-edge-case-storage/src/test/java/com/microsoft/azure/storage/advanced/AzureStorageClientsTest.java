package com.microsoft.azure.storage.advanced;

import com.microsoft.azure.storage.CloudStorageAccount;
import com.microsoft.azure.storage.blob.CloudBlobClient;
import com.microsoft.azure.storage.blob.BlobRequestOptions;
import com.microsoft.azure.storage.queue.CloudQueueClient;
import com.microsoft.azure.storage.table.CloudTableClient;

import org.junit.Test;

import java.net.URISyntaxException;
import java.security.InvalidKeyException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class AzureStorageClientsTest {

    private static final String CONNECTION_STRING =
            "DefaultEndpointsProtocol=https;AccountName=devaccount;AccountKey=dGVzdGtleQ==;EndpointSuffix=core.windows.net";

    @Test
    public void testCreateBlobClient() throws URISyntaxException, InvalidKeyException {
        CloudBlobClient client = AzureStorageClients.createBlobClient(CONNECTION_STRING);
        assertNotNull(client);
        assertEquals(4, (int) client.getDefaultRequestOptions().getConcurrentRequestCount());
    }

    @Test
    public void testCreateQueueClient() throws URISyntaxException, InvalidKeyException {
        CloudQueueClient client = AzureStorageClients.createQueueClient(CONNECTION_STRING);
        assertNotNull(client);
    }

    @Test
    public void testCreateTableClient() throws URISyntaxException, InvalidKeyException {
        CloudTableClient client = AzureStorageClients.createTableClient(CONNECTION_STRING);
        assertNotNull(client);
    }

    @Test
    public void testParseAccount() throws URISyntaxException, InvalidKeyException {
        CloudStorageAccount account = AzureStorageClients.parseAccount(CONNECTION_STRING);
        assertNotNull(account);
        assertNotNull(account.getBlobEndpoint());
    }

    @Test(expected = InvalidKeyException.class)
    public void testInvalidConnectionString() throws URISyntaxException, InvalidKeyException {
        AzureStorageClients.createBlobClient("DefaultEndpointsProtocol=https;AccountName=x;AccountKey=bad");
    }

    @Test
    public void testBlobClientEndpoint() throws URISyntaxException, InvalidKeyException {
        CloudBlobClient client = AzureStorageClients.createBlobClient(CONNECTION_STRING);
        assertNotNull(client.getEndpoint());
        assertEquals("https", client.getEndpoint().getScheme());
    }

    @Test
    public void testReturnTypesAreLegacyClients() throws URISyntaxException, InvalidKeyException {
        CloudBlobClient blobClient = AzureStorageClients.createBlobClient(CONNECTION_STRING);
        CloudQueueClient queueClient = AzureStorageClients.createQueueClient(CONNECTION_STRING);
        CloudTableClient tableClient = AzureStorageClients.createTableClient(CONNECTION_STRING);

        assertNotNull(blobClient.getDefaultRequestOptions());
        assertNotNull(queueClient.getEndpoint());
        assertNotNull(tableClient.getEndpoint());
    }
}
