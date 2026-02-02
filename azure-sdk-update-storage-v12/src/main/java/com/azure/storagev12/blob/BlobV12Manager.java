package com.azure.storagev12.blob;

import com.azure.core.util.Context;
import com.azure.storage.blob.BlobClient;
import com.azure.storage.blob.BlobContainerClient;
import com.azure.storage.blob.BlobServiceClient;
import com.azure.storage.blob.BlobServiceClientBuilder;
import com.azure.storage.blob.models.BlobItem;
import com.azure.storage.blob.models.BlobStorageException;
import com.azure.storage.common.StorageSharedKeyCredential;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Encapsulates the V12 client stack so consumers can focus on business scenarios.
 */
public class BlobV12Manager {

    private final BlobServiceClient serviceClient;

    public BlobV12Manager(BlobServiceClient serviceClient) {
        this.serviceClient = Objects.requireNonNull(serviceClient, "serviceClient");
    }

    public static BlobV12Manager fromAccount(String accountName, String accountKey) {
        Objects.requireNonNull(accountName, "accountName");
        Objects.requireNonNull(accountKey, "accountKey");
        String endpoint = String.format("https://%s.blob.core.windows.net", accountName);
        StorageSharedKeyCredential credential = new StorageSharedKeyCredential(accountName, accountKey);
        BlobServiceClient serviceClient = new BlobServiceClientBuilder()
                .endpoint(endpoint)
                .credential(credential)
                .buildClient();
        return new BlobV12Manager(serviceClient);
    }

    public BlobContainerClient ensureContainer(String containerName) {
        BlobContainerClient containerClient = serviceClient.getBlobContainerClient(containerName);
        if (!containerClient.exists()) {
            containerClient.create();
        }
        return containerClient;
    }

    public BlobClient uploadText(String containerName, String blobName, String content) {
        BlobContainerClient containerClient = ensureContainer(containerName);
        BlobClient blobClient = containerClient.getBlobClient(blobName);
        byte[] bytes = content.getBytes(StandardCharsets.UTF_8);
        ByteArrayInputStream inputStream = new ByteArrayInputStream(bytes);
        blobClient.upload(inputStream, bytes.length, true);
        return blobClient;
    }

    public String downloadText(String containerName, String blobName) {
        BlobContainerClient containerClient = ensureContainer(containerName);
        BlobClient blobClient = containerClient.getBlobClient(blobName);
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        blobClient.downloadStream(outputStream);
        return new String(outputStream.toByteArray(), StandardCharsets.UTF_8);
    }

    public List<String> listBlobUris(String containerName) {
        BlobContainerClient containerClient = ensureContainer(containerName);
        List<String> uris = new ArrayList<>();
        for (BlobItem blobItem : containerClient.listBlobs()) {
            BlobClient blobClient = containerClient.getBlobClient(blobItem.getName());
            uris.add(blobClient.getBlobUrl());
        }
        return uris;
    }

    public void downloadToFile(String containerName, String blobName, Path destination) {
        BlobContainerClient containerClient = ensureContainer(containerName);
        BlobClient blobClient = containerClient.getBlobClient(blobName);
        Path parent = destination.getParent();
        if (parent != null) {
            try {
                Files.createDirectories(parent);
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        }
        blobClient.downloadToFile(destination.toString(), true);
    }

    public boolean deleteBlob(String containerName, String blobName) {
        BlobContainerClient containerClient = ensureContainer(containerName);
        BlobClient blobClient = containerClient.getBlobClient(blobName);
        try {
            blobClient.delete();
            return true;
        } catch (BlobStorageException ex) {
            if (ex.getStatusCode() == 404) {
                return false;
            }
            throw ex;
        }
    }
}
