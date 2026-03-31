package com.microsoft.azure.storage.advanced;

import com.microsoft.azure.storage.CloudStorageAccount;
import com.microsoft.azure.storage.blob.CloudBlobClient;
import com.microsoft.azure.storage.blob.CloudBlobContainer;
import com.microsoft.azure.storage.blob.CloudBlockBlob;

import java.net.URI;
import java.net.URISyntaxException;
import java.security.InvalidKeyException;
import java.time.Duration;
import java.util.Map;

public class StorageApplicationRunner {

    private static final String DEFAULT_CONNECTION_STRING =
            "DefaultEndpointsProtocol=https;AccountName=devaccount;AccountKey=dGVzdGtleQ==;EndpointSuffix=core.windows.net";

    public static void main(String[] args) throws Exception {
        String connectionString = System.getenv("AZURE_STORAGE_CONNECTION_STRING");
        if (connectionString == null || connectionString.isEmpty()) {
            connectionString = DEFAULT_CONNECTION_STRING;
        }

        String containerName = System.getenv("STORAGE_CONTAINER_NAME");
        if (containerName == null || containerName.isEmpty()) {
            containerName = "advanced-ops";
        }

        run(connectionString, containerName);
    }

    static void run(String connectionString, String containerName) throws Exception {
        CloudStorageAccount account = AzureStorageClients.parseAccount(connectionString);
        CloudBlobClient blobClient = AzureStorageClients.createBlobClient(connectionString);
        CloudBlobContainer container = blobClient.getContainerReference(containerName);

        BlobResourceCache<CloudBlockBlob> cache = new BlobResourceCache<>();
        StorageErrorRecovery errorRecovery = new StorageErrorRecovery();
        BlobTransferService transferService = new BlobTransferService(errorRecovery, cache);
        BlobTypeDispatcher dispatcher = new BlobTypeDispatcher(container);
        SasTokenGenerator sasGenerator = new SasTokenGenerator(account);

        System.out.println("=== Azure Storage Advanced Operations ===");
        System.out.println("Container: " + containerName);
        System.out.println("Account: " + account.getBlobEndpoint());

        String sasToken = sasGenerator.generateAccountSas(Duration.ofHours(1));
        System.out.println("Generated account SAS token (length=" + sasToken.length() + ")");

        String readOnlySas = sasGenerator.generateReadOnlyContainerSas(container, Duration.ofHours(2));
        System.out.println("Generated read-only container SAS token (length=" + readOnlySas.length() + ")");

        System.out.println("Cache size: " + cache.size());
        System.out.println("Error recovery map entries: " + StorageErrorRecovery.getErrorRecoveryMap().size());
        System.out.println("=== Complete ===");
    }
}
