package com.microsoft.azure.storagev10.blob;

import com.microsoft.azure.storage.blob.BlockBlobURL;
import com.microsoft.azure.storage.blob.ContainerURL;
import com.microsoft.azure.storage.blob.DownloadResponse;
import com.microsoft.azure.storage.blob.ReliableDownloadOptions;
import com.microsoft.azure.storage.blob.ServiceURL;
import com.microsoft.azure.storage.blob.StorageException;
import com.microsoft.azure.storage.blob.models.BlobItem;
import com.microsoft.azure.storage.blob.models.BlockBlobUploadResponse;
import com.microsoft.azure.storage.blob.models.ContainerCreateResponse;
import com.microsoft.azure.storage.blob.models.ContainerListBlobFlatSegmentResponse;
import com.microsoft.azure.storage.blob.models.ListBlobsFlatSegmentResponse;
import io.reactivex.Flowable;
import io.reactivex.Single;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertSame;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class BlobV10ManagerTest {

    @Mock
    private ServiceURL serviceURL;

    @Mock
    private ContainerURL containerURL;

    @Mock
    private BlockBlobURL blockBlobURL;

    private BlobV10Manager manager;

    @Before
    public void setUp() {
        manager = new BlobV10Manager(serviceURL);
        when(serviceURL.createContainerURL(anyString())).thenReturn(containerURL);
        when(containerURL.createBlockBlobURL(anyString())).thenReturn(blockBlobURL);
        when(containerURL.create()).thenReturn(Single.just(mock(ContainerCreateResponse.class)));
    }

    @Test
    public void ensureContainerCreatesContainerWhenMissing() throws StorageException {
        ContainerURL actual = manager.ensureContainer("documents");

        verify(containerURL).create();
        assertSame(containerURL, actual);
    }

    @Test
    public void ensureContainerIgnoresConflict() throws StorageException {
        StorageException conflict = mock(StorageException.class);
        when(conflict.statusCode()).thenReturn(409);
        when(containerURL.create()).thenReturn(Single.error(conflict));

        ContainerURL actual = manager.ensureContainer("documents");

        assertSame(containerURL, actual);
    }

    @Test
    public void uploadTextStreamsContent() throws StorageException {
        when(blockBlobURL.upload(any(), eq(7L))).thenReturn(Single.just(mock(BlockBlobUploadResponse.class)));

        BlockBlobURL actual = manager.uploadText("documents", "note.txt", "payload");

        ArgumentCaptor<Flowable<ByteBuffer>> captor = ArgumentCaptor.forClass(Flowable.class);
        verify(blockBlobURL).upload(captor.capture(), eq(7L));
        String data = captor.getValue()
                .map(byteBuffer -> {
                    byte[] bytes = new byte[byteBuffer.remaining()];
                    byteBuffer.get(bytes);
                    return new String(bytes, StandardCharsets.UTF_8);
                })
                .blockingFirst();
        assertThat(data, is("payload"));
        assertSame(blockBlobURL, actual);
    }

    @Test
    public void downloadTextAggregatesResponse() throws StorageException {
        DownloadResponse downloadResponse = mock(DownloadResponse.class);
        when(blockBlobURL.download()).thenReturn(Single.just(downloadResponse));
        Flowable<ByteBuffer> body = Flowable.just(ByteBuffer.wrap("part1".getBytes(StandardCharsets.UTF_8)),
                ByteBuffer.wrap("part2".getBytes(StandardCharsets.UTF_8)));
        when(downloadResponse.body(any(ReliableDownloadOptions.class))).thenReturn(body);

        String result = manager.downloadText("documents", "blob.txt");

        assertThat(result, is("part1part2"));
    }

    @Test
    public void listBlobUrisCollectsItems() throws StorageException, MalformedURLException {
        ContainerListBlobFlatSegmentResponse response = mock(ContainerListBlobFlatSegmentResponse.class);
        ListBlobsFlatSegmentResponse body = mock(ListBlobsFlatSegmentResponse.class);
        com.microsoft.azure.storage.blob.models.BlobFlatListSegment segment =
                mock(com.microsoft.azure.storage.blob.models.BlobFlatListSegment.class);
        BlobItem blobItem = mock(BlobItem.class);
        when(blobItem.name()).thenReturn("alpha.txt");
        when(segment.blobItems()).thenReturn(Collections.singletonList(blobItem));
        when(body.segment()).thenReturn(segment);
        when(body.nextMarker()).thenReturn("");
        when(response.body()).thenReturn(body);
        when(containerURL.listBlobsFlatSegment(null, null)).thenReturn(Single.just(response));
        URL blobUrl = new URL("https://example.blob.core.windows.net/documents/alpha.txt");
        when(containerURL.createBlockBlobURL("alpha.txt")).thenReturn(blockBlobURL);
        when(blockBlobURL.toURL()).thenReturn(blobUrl);

        List<String> uris = manager.listBlobUris("documents");

        assertThat(uris, is(Collections.singletonList(blobUrl.toString())));
    }

    @Test
    public void downloadToFileWritesBytes() throws Exception {
        DownloadResponse downloadResponse = mock(DownloadResponse.class);
        when(blockBlobURL.download()).thenReturn(Single.just(downloadResponse));
        Flowable<ByteBuffer> body = Flowable.just(ByteBuffer.wrap("payload".getBytes(StandardCharsets.UTF_8)));
        when(downloadResponse.body(any(ReliableDownloadOptions.class))).thenReturn(body);
        Path target = Files.createTempFile("blob-v10", ".txt");

        manager.downloadToFile("documents", "note.txt", target);

        String fileContents = new String(Files.readAllBytes(target), StandardCharsets.UTF_8);
        assertThat(fileContents, is("payload"));
    }

    @Test
    public void deleteBlobReturnsFalseWhenMissing() throws StorageException {
        StorageException missing = mock(StorageException.class);
        when(missing.statusCode()).thenReturn(404);
        when(blockBlobURL.delete()).thenThrow(missing);

        boolean deleted = manager.deleteBlob("documents", "ghost.txt");

        assertThat(deleted, is(false));
    }

    @Test
    public void deleteBlobReturnsTrueWhenRemoved() throws StorageException {
        when(blockBlobURL.delete()).thenReturn(Single.just(mock(com.microsoft.azure.storage.blob.models.BlobDeleteResponse.class)));

        boolean deleted = manager.deleteBlob("documents", "note.txt");

        assertThat(deleted, is(true));
    }
}
