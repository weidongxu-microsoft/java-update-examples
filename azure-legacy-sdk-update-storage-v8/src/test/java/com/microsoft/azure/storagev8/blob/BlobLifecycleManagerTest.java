package com.microsoft.azure.storagev8.blob;

import com.microsoft.azure.storage.StorageException;
import com.microsoft.azure.storage.blob.CloudBlobClient;
import com.microsoft.azure.storage.blob.CloudBlobContainer;
import com.microsoft.azure.storage.blob.CloudBlockBlob;
import com.microsoft.azure.storage.blob.ListBlobItem;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertSame;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class BlobLifecycleManagerTest {

    @Mock
    private CloudBlobClient blobClient;

    @Mock
    private CloudBlobContainer container;

    @Mock
    private CloudBlockBlob blockBlob;

    private BlobLifecycleManager manager;

    @Before
    public void setUp() {
        manager = new BlobLifecycleManager(blobClient);
    }

    @Test
    public void ensureContainerCreatesContainerWhenMissing() throws StorageException, URISyntaxException {
        when(blobClient.getContainerReference("documents")).thenReturn(container);

        CloudBlobContainer actual = manager.ensureContainer("documents");

        verify(container).createIfNotExists();
        assertSame(container, actual);
    }

    @Test
    public void uploadTextWritesPayload() throws StorageException, URISyntaxException, IOException {
        when(blobClient.getContainerReference("documents")).thenReturn(container);
        when(container.getBlockBlobReference("invoice.txt")).thenReturn(blockBlob);

        manager.uploadText("documents", "invoice.txt", "payload");

        verify(blockBlob).uploadText(eq("payload"), eq(StandardCharsets.UTF_8.name()), isNull(), isNull(), isNull());
    }

    @Test
    public void downloadTextReturnsContent() throws StorageException, URISyntaxException, IOException {
        when(blobClient.getContainerReference("documents")).thenReturn(container);
        when(container.getBlockBlobReference("invoice.txt")).thenReturn(blockBlob);
        when(blockBlob.downloadText(eq(StandardCharsets.UTF_8.name()), isNull(), isNull(), isNull()))
                .thenReturn("payload");

        String result = manager.downloadText("documents", "invoice.txt");

        assertThat(result, is("payload"));
    }

    @Test
    public void deleteBlobRemovesBlobWhenPresent() throws StorageException, URISyntaxException {
        when(blobClient.getContainerReference("documents")).thenReturn(container);
        when(container.getBlockBlobReference("invoice.txt")).thenReturn(blockBlob);
        when(blockBlob.deleteIfExists()).thenReturn(true);

        boolean deleted = manager.deleteBlob("documents", "invoice.txt");

        assertThat(deleted, is(true));
        verify(blockBlob).deleteIfExists();
    }

    @Test
    public void listBlobUrisReturnsEveryBlob() throws StorageException, URISyntaxException {
        when(blobClient.getContainerReference("documents")).thenReturn(container);
        ListBlobItem first = mock(ListBlobItem.class);
        ListBlobItem second = mock(ListBlobItem.class);
        when(first.getUri()).thenReturn(URI.create("https://account.blob.core.windows.net/documents/alpha.txt"));
        when(second.getUri()).thenReturn(URI.create("https://account.blob.core.windows.net/documents/beta.txt"));
        when(container.listBlobs()).thenReturn(Arrays.asList(first, second));

        List<String> uris = manager.listBlobUris("documents");

        assertThat(uris, is(Arrays.asList(
                "https://account.blob.core.windows.net/documents/alpha.txt",
                "https://account.blob.core.windows.net/documents/beta.txt")));
    }

    @Test
    public void downloadToFileStreamsBlob() throws StorageException, URISyntaxException, IOException {
        when(blobClient.getContainerReference("documents")).thenReturn(container);
        when(container.getBlockBlobReference("invoice.txt")).thenReturn(blockBlob);
        Path target = Paths.get("build", "invoice.txt");

        manager.downloadToFile("documents", "invoice.txt", target);

        verify(blockBlob).downloadToFile(target.toString());
    }
}
