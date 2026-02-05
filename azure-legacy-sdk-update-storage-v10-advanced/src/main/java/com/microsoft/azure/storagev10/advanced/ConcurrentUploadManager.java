package com.microsoft.azure.storagev10.advanced;

import com.microsoft.azure.storage.blob.BlockBlobURL;
import com.microsoft.azure.storage.blob.ContainerURL;
import com.microsoft.azure.storage.blob.PipelineOptions;
import com.microsoft.azure.storage.blob.ServiceURL;
import com.microsoft.azure.storage.blob.SharedKeyCredentials;
import com.microsoft.azure.storage.blob.StorageException;
import com.microsoft.azure.storage.blob.StorageURL;
import com.microsoft.azure.storage.blob.TransferManager;
import com.microsoft.azure.storage.blob.models.BlockBlobCommitBlockListResponse;
import io.reactivex.Flowable;
import io.reactivex.Single;
import io.reactivex.schedulers.Schedulers;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousFileChannel;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.security.InvalidKeyException;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

/**
 * Manages concurrent blob upload operations using Azure Storage SDK v10.
 * Demonstrates async upload with parallel transfer capabilities.
 */
public class ConcurrentUploadManager {

    private final ServiceURL serviceURL;
    private final int blockSizeBytes;
    private final int maxConcurrency;

    /**
     * Creates a new instance with default transfer options.
     *
     * @param serviceURL the service URL
     */
    public ConcurrentUploadManager(ServiceURL serviceURL) {
        this(serviceURL, 4 * 1024 * 1024, 5); // 4MB blocks, 5 concurrent
    }

    /**
     * Creates a new instance with custom transfer options.
     *
     * @param serviceURL      the service URL
     * @param blockSizeBytes  size of each block in bytes
     * @param maxConcurrency  maximum number of concurrent operations
     */
    public ConcurrentUploadManager(ServiceURL serviceURL, int blockSizeBytes, int maxConcurrency) {
        this.serviceURL = Objects.requireNonNull(serviceURL, "serviceURL");
        this.blockSizeBytes = blockSizeBytes;
        this.maxConcurrency = maxConcurrency;
    }

    /**
     * Creates a manager from account credentials.
     *
     * @param accountName account name
     * @param accountKey  account key
     * @return new manager instance
     * @throws MalformedURLException if URL is invalid
     * @throws InvalidKeyException   if key is invalid
     */
    public static ConcurrentUploadManager fromAccount(String accountName, String accountKey)
            throws MalformedURLException, InvalidKeyException {
        return fromAccount(accountName, accountKey, 4 * 1024 * 1024, 5);
    }

    /**
     * Creates a manager from account credentials with custom options.
     *
     * @param accountName    account name
     * @param accountKey     account key
     * @param blockSizeBytes size of each block in bytes
     * @param maxConcurrency maximum number of concurrent operations
     * @return new manager instance
     * @throws MalformedURLException if URL is invalid
     * @throws InvalidKeyException   if key is invalid
     */
    public static ConcurrentUploadManager fromAccount(String accountName, String accountKey,
                                                      int blockSizeBytes, int maxConcurrency)
            throws MalformedURLException, InvalidKeyException {
        Objects.requireNonNull(accountName, "accountName");
        Objects.requireNonNull(accountKey, "accountKey");
        SharedKeyCredentials credentials = new SharedKeyCredentials(accountName, accountKey);
        URL endpoint = new URL(String.format("https://%s.blob.core.windows.net", accountName));
        ServiceURL serviceURL = new ServiceURL(endpoint, StorageURL.createPipeline(credentials, new PipelineOptions()));
        return new ConcurrentUploadManager(serviceURL, blockSizeBytes, maxConcurrency);
    }

    /**
     * Ensures a container exists, creating it if necessary.
     *
     * @param containerName container name
     * @return container URL
     * @throws StorageException if operation fails
     */
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

    /**
     * Uploads a file to blob storage using concurrent upload with async operations.
     * This method demonstrates parallel transfer capabilities similar to ParallelTransferOptions.
     *
     * @param containerName container name
     * @param blobName      blob name
     * @param filePath      path to file to upload
     * @return single that completes when upload is finished
     */
    public Single<com.microsoft.azure.storage.blob.CommonRestResponse> uploadFileAsync(String containerName, String blobName, Path filePath) {
        return uploadFileAsync(containerName, blobName, filePath, null, null, null);
    }

    /**
     * Uploads a file to blob storage using concurrent upload with async operations.
     * This method demonstrates parallel transfer capabilities similar to ParallelTransferOptions.
     *
     * @param containerName     container name
     * @param blobName          blob name
     * @param filePath          path to file to upload
     * @param httpHeaders       HTTP headers to set on the blob
     * @param metadata          metadata to set on the blob
     * @param accessConditions  access conditions for the upload
     * @return single that completes when upload is finished
     */
    public Single<com.microsoft.azure.storage.blob.CommonRestResponse> uploadFileAsync(
            String containerName, 
            String blobName, 
            Path filePath,
            com.microsoft.azure.storage.blob.models.BlobHTTPHeaders httpHeaders,
            com.microsoft.azure.storage.blob.Metadata metadata,
            com.microsoft.azure.storage.blob.BlobAccessConditions accessConditions) {
        return Single.fromCallable(() -> ensureContainer(containerName))
                .subscribeOn(Schedulers.io())
                .flatMap(container -> {
                    BlockBlobURL blob = container.createBlockBlobURL(blobName);
                    File file = filePath.toFile();
                    
                    try {
                        AsynchronousFileChannel channel = AsynchronousFileChannel.open(filePath, StandardOpenOption.READ);
                        
                        // Use TransferManager for concurrent upload with specified block size and concurrency
                        com.microsoft.azure.storage.blob.TransferManagerUploadToBlockBlobOptions options = 
                                new com.microsoft.azure.storage.blob.TransferManagerUploadToBlockBlobOptions(
                                        null, httpHeaders, metadata, accessConditions, maxConcurrency);
                        
                        return TransferManager.uploadFileToBlockBlob(
                                channel,
                                blob,
                                blockSizeBytes,
                                options
                        ).doFinally(() -> {
                            try {
                                channel.close();
                            } catch (IOException e) {
                                // Log or handle close error
                            }
                        });
                    } catch (IOException e) {
                        return Single.error(e);
                    }
                });
    }

    /**
     * Uploads a file to blob storage synchronously with concurrent operations.
     *
     * @param containerName container name
     * @param blobName      blob name
     * @param filePath      path to file to upload
     * @return commit response
     * @throws StorageException if operation fails
     */
    public com.microsoft.azure.storage.blob.CommonRestResponse uploadFile(String containerName, String blobName, Path filePath)
            throws StorageException {
        return uploadFile(containerName, blobName, filePath, null, null, null);
    }

    /**
     * Uploads a file to blob storage synchronously with concurrent operations.
     *
     * @param containerName     container name
     * @param blobName          blob name
     * @param filePath          path to file to upload
     * @param httpHeaders       HTTP headers to set on the blob
     * @param metadata          metadata to set on the blob
     * @param accessConditions  access conditions for the upload
     * @return commit response
     * @throws StorageException if operation fails
     */
    public com.microsoft.azure.storage.blob.CommonRestResponse uploadFile(
            String containerName, 
            String blobName, 
            Path filePath,
            com.microsoft.azure.storage.blob.models.BlobHTTPHeaders httpHeaders,
            com.microsoft.azure.storage.blob.Metadata metadata,
            com.microsoft.azure.storage.blob.BlobAccessConditions accessConditions)
            throws StorageException {
        try {
            return uploadFileAsync(containerName, blobName, filePath, httpHeaders, metadata, accessConditions)
                    .timeout(5, TimeUnit.MINUTES)
                    .blockingGet();
        } catch (Exception e) {
            if (e.getCause() instanceof StorageException) {
                throw (StorageException) e.getCause();
            }
            throw new RuntimeException("Upload failed", e);
        }
    }

    /**
     * Uploads data from a Flowable to blob storage with concurrent operations.
     * This demonstrates buffered upload similar to the AsyncBufferedUploadExample.
     *
     * @param containerName container name
     * @param blobName      blob name
     * @param data          flowable of data chunks
     * @param totalSize     total size of data, or -1 if unknown
     * @return single that completes when upload is finished
     */
    public Single<com.microsoft.azure.storage.blob.models.BlockBlobCommitBlockListResponse> uploadFromFlowableAsync(
            String containerName, String blobName, Flowable<ByteBuffer> data, long totalSize) {
        return Single.fromCallable(() -> ensureContainer(containerName))
                .subscribeOn(Schedulers.io())
                .flatMap(container -> {
                    BlockBlobURL blob = container.createBlockBlobURL(blobName);
                    
                    if (totalSize > 0) {
                        // If size is known, use TransferManager with Flowable
                        return TransferManager.uploadFromNonReplayableFlowable(
                                data,
                                blob,
                                blockSizeBytes,
                                maxConcurrency,
                                null
                        );
                    } else {
                        // For unknown size, buffer and upload
                        return data.toList()
                                .flatMap(buffers -> {
                                    long size = buffers.stream().mapToLong(ByteBuffer::remaining).sum();
                                    Flowable<ByteBuffer> bufferedData = Flowable.fromIterable(buffers);
                                    return TransferManager.uploadFromNonReplayableFlowable(
                                            bufferedData,
                                            blob,
                                            blockSizeBytes,
                                            maxConcurrency,
                                            null
                                    );
                                });
                    }
                });
    }

    /**
     * Gets the configured block size in bytes.
     *
     * @return block size
     */
    public int getBlockSizeBytes() {
        return blockSizeBytes;
    }

    /**
     * Gets the configured maximum concurrency.
     *
     * @return max concurrency
     */
    public int getMaxConcurrency() {
        return maxConcurrency;
    }

    /**
     * Gets the service URL.
     *
     * @return service URL
     */
    public ServiceURL getServiceURL() {
        return serviceURL;
    }
}
