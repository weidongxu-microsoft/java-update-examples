package com.microsoft.azure.storage.advanced;

import com.microsoft.azure.storage.CloudStorageAccount;
import com.microsoft.azure.storage.blob.CloudBlobClient;
import com.microsoft.azure.storage.queue.CloudQueueClient;
import com.microsoft.azure.storage.table.CloudTableClient;

import java.net.URISyntaxException;
import java.security.InvalidKeyException;

public final class AzureStorageClients {

    private AzureStorageClients() {
    }

    public static CloudBlobClient createBlobClient(String connectionString)
            throws URISyntaxException, InvalidKeyException {
        CloudStorageAccount account = CloudStorageAccount.parse(connectionString);
        CloudBlobClient client = account.createCloudBlobClient();
        client.getDefaultRequestOptions().setConcurrentRequestCount(4);
        return client;
    }

    public static CloudQueueClient createQueueClient(String connectionString)
            throws URISyntaxException, InvalidKeyException {
        CloudStorageAccount account = CloudStorageAccount.parse(connectionString);
        return account.createCloudQueueClient();
    }

    public static CloudTableClient createTableClient(String connectionString)
            throws URISyntaxException, InvalidKeyException {
        CloudStorageAccount account = CloudStorageAccount.parse(connectionString);
        return account.createCloudTableClient();
    }

    public static CloudStorageAccount parseAccount(String connectionString)
            throws URISyntaxException, InvalidKeyException {
        return CloudStorageAccount.parse(connectionString);
    }
}
