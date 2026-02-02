package com.microsoft.azure.storagev8.queue;

import com.microsoft.azure.storage.StorageException;
import com.microsoft.azure.storage.queue.CloudQueue;
import com.microsoft.azure.storage.queue.CloudQueueClient;
import com.microsoft.azure.storage.queue.CloudQueueMessage;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.net.URISyntaxException;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertSame;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class QueueLifecycleManagerTest {

    @Mock
    private CloudQueueClient queueClient;

    @Mock
    private CloudQueue queue;

    private QueueLifecycleManager manager;

    @Before
    public void setUp() throws Exception {
        manager = new QueueLifecycleManager(queueClient);
        when(queueClient.getQueueReference(anyString())).thenReturn(queue);
    }

    @Test
    public void ensureQueueCreatesQueueWhenMissing() throws StorageException, URISyntaxException {
        CloudQueue actual = manager.ensureQueue("notifications");

        verify(queue).createIfNotExists();
        assertSame(queue, actual);
    }

    @Test
    public void enqueueMessageAddsMessage() throws StorageException, URISyntaxException {
        manager.enqueueMessage("notifications", "hello world");

        ArgumentCaptor<CloudQueueMessage> captor = ArgumentCaptor.forClass(CloudQueueMessage.class);
        verify(queue).addMessage(captor.capture());
        assertThat(captor.getValue().getMessageContentAsString(), is("hello world"));
    }

    @Test
    public void peekMessageReturnsContent() throws StorageException, URISyntaxException {
        CloudQueueMessage message = new CloudQueueMessage("peek me");
        when(queue.peekMessage()).thenReturn(message);

        String result = manager.peekMessage("notifications");

        assertThat(result, is("peek me"));
    }

    @Test
    public void clearQueueDeletesMessages() throws StorageException, URISyntaxException {
        manager.clearQueue("notifications");

        verify(queue).clear();
    }
}
