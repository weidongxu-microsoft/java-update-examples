package com.azure.storagev12.blob;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.util.List;

public final class StorageV12Runner {

    public static void main(String[] args) {
        try {
            new StorageV12Runner().run(args == null ? new String[0] : args);
        } catch (IllegalArgumentException e) {
            System.err.println(e.getMessage());
            System.exit(2);
        } catch (IOException e) {
            System.err.println("Failed to execute blob workflow: " + e.getMessage());
            e.printStackTrace(System.err);
            System.exit(1);
        }
    }

    private void run(String[] args) throws IOException {
        String accountName = requireEnv("STORAGE_V12_ACCOUNT_NAME");
        String accountKey = requireEnv("STORAGE_V12_ACCOUNT_KEY");
        String containerName = System.getenv().getOrDefault("STORAGE_V12_CONTAINER", "documents-v12");

        BlobV12Manager manager = BlobV12Manager.fromAccount(accountName, accountKey);
        if (args.length == 0) {
            runLifecycle(manager, containerName);
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
                throw new IllegalArgumentException("Unsupported command '" + command + "'. Use 'list' or 'download'.");
        }
    }

    private void runLifecycle(BlobV12Manager manager, String containerName) throws IOException {
        String blobName = "guide-overview.txt";
        String payload = "V12 blob generated at " + Instant.now();

        manager.uploadText(containerName, blobName, payload);
        String downloaded = manager.downloadText(containerName, blobName);
        System.out.printf("Downloaded blob '%s' with %d characters%n", blobName, downloaded.length());

        manager.deleteBlob(containerName, blobName);
        System.out.println("Blob deleted. Workflow complete.");
    }

    private void executeList(BlobV12Manager manager, String containerName) {
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

    private void executeDownload(BlobV12Manager manager, String containerName, String[] args) throws IOException {
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
            throw new IllegalArgumentException("Environment variable '" + key + "' must be set.");
        }
        return value;
    }
}
