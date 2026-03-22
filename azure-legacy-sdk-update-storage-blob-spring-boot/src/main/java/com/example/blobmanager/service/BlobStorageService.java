package com.example.blobmanager.service;

import com.azure.identity.DefaultAzureCredentialBuilder;
import com.azure.storage.blob.BlobClient;
import com.azure.storage.blob.BlobContainerClient;
import com.azure.storage.blob.BlobServiceClient;
import com.azure.storage.blob.BlobServiceClientBuilder;
import com.azure.storage.blob.models.BlobHttpHeaders;
import com.azure.storage.blob.models.BlobItem;
import com.azure.storage.blob.models.BlobItemProperties;
import com.azure.storage.blob.models.BlobProperties;
import com.azure.storage.blob.specialized.BlockBlobClient;
import com.example.blobmanager.model.BlobInfo;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

@Service
public class BlobStorageService {

    private BlobServiceClient blobServiceClient;

    @Value("${azure.storage.account-name:}")
    private String accountName;

    @Value("${azure.storage.blob-endpoint:}")
    private String blobEndpoint;

    @Value("${azure.storage.connection-string:}")
    private String connectionString;

    @PostConstruct
    public void init() {
        if (accountName != null && !accountName.isBlank()) {
            // Azure deployment: use Entra ID token via DefaultAzureCredential
            String endpoint = (blobEndpoint != null && !blobEndpoint.isBlank())
                    ? blobEndpoint
                    : "https://" + accountName + ".blob.core.windows.net";
            this.blobServiceClient = new BlobServiceClientBuilder()
                    .endpoint(endpoint)
                    .credential(new DefaultAzureCredentialBuilder().build())
                    .buildClient();
        } else if (connectionString != null && !connectionString.isBlank()) {
            // Local dev / Azurite: use connection string with shared key
            this.blobServiceClient = new BlobServiceClientBuilder()
                    .connectionString(connectionString)
                    .buildClient();
        } else {
            throw new IllegalStateException(
                    "Configure either AZURE_STORAGE_CONNECTION_STRING (local) or AZURE_STORAGE_ACCOUNT_NAME (Azure)");
        }
    }

    // Visible for testing
    void setBlobServiceClient(BlobServiceClient blobServiceClient) {
        this.blobServiceClient = blobServiceClient;
    }

    public List<String> listContainers() {
        List<String> containers = new ArrayList<>();
        for (com.azure.storage.blob.models.BlobContainerItem item : blobServiceClient.listBlobContainers()) {
            containers.add(item.getName());
        }
        return containers;
    }

    public void createContainer(String containerName) {
        blobServiceClient.createBlobContainerIfNotExists(containerName);
    }

    public void deleteContainer(String containerName) {
        BlobContainerClient container = blobServiceClient.getBlobContainerClient(containerName);
        container.deleteIfExists();
    }

    public List<BlobInfo> listBlobs(String containerName) {
        BlobContainerClient container = blobServiceClient.getBlobContainerClient(containerName);
        if (!Boolean.TRUE.equals(container.exists())) {
            throw new IllegalArgumentException("Container not found: " + containerName);
        }

        List<BlobInfo> blobs = new ArrayList<>();
        for (BlobItem item : container.listBlobs()) {
            BlobItemProperties props = item.getProperties();
            String blobUrl = container.getBlobClient(item.getName()).getBlobUrl();
            blobs.add(new BlobInfo(
                    item.getName(),
                    blobUrl,
                    props.getContentLength() != null ? props.getContentLength() : 0L,
                    props.getContentType(),
                    props.getLastModified()
            ));
        }
        return blobs;
    }

    public void uploadBlob(String containerName, String blobName, InputStream data, long length, String contentType)
            throws IOException {
        BlobContainerClient container = blobServiceClient.getBlobContainerClient(containerName);
        container.createIfNotExists();
        BlockBlobClient blockBlob = container.getBlobClient(blobName).getBlockBlobClient();
        blockBlob.upload(data, length, true);
        blockBlob.setHttpHeaders(new BlobHttpHeaders().setContentType(contentType));
    }

    public void downloadBlob(String containerName, String blobName, OutputStream outputStream) {
        BlobContainerClient container = blobServiceClient.getBlobContainerClient(containerName);
        BlobClient blob = container.getBlobClient(blobName);
        if (!Boolean.TRUE.equals(blob.exists())) {
            throw new IllegalArgumentException("Blob not found: " + blobName);
        }
        blob.downloadStream(outputStream);
    }

    public BlobInfo getBlobInfo(String containerName, String blobName) {
        BlobContainerClient container = blobServiceClient.getBlobContainerClient(containerName);
        BlobClient blob = container.getBlobClient(blobName);
        if (!Boolean.TRUE.equals(blob.exists())) {
            throw new IllegalArgumentException("Blob not found: " + blobName);
        }
        BlobProperties props = blob.getProperties();
        return new BlobInfo(
                blobName,
                blob.getBlobUrl(),
                props.getBlobSize(),
                props.getContentType(),
                props.getLastModified()
        );
    }

    public boolean deleteBlob(String containerName, String blobName) {
        BlobContainerClient container = blobServiceClient.getBlobContainerClient(containerName);
        BlobClient blob = container.getBlobClient(blobName);
        return blob.deleteIfExists();
    }
}

