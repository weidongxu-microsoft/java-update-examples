package com.azure.storagev12.blob;

import com.azure.core.http.rest.PagedIterable;
import com.azure.storage.blob.BlobClient;
import com.azure.storage.blob.BlobContainerClient;
import com.azure.storage.blob.BlobServiceClient;
import com.azure.storage.blob.models.BlobItem;
import com.azure.storage.blob.models.BlobStorageException;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertFalse;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class BlobV12ManagerTest {

    @Mock
    private BlobServiceClient serviceClient;

    @Mock
    private BlobContainerClient containerClient;

    @Mock
    private BlobClient blobClient;

    private BlobV12Manager manager;

    @Before
    public void setUp() {
        manager = new BlobV12Manager(serviceClient);
        when(serviceClient.getBlobContainerClient(anyString())).thenReturn(containerClient);
        when(containerClient.getBlobClient(anyString())).thenReturn(blobClient);
        when(containerClient.exists()).thenReturn(false);
    }

    @Test
    public void ensureContainerCreatesContainerWhenMissing() {
        BlobContainerClient actual = manager.ensureContainer("documents");

        verify(containerClient).create();
        assertSame(containerClient, actual);
    }

    @Test
    public void ensureContainerSkipsCreationWhenExists() {
        when(containerClient.exists()).thenReturn(true);

        BlobContainerClient actual = manager.ensureContainer("documents");

        assertSame(containerClient, actual);
    }

    @Test
    public void uploadTextStreamsContent() {
        ArgumentCaptor<ByteArrayInputStream> streamCaptor = ArgumentCaptor.forClass(ByteArrayInputStream.class);

        BlobClient actual = manager.uploadText("documents", "note.txt", "payload");

        verify(blobClient).upload(streamCaptor.capture(), eq(7L), eq(true));
        ByteArrayInputStream captured = streamCaptor.getValue();
        byte[] data = new byte[7];
        try {
            captured.read(data);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        assertThat(new String(data, StandardCharsets.UTF_8), is("payload"));
        assertSame(blobClient, actual);
    }

    @Test
    public void downloadTextReadsContent() {
        doAnswer(invocation -> {
            ByteArrayOutputStream stream = invocation.getArgument(0);
            stream.write("part1part2".getBytes(StandardCharsets.UTF_8));
            return null;
        }).when(blobClient).downloadStream(any(ByteArrayOutputStream.class));

        String result = manager.downloadText("documents", "blob.txt");

        assertThat(result, is("part1part2"));
    }

    @Test
    public void listBlobUrisCollectsItems() {
        BlobItem blobItem = mock(BlobItem.class);
        when(blobItem.getName()).thenReturn("alpha.txt");
        PagedIterable<BlobItem> pagedIterable = mock(PagedIterable.class);
        when(pagedIterable.iterator()).thenReturn(Collections.singletonList(blobItem).iterator());
        when(containerClient.listBlobs()).thenReturn(pagedIterable);
        when(containerClient.exists()).thenReturn(true);
        when(containerClient.getBlobClient("alpha.txt")).thenReturn(blobClient);
        when(blobClient.getBlobUrl()).thenReturn("https://example.blob.core.windows.net/documents/alpha.txt");

        List<String> uris = manager.listBlobUris("documents");

        assertThat(uris, is(Collections.singletonList("https://example.blob.core.windows.net/documents/alpha.txt")));
    }

    @Test
    public void downloadToFileWritesBytes() throws Exception {
        Path target = Files.createTempFile("blob-v12", ".txt");
        doAnswer(invocation -> {
            String path = invocation.getArgument(0);
            Files.write(Path.of(path), "payload".getBytes(StandardCharsets.UTF_8));
            return null;
        }).when(blobClient).downloadToFile(anyString(), anyBoolean());

        manager.downloadToFile("documents", "note.txt", target);

        String fileContents = new String(Files.readAllBytes(target), StandardCharsets.UTF_8);
        assertThat(fileContents, is("payload"));
    }

    @Test
    public void deleteBlobReturnsFalseWhenMissing() {
        BlobStorageException missing = mock(BlobStorageException.class);
        when(missing.getStatusCode()).thenReturn(404);
        doThrow(missing).when(blobClient).delete();

        boolean deleted = manager.deleteBlob("documents", "ghost.txt");

        assertThat(deleted, is(false));
    }

    @Test
    public void deleteBlobReturnsTrueWhenRemoved() {
        boolean deleted = manager.deleteBlob("documents", "note.txt");

        verify(blobClient).delete();
        assertThat(deleted, is(true));
    }
}
