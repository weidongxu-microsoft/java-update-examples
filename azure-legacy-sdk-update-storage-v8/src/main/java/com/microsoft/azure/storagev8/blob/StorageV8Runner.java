package com.microsoft.azure.storagev8.blob;

import com.microsoft.azure.storage.StorageException;
import com.microsoft.azure.storagev8.fileshare.FileShareLifecycleManager;
import com.microsoft.azure.storagev8.queue.QueueLifecycleManager;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.InvalidKeyException;
import java.time.Instant;
import java.util.List;

public final class StorageV8Runner {

    public static void main(String[] args) {
        try {
            new StorageV8Runner().run(args == null ? new String[0] : args);
        } catch (InvalidKeyException | URISyntaxException | StorageException | IOException e) {
            System.err.println("Failed to execute blob workflow: " + e.getMessage());
            e.printStackTrace(System.err);
            System.exit(1);
        } catch (IllegalArgumentException e) {
            System.err.println(e.getMessage());
            System.exit(2);
        }
    }

    private void run(String[] args) throws InvalidKeyException, URISyntaxException, StorageException, IOException {
        String connectionString = requireEnv("AZURE_STORAGE_CONNECTION_STRING");
        String containerName = System.getenv().getOrDefault("STORAGE_V8_CONTAINER", "documents-container");
        String queueName = System.getenv().getOrDefault("STORAGE_V8_QUEUE", "notifications-queue");
        String shareName = System.getenv().getOrDefault("STORAGE_V8_SHARE", "documents-share");

        BlobLifecycleManager manager = BlobLifecycleManager.fromConnectionString(connectionString);
        QueueLifecycleManager queueManager = QueueLifecycleManager.fromConnectionString(connectionString);
        FileShareLifecycleManager fileShareManager = FileShareLifecycleManager.fromConnectionString(connectionString);
        manager.ensureContainer(containerName);

        if (args.length == 0) {
            executeLifecycleDemo(manager, containerName);
            runQueueWorkflow(queueManager, queueName);
            runFileShareWorkflow(fileShareManager, shareName);
            return;
        }

        String command = args[0].toLowerCase();
        switch (command) {
            case "list":
                executeList(manager, containerName);
                break;
            case "download":
                executeDownload(manager, containerName, args);
                break;
            default:
                throw new IllegalArgumentException("Unsupported command '" + command + "'. Expected 'list' or 'download'.");
        }
    }

    private void executeLifecycleDemo(BlobLifecycleManager manager, String containerName)
            throws StorageException, URISyntaxException, IOException {
        String blobName = "guide-overview.txt";
        String payload = "Blob generated at " + Instant.now();

        manager.uploadText(containerName, blobName, payload);
        String downloaded = manager.downloadText(containerName, blobName);
        System.out.printf("Downloaded blob '%s' with %d characters%n", blobName, downloaded.length());

        manager.deleteBlob(containerName, blobName);
        System.out.println("Blob deleted. Workflow complete.");
    }

    private void runQueueWorkflow(QueueLifecycleManager queueManager, String queueName)
            throws StorageException, URISyntaxException {
        String message = "V8 queue message created at " + Instant.now();
        queueManager.enqueueMessage(queueName, message);
        String peeked = queueManager.peekMessage(queueName);
        if (peeked != null) {
            System.out.printf("Peeked queue message: %s%n", peeked);
        } else {
            System.out.println("Queue is empty after enqueue attempt.");
        }
        queueManager.clearQueue(queueName);
        System.out.println("Queue cleared. Workflow complete.");
    }

    private void runFileShareWorkflow(FileShareLifecycleManager fileShareManager, String shareName)
            throws StorageException, URISyntaxException, IOException {
        String fileName = "guide-overview.txt";
        String content = "File share entry created at " + Instant.now();
        fileShareManager.uploadTextFile(shareName, fileName, content);
        String downloaded = fileShareManager.downloadTextFile(shareName, fileName);
        System.out.printf("Downloaded file share '%s' with %d characters%n", fileName, downloaded.length());
        fileShareManager.deleteFile(shareName, fileName);
        System.out.println("File share entry deleted. Workflow complete.");
    }

    private void executeList(BlobLifecycleManager manager, String containerName)
            throws StorageException, URISyntaxException {
        List<String> uris = manager.listBlobUris(containerName);
        if (uris.isEmpty()) {
            System.out.println("No blobs found in container '" + containerName + "'.");
            return;
        }

        System.out.println("Blobs in container '" + containerName + "':");
        for (String uri : uris) {
            System.out.println(" - " + uri);
        }
    }

    private void executeDownload(BlobLifecycleManager manager, String containerName, String[] args)
            throws StorageException, URISyntaxException, IOException {
        if (args.length < 3) {
            throw new IllegalArgumentException("Use: download <blobName> <destinationPath>");
        }
        String blobName = args[1];
        Path destination = Paths.get(args[2]);
        manager.downloadToFile(containerName, blobName, destination);
        System.out.printf("Blob '%s' downloaded to %s%n", blobName, destination);
    }

    private String requireEnv(String key) {
        String value = System.getenv(key);
        if (value == null || value.isEmpty()) {
            throw new IllegalStateException("Environment variable '" + key + "' must be set.");
        }
        return value;
    }
}
