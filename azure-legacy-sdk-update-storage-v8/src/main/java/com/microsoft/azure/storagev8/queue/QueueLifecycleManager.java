package com.microsoft.azure.storagev8.queue;

import com.microsoft.azure.storage.CloudStorageAccount;
import com.microsoft.azure.storage.StorageException;
import com.microsoft.azure.storage.queue.CloudQueue;
import com.microsoft.azure.storage.queue.CloudQueueClient;
import com.microsoft.azure.storage.queue.CloudQueueMessage;

import java.net.URISyntaxException;
import java.security.InvalidKeyException;
import java.util.Objects;

/**
 * Minimal wrapper that mirrors the queue snippets in the migration guide so callers have a simple entry point.
 */
public class QueueLifecycleManager {

    private final CloudQueueClient queueClient;

    public QueueLifecycleManager(CloudQueueClient queueClient) {
        this.queueClient = Objects.requireNonNull(queueClient, "queueClient");
    }

    public static QueueLifecycleManager fromConnectionString(String connectionString)
            throws URISyntaxException, InvalidKeyException {
        CloudStorageAccount account = CloudStorageAccount.parse(connectionString);
        CloudQueueClient queueClient = account.createCloudQueueClient();
        return new QueueLifecycleManager(queueClient);
    }

    public CloudQueue ensureQueue(String queueName) throws URISyntaxException, StorageException {
        CloudQueue queue = queueClient.getQueueReference(queueName);
        queue.createIfNotExists();
        return queue;
    }

    public void enqueueMessage(String queueName, String content) throws URISyntaxException, StorageException {
        CloudQueue queue = ensureQueue(queueName);
        queue.addMessage(new CloudQueueMessage(content));
    }

    public String peekMessage(String queueName) throws URISyntaxException, StorageException {
        CloudQueue queue = ensureQueue(queueName);
        CloudQueueMessage message = queue.peekMessage();
        return message == null ? null : message.getMessageContentAsString();
    }

    public void clearQueue(String queueName) throws URISyntaxException, StorageException {
        CloudQueue queue = ensureQueue(queueName);
        queue.clear();
    }
}
