package com.microsoft.azure.storagev8.blob;

import com.microsoft.azure.storage.StorageException;

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

        BlobLifecycleManager manager = BlobLifecycleManager.fromConnectionString(connectionString);
        manager.ensureContainer(containerName);

        if (args.length == 0) {
            executeLifecycleDemo(manager, containerName);
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
