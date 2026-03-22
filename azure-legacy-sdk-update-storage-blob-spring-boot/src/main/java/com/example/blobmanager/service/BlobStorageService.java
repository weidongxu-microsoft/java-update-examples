package com.example.blobmanager.service;

import com.azure.core.credential.AccessToken;
import com.azure.core.credential.TokenRequestContext;
import com.azure.identity.DefaultAzureCredentialBuilder;
import com.example.blobmanager.model.BlobInfo;
import com.microsoft.azure.storage.CloudStorageAccount;
import com.microsoft.azure.storage.StorageCredentialsToken;
import com.microsoft.azure.storage.StorageException;
import com.microsoft.azure.storage.blob.BlobProperties;
import com.microsoft.azure.storage.blob.CloudBlob;
import com.microsoft.azure.storage.blob.CloudBlobClient;
import com.microsoft.azure.storage.blob.CloudBlobContainer;
import com.microsoft.azure.storage.blob.CloudBlockBlob;
import com.microsoft.azure.storage.blob.ListBlobItem;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.security.InvalidKeyException;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;

@Service
public class BlobStorageService {

    private CloudBlobClient blobClient;

    @Value("${azure.storage.account-name:}")
    private String accountName;

    @Value("${azure.storage.blob-endpoint:}")
    private String blobEndpoint;

    @Value("${azure.storage.connection-string:}")
    private String connectionString;

    @PostConstruct
    public void init() throws URISyntaxException, InvalidKeyException {
        if (accountName != null && !accountName.isBlank()) {
            // Azure deployment: use Entra ID token via DefaultAzureCredential
            this.blobClient = createTokenAuthClient();
        } else if (connectionString != null && !connectionString.isBlank()) {
            // Local dev / Azurite: use connection string with shared key
            CloudStorageAccount storageAccount = CloudStorageAccount.parse(connectionString);
            this.blobClient = storageAccount.createCloudBlobClient();
        } else {
            throw new IllegalStateException(
                    "Configure either AZURE_STORAGE_CONNECTION_STRING (local) or AZURE_STORAGE_ACCOUNT_NAME (Azure)");
        }
    }

    private CloudBlobClient createTokenAuthClient() throws URISyntaxException {
        String endpoint = (blobEndpoint != null && !blobEndpoint.isBlank())
                ? blobEndpoint
                : "https://" + accountName + ".blob.core.windows.net";

        AccessToken accessToken = new DefaultAzureCredentialBuilder().build()
                .getTokenSync(new TokenRequestContext()
                        .addScopes("https://storage.azure.com/.default"));

        StorageCredentialsToken credentials = new StorageCredentialsToken(accountName, accessToken.getToken());
        return new CloudBlobClient(new URI(endpoint), credentials);
    }

    // Visible for testing
    void setBlobClient(CloudBlobClient blobClient) {
        this.blobClient = blobClient;
    }

    public List<String> listContainers() {
        List<String> containers = new ArrayList<>();
        for (CloudBlobContainer container : blobClient.listContainers()) {
            containers.add(container.getName());
        }
        return containers;
    }

    public void createContainer(String containerName) throws URISyntaxException, StorageException {
        CloudBlobContainer container = blobClient.getContainerReference(containerName);
        container.createIfNotExists();
    }

    public void deleteContainer(String containerName) throws URISyntaxException, StorageException {
        CloudBlobContainer container = blobClient.getContainerReference(containerName);
        container.deleteIfExists();
    }

    public List<BlobInfo> listBlobs(String containerName) throws URISyntaxException, StorageException {
        CloudBlobContainer container = blobClient.getContainerReference(containerName);
        if (!container.exists()) {
            throw new IllegalArgumentException("Container not found: " + containerName);
        }

        List<BlobInfo> blobs = new ArrayList<>();
        for (ListBlobItem item : container.listBlobs()) {
            if (item instanceof CloudBlob) {
                CloudBlob blob = (CloudBlob) item;
                blob.downloadAttributes();
                BlobProperties props = blob.getProperties();
                blobs.add(new BlobInfo(
                        blob.getName(),
                        blob.getUri().toString(),
                        props.getLength(),
                        props.getContentType(),
                        props.getLastModified() != null
                                ? OffsetDateTime.ofInstant(props.getLastModified().toInstant(), ZoneOffset.UTC)
                                : null
                ));
            }
        }
        return blobs;
    }

    public void uploadBlob(String containerName, String blobName, InputStream data, long length, String contentType)
            throws URISyntaxException, StorageException, IOException {
        CloudBlobContainer container = blobClient.getContainerReference(containerName);
        container.createIfNotExists();
        CloudBlockBlob blob = container.getBlockBlobReference(blobName);
        blob.getProperties().setContentType(contentType);
        blob.upload(data, length);
    }

    public void downloadBlob(String containerName, String blobName, OutputStream outputStream)
            throws URISyntaxException, StorageException {
        CloudBlobContainer container = blobClient.getContainerReference(containerName);
        CloudBlockBlob blob = container.getBlockBlobReference(blobName);
        if (!blob.exists()) {
            throw new IllegalArgumentException("Blob not found: " + blobName);
        }
        blob.download(outputStream);
    }

    public BlobInfo getBlobInfo(String containerName, String blobName)
            throws URISyntaxException, StorageException {
        CloudBlobContainer container = blobClient.getContainerReference(containerName);
        CloudBlockBlob blob = container.getBlockBlobReference(blobName);
        if (!blob.exists()) {
            throw new IllegalArgumentException("Blob not found: " + blobName);
        }
        blob.downloadAttributes();
        BlobProperties props = blob.getProperties();
        return new BlobInfo(
                blob.getName(),
                blob.getUri().toString(),
                props.getLength(),
                props.getContentType(),
                props.getLastModified() != null
                        ? OffsetDateTime.ofInstant(props.getLastModified().toInstant(), ZoneOffset.UTC)
                        : null
        );
    }

    public boolean deleteBlob(String containerName, String blobName)
            throws URISyntaxException, StorageException {
        CloudBlobContainer container = blobClient.getContainerReference(containerName);
        CloudBlockBlob blob = container.getBlockBlobReference(blobName);
        return blob.deleteIfExists();
    }
}
