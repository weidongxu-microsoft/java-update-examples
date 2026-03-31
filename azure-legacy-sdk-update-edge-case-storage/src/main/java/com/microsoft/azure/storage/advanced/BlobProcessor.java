package com.microsoft.azure.storage.advanced;

import com.microsoft.azure.storage.StorageException;
import com.microsoft.azure.storage.blob.CloudBlockBlob;

@FunctionalInterface
public interface BlobProcessor {
    void process(CloudBlockBlob blob) throws StorageException;
}
