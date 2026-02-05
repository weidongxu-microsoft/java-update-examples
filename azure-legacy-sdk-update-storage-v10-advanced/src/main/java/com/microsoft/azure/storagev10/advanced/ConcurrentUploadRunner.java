package com.microsoft.azure.storagev10.advanced;

import com.microsoft.azure.storage.blob.CommonRestResponse;
import com.microsoft.azure.storage.blob.Metadata;
import com.microsoft.azure.storage.blob.models.BlobHTTPHeaders;
import com.microsoft.azure.storage.blob.models.BlockBlobCommitBlockListResponse;
import io.reactivex.Flowable;
import io.reactivex.disposables.Disposable;

import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.InvalidKeyException;
import java.util.Random;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * Demonstrates concurrent blob upload operations using Azure Storage SDK v10.
 * Shows both file upload and streaming upload with async operations.
 */
public class ConcurrentUploadRunner {

    private static final String CONTAINER_NAME = "concurrent-uploads";

    public static void main(String[] args) {
        String accountName = System.getenv("AZURE_STORAGE_ACCOUNT_NAME");
        String accountKey = System.getenv("AZURE_STORAGE_ACCOUNT_KEY");

        if (accountName == null || accountKey == null) {
            System.out.println("Please set AZURE_STORAGE_ACCOUNT_NAME and AZURE_STORAGE_ACCOUNT_KEY environment variables");
            return;
        }

        try {
            runDemo(accountName, accountKey);
        } catch (Exception e) {
            System.err.println("Error running demo: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private static void runDemo(String accountName, String accountKey)
            throws MalformedURLException, InvalidKeyException, IOException, InterruptedException {
        
        // Create manager with custom transfer options: 2MB blocks, 8 concurrent
        ConcurrentUploadManager manager = ConcurrentUploadManager.fromAccount(
                accountName, accountKey, 2 * 1024 * 1024, 8);

        System.out.println("Azure Storage Concurrent Upload Demo");
        System.out.println("Block size: " + manager.getBlockSizeBytes() / 1024 + " KB");
        System.out.println("Max concurrency: " + manager.getMaxConcurrency());
        System.out.println();

        // Demo 1: Upload a file synchronously with concurrent operations
        demonstrateFileSyncUpload(manager);

        // Demo 2: Upload a file asynchronously
        demonstrateFileAsyncUpload(manager);

        // Demo 3: Upload from a Flowable stream
        demonstrateStreamUpload(manager);

        System.out.println("\nAll demos completed successfully!");
    }

    /**
     * Demonstrates synchronous file upload with concurrent operations.
     */
    private static void demonstrateFileSyncUpload(ConcurrentUploadManager manager) throws IOException {
        System.out.println("Demo 1: Synchronous file upload with concurrent blocks");
        
        // Create a temporary file with some content
        Path tempFile = createTempFile("sync-upload-", 5 * 1024 * 1024); // 5 MB
        
        try {
            long startTime = System.currentTimeMillis();
            
            // Set HTTP headers and metadata
            BlobHTTPHeaders httpHeaders = new BlobHTTPHeaders()
                    .withBlobContentType("application/octet-stream")
                    .withBlobContentDisposition("attachment; filename=\"sync-uploaded-file.dat\"");
            
            Metadata metadata = new Metadata();
            metadata.put("uploaded-by", "demo-app");
            metadata.put("upload-type", "synchronous");
            metadata.put("timestamp", String.valueOf(System.currentTimeMillis()));
            
            CommonRestResponse response = manager.uploadFile(
                    CONTAINER_NAME,
                    "sync-uploaded-file.dat",
                    tempFile,
                    httpHeaders,
                    metadata,
                    null  // No access conditions
            );
            
            long duration = System.currentTimeMillis() - startTime;
            
            System.out.println("  Upload completed in " + duration + " ms");
            System.out.println("  Status: " + response.statusCode());
            System.out.println();
        } finally {
            Files.deleteIfExists(tempFile);
        }
    }

    /**
     * Demonstrates asynchronous file upload with concurrent operations.
     */
    private static void demonstrateFileAsyncUpload(ConcurrentUploadManager manager)
            throws IOException, InterruptedException {
        System.out.println("Demo 2: Asynchronous file upload with concurrent blocks");
        
        // Create a temporary file with some content
        Path tempFile = createTempFile("async-upload-", 8 * 1024 * 1024); // 8 MB
        
        try {
            CountDownLatch latch = new CountDownLatch(1);
            long startTime = System.currentTimeMillis();
            
            Disposable subscription = manager.uploadFileAsync(
                    CONTAINER_NAME,
                    "async-uploaded-file.dat",
                    tempFile
            ).subscribe(
                    response -> {
                        long duration = System.currentTimeMillis() - startTime;
                        System.out.println("  Upload completed in " + duration + " ms");
                        System.out.println("  Status: " + response.statusCode());
                        System.out.println();
                        latch.countDown();
                    },
                    error -> {
                        System.err.println("  Upload failed: " + error.getMessage());
                        error.printStackTrace();
                        latch.countDown();
                    }
            );
            
            // Wait for completion
            latch.await(2, TimeUnit.MINUTES);
        } finally {
            Files.deleteIfExists(tempFile);
        }
    }

    /**
     * Demonstrates upload from a Flowable stream with concurrent operations.
     */
    private static void demonstrateStreamUpload(ConcurrentUploadManager manager) throws InterruptedException {
        System.out.println("Demo 3: Stream upload with concurrent blocks");
        
        // Create a Flowable that generates data chunks
        int chunkCount = 50;
        int chunkSize = 100 * 1024; // 100 KB per chunk
        
        Flowable<ByteBuffer> dataStream = Flowable.range(0, chunkCount)
                .map(i -> {
                    byte[] data = new byte[chunkSize];
                    new Random().nextBytes(data);
                    return ByteBuffer.wrap(data);
                });
        
        long totalSize = (long) chunkCount * chunkSize;
        
        CountDownLatch latch = new CountDownLatch(1);
        long startTime = System.currentTimeMillis();
        
        Disposable subscription = manager.uploadFromFlowableAsync(
                CONTAINER_NAME,
                "stream-uploaded-data.dat",
                dataStream,
                totalSize
        ).subscribe(
                response -> {
                    long duration = System.currentTimeMillis() - startTime;
                    System.out.println("  Upload completed in " + duration + " ms");
                    System.out.println("  Total size: " + totalSize / 1024 + " KB");
                    System.out.println("  ETag: " + response.headers().eTag());
                    System.out.println();
                    latch.countDown();
                },
                error -> {
                    System.err.println("  Upload failed: " + error.getMessage());
                    error.printStackTrace();
                    latch.countDown();
                }
        );
        
        // Wait for completion
        latch.await(2, TimeUnit.MINUTES);
    }

    /**
     * Creates a temporary file with random content for testing.
     */
    private static Path createTempFile(String prefix, long sizeBytes) throws IOException {
        Path tempFile = Files.createTempFile(prefix, ".dat");
        
        // Write random data to the file
        byte[] buffer = new byte[64 * 1024]; // 64 KB buffer
        long remaining = sizeBytes;
        
        try (java.io.OutputStream outputStream = Files.newOutputStream(tempFile)) {
            Random random = new Random();
            while (remaining > 0) {
                int writeSize = (int) Math.min(buffer.length, remaining);
                random.nextBytes(buffer);
                outputStream.write(buffer, 0, writeSize);
                remaining -= writeSize;
            }
        }
        
        return tempFile;
    }
}
