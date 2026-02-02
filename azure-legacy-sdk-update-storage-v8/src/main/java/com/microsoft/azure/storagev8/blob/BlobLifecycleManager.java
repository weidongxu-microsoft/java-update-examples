package com.microsoft.azure.storagev8.blob;

import com.microsoft.azure.storage.CloudStorageAccount;
import com.microsoft.azure.storage.StorageException;
import com.microsoft.azure.storage.blob.CloudBlobClient;
import com.microsoft.azure.storage.blob.CloudBlobContainer;
import com.microsoft.azure.storage.blob.CloudBlockBlob;
import com.microsoft.azure.storage.blob.ListBlobItem;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.security.InvalidKeyException;
import java.util.ArrayList;
import java.util.List;

/**
 * Wraps the v8 blob client so callers can focus on business steps rather than SDK plumbing.
 */
public class BlobLifecycleManager {

    private final CloudBlobClient blobClient;

    public BlobLifecycleManager(CloudBlobClient blobClient) {
        this.blobClient = blobClient;
    }

    /**
     * Mirrors the v8 initialization snippet from the migration guide: parse -> create client.
     */
    public static BlobLifecycleManager fromConnectionString(String connectionString)
            throws InvalidKeyException, URISyntaxException {
        CloudStorageAccount storageAccount = CloudStorageAccount.parse(connectionString);
        CloudBlobClient blobClient = storageAccount.createCloudBlobClient();
        return new BlobLifecycleManager(blobClient);
    }

    public CloudBlobContainer ensureContainer(String containerName)
            throws StorageException, URISyntaxException {
        CloudBlobContainer container = blobClient.getContainerReference(containerName);
        container.createIfNotExists();
        return container;
    }

    public CloudBlockBlob uploadText(String containerName, String blobName, String content)
            throws StorageException, URISyntaxException, IOException {
        CloudBlobContainer container = ensureContainer(containerName);
        CloudBlockBlob blob = container.getBlockBlobReference(blobName);
        blob.uploadText(content, StandardCharsets.UTF_8.name(), null, null, null);
        return blob;
    }

    public String downloadText(String containerName, String blobName)
            throws StorageException, URISyntaxException, IOException {
        CloudBlobContainer container = ensureContainer(containerName);
        CloudBlockBlob blob = container.getBlockBlobReference(blobName);
        return blob.downloadText(StandardCharsets.UTF_8.name(), null, null, null);
    }

    public List<String> listBlobUris(String containerName)
            throws StorageException, URISyntaxException {
        CloudBlobContainer container = ensureContainer(containerName);
        List<String> uris = new ArrayList<>();
        for (ListBlobItem item : container.listBlobs()) {
            uris.add(item.getUri().toString());
        }
        return uris;
    }

    public void downloadToFile(String containerName, String blobName, Path destination)
            throws StorageException, URISyntaxException, IOException {
        CloudBlobContainer container = ensureContainer(containerName);
        CloudBlockBlob blob = container.getBlockBlobReference(blobName);
        blob.downloadToFile(destination.toString());
    }

    public boolean deleteBlob(String containerName, String blobName)
            throws StorageException, URISyntaxException {
        CloudBlobContainer container = ensureContainer(containerName);
        CloudBlockBlob blob = container.getBlockBlobReference(blobName);
        return blob.deleteIfExists();
    }
}
