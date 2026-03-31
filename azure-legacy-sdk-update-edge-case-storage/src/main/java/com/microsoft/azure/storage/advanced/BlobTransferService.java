package com.microsoft.azure.storage.advanced;

import com.microsoft.azure.storage.StorageException;
import com.microsoft.azure.storage.blob.CloudBlobContainer;
import com.microsoft.azure.storage.blob.CloudBlockBlob;
import com.microsoft.azure.storage.blob.ListBlobItem;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

public class BlobTransferService {

    private final StorageErrorRecovery errorRecovery;
    private final BlobResourceCache<CloudBlockBlob> blobCache;

    public BlobTransferService(StorageErrorRecovery errorRecovery,
                                BlobResourceCache<CloudBlockBlob> blobCache) {
        this.errorRecovery = errorRecovery;
        this.blobCache = blobCache;
    }

    public CloudBlockBlob copyBlobWithMetadata(
            CloudBlobContainer source, CloudBlobContainer target,
            String blobName, HashMap<String, String> metadata)
            throws StorageException, URISyntaxException {
        CloudBlockBlob sourceBlob = source.getBlockBlobReference(blobName);
        CloudBlockBlob targetBlob = target.getBlockBlobReference(blobName);
        targetBlob.startCopy(sourceBlob);
        targetBlob.setMetadata(metadata);
        targetBlob.uploadMetadata();
        blobCache.invalidate(blobName);
        return targetBlob;
    }

    public List<CloudBlockBlob> copyAllBlobs(
            CloudBlobContainer source, CloudBlobContainer target)
            throws StorageException, URISyntaxException {
        List<CloudBlockBlob> copiedBlobs = new ArrayList<>();
        for (ListBlobItem item : source.listBlobs()) {
            if (item instanceof CloudBlockBlob) {
                CloudBlockBlob sourceBlob = (CloudBlockBlob) item;
                try {
                    CloudBlockBlob targetBlob = target.getBlockBlobReference(sourceBlob.getName());
                    targetBlob.startCopy(sourceBlob);
                    copiedBlobs.add(targetBlob);
                } catch (StorageException e) {
                    StorageErrorRecovery.RecoveryAction action = errorRecovery.determineRecovery(e);
                    if (action == StorageErrorRecovery.RecoveryAction.SKIP) {
                        continue;
                    } else if (action == StorageErrorRecovery.RecoveryAction.RETRY_WITH_BACKOFF) {
                        try {
                            Thread.sleep(1000);
                            CloudBlockBlob targetBlob = target.getBlockBlobReference(sourceBlob.getName());
                            targetBlob.startCopy(sourceBlob);
                            copiedBlobs.add(targetBlob);
                        } catch (InterruptedException ie) {
                            Thread.currentThread().interrupt();
                            throw e;
                        }
                    } else {
                        throw e;
                    }
                }
            }
        }
        return copiedBlobs;
    }

    public CloudBlockBlob uploadWithErrorHandling(
            CloudBlobContainer container, String blobName, String content)
            throws StorageException, URISyntaxException, IOException {
        try {
            container.createIfNotExists();
        } catch (StorageException e) {
            errorRecovery.handleWithRecovery(e, () -> {
                try {
                    container.createIfNotExists();
                } catch (StorageException ex) {
                    throw new RuntimeException(ex);
                }
            });
        }

        CloudBlockBlob blob = container.getBlockBlobReference(blobName);
        blob.uploadText(content);

        blobCache.getOrFetch(blobName, () -> {
            try {
                return container.getBlockBlobReference(blobName);
            } catch (URISyntaxException | StorageException ex) {
                throw new RuntimeException(ex);
            }
        });

        return blob;
    }

    public Map<String, URI> buildBlobUriMap(CloudBlobContainer container)
            throws StorageException, URISyntaxException {
        Map<String, URI> uriMap = new HashMap<>();
        for (ListBlobItem item : container.listBlobs()) {
            if (item instanceof CloudBlockBlob) {
                CloudBlockBlob blob = (CloudBlockBlob) item;
                URI uri = blobCache.getBlobUri(blob);
                uriMap.put(blob.getName(), uri);
            }
        }
        return uriMap;
    }

    public <R> List<R> transformBlobs(CloudBlobContainer container,
                                       Function<CloudBlockBlob, R> transformer)
            throws StorageException, URISyntaxException {
        List<R> results = new ArrayList<>();
        for (ListBlobItem item : container.listBlobs()) {
            if (item instanceof CloudBlockBlob) {
                results.add(transformer.apply((CloudBlockBlob) item));
            }
        }
        return results;
    }
}
