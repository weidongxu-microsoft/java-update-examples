package com.microsoft.azure.storagev8.fileshare;

import com.microsoft.azure.storage.CloudStorageAccount;
import com.microsoft.azure.storage.StorageException;
import com.microsoft.azure.storage.file.CloudFile;
import com.microsoft.azure.storage.file.CloudFileClient;
import com.microsoft.azure.storage.file.CloudFileDirectory;
import com.microsoft.azure.storage.file.CloudFileShare;

import java.io.IOException;
import java.net.URISyntaxException;
import java.security.InvalidKeyException;
import java.util.Objects;

/**
 * Encapsulates the v8 file share object model to keep demos concise.
 */
public class FileShareLifecycleManager {

    private final CloudFileClient fileClient;

    public FileShareLifecycleManager(CloudFileClient fileClient) {
        this.fileClient = Objects.requireNonNull(fileClient, "fileClient");
    }

    public static FileShareLifecycleManager fromConnectionString(String connectionString)
            throws URISyntaxException, InvalidKeyException {
        CloudStorageAccount account = CloudStorageAccount.parse(connectionString);
        CloudFileClient fileClient = account.createCloudFileClient();
        return new FileShareLifecycleManager(fileClient);
    }

    public CloudFileShare ensureShare(String shareName) throws URISyntaxException, StorageException {
        CloudFileShare share = fileClient.getShareReference(shareName);
        share.createIfNotExists();
        return share;
    }

    public void uploadTextFile(String shareName, String fileName, String content)
            throws URISyntaxException, StorageException, IOException {
        CloudFile file = getFileReference(shareName, fileName);
        file.uploadText(content);
    }

    public String downloadTextFile(String shareName, String fileName)
            throws URISyntaxException, StorageException, IOException {
        CloudFile file = getFileReference(shareName, fileName);
        return file.downloadText();
    }

    public boolean deleteFile(String shareName, String fileName)
            throws URISyntaxException, StorageException {
        CloudFile file = getFileReference(shareName, fileName);
        return file.deleteIfExists();
    }

    private CloudFile getFileReference(String shareName, String fileName)
            throws URISyntaxException, StorageException {
        CloudFileShare share = ensureShare(shareName);
        CloudFileDirectory rootDirectory = share.getRootDirectoryReference();
        return rootDirectory.getFileReference(fileName);
    }
}
