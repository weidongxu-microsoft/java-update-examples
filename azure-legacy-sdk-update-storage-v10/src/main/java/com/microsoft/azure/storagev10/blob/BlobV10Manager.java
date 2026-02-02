package com.microsoft.azure.storagev10.blob;

import com.microsoft.azure.storage.blob.BlockBlobURL;
import com.microsoft.azure.storage.blob.ContainerURL;
import com.microsoft.azure.storage.blob.DownloadResponse;
import com.microsoft.azure.storage.blob.PipelineOptions;
import com.microsoft.azure.storage.blob.ReliableDownloadOptions;
import com.microsoft.azure.storage.blob.ServiceURL;
import com.microsoft.azure.storage.blob.SharedKeyCredentials;
import com.microsoft.azure.storage.blob.StorageException;
import com.microsoft.azure.storage.blob.StorageURL;
import com.microsoft.azure.storage.blob.models.BlobItem;
import com.microsoft.azure.storage.blob.models.ContainerListBlobFlatSegmentResponse;
import io.reactivex.Flowable;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.UncheckedIOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.InvalidKeyException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Encapsulates the V10 client stack so consumers can focus on business scenarios.
 */
public class BlobV10Manager {

    private final ServiceURL serviceURL;

    public BlobV10Manager(ServiceURL serviceURL) {
        this.serviceURL = Objects.requireNonNull(serviceURL, "serviceURL");
    }

    public static BlobV10Manager fromAccount(String accountName, String accountKey)
            throws MalformedURLException, InvalidKeyException {
        Objects.requireNonNull(accountName, "accountName");
        Objects.requireNonNull(accountKey, "accountKey");
        SharedKeyCredentials credentials = new SharedKeyCredentials(accountName, accountKey);
        URL endpoint = new URL(String.format("https://%s.blob.core.windows.net", accountName));
        ServiceURL serviceURL = new ServiceURL(endpoint, StorageURL.createPipeline(credentials, new PipelineOptions()));
        return new BlobV10Manager(serviceURL);
    }

    public ContainerURL ensureContainer(String containerName) throws StorageException {
        ContainerURL containerURL = serviceURL.createContainerURL(containerName);
        try {
            containerURL.create().blockingGet();
        } catch (StorageException ex) {
            if (ex.statusCode() != 409) {
                throw ex;
            }
        }
        return containerURL;
    }

    public BlockBlobURL uploadText(String containerName, String blobName, String content) throws StorageException {
        ContainerURL container = ensureContainer(containerName);
        BlockBlobURL blob = container.createBlockBlobURL(blobName);
        byte[] bytes = content.getBytes(StandardCharsets.UTF_8);
        Flowable<ByteBuffer> body = Flowable.just(ByteBuffer.wrap(bytes));
        blob.upload(body, bytes.length).blockingGet();
        return blob;
    }

    public String downloadText(String containerName, String blobName) throws StorageException {
        ContainerURL container = ensureContainer(containerName);
        BlockBlobURL blob = container.createBlockBlobURL(blobName);
        DownloadResponse response = blob.download().blockingGet();
        ReliableDownloadOptions options = new ReliableDownloadOptions().withMaxRetryRequests(5);
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        response.body(options).blockingForEach(byteBuffer -> writeBuffer(buffer, byteBuffer));
        return new String(buffer.toByteArray(), StandardCharsets.UTF_8);
    }

    public List<String> listBlobUris(String containerName) throws StorageException {
        ContainerURL container = ensureContainer(containerName);
        List<String> uris = new ArrayList<>();
        String marker = null;
        do {
            ContainerListBlobFlatSegmentResponse response = container.listBlobsFlatSegment(marker, null).blockingGet();
            if (response.body().segment() != null) {
                for (BlobItem item : response.body().segment().blobItems()) {
                    uris.add(container.createBlockBlobURL(item.name()).toURL().toString());
                }
            }
            marker = response.body().nextMarker();
        } while (marker != null && !marker.isEmpty());
        return uris;
    }

    public void downloadToFile(String containerName, String blobName, Path destination)
            throws StorageException, IOException {
        ContainerURL container = ensureContainer(containerName);
        BlockBlobURL blob = container.createBlockBlobURL(blobName);
        Path parent = destination.getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }
        ReliableDownloadOptions options = new ReliableDownloadOptions().withMaxRetryRequests(5);
        try (OutputStream stream = Files.newOutputStream(destination)) {
            blob.download().blockingGet()
                .body(options)
                .blockingForEach(byteBuffer -> writeBuffer(stream, byteBuffer));
        } catch (UncheckedIOException ex) {
            throw ex.getCause();
        }
    }

    public boolean deleteBlob(String containerName, String blobName) throws StorageException {
        ContainerURL container = ensureContainer(containerName);
        BlockBlobURL blob = container.createBlockBlobURL(blobName);
        try {
            blob.delete().blockingGet();
            return true;
        } catch (StorageException ex) {
            if (ex.statusCode() == 404) {
                return false;
            }
            throw ex;
        }
    }

    private void writeBuffer(OutputStream stream, ByteBuffer buffer) {
        byte[] chunk = new byte[buffer.remaining()];
        buffer.get(chunk);
        try {
            stream.write(chunk);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
