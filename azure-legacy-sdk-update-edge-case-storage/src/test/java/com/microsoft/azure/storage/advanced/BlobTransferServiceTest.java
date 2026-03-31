package com.microsoft.azure.storage.advanced;

import com.microsoft.azure.storage.StorageException;
import com.microsoft.azure.storage.blob.CloudBlobContainer;
import com.microsoft.azure.storage.blob.CloudBlockBlob;
import com.microsoft.azure.storage.blob.ListBlobItem;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class BlobTransferServiceTest {

    @Mock private CloudBlobContainer mockSourceContainer;
    @Mock private CloudBlobContainer mockTargetContainer;
    @Mock private CloudBlockBlob mockSourceBlob;
    @Mock private CloudBlockBlob mockTargetBlob;
    @Mock private StorageErrorRecovery mockErrorRecovery;

    private BlobResourceCache<CloudBlockBlob> realCache;
    private BlobTransferService service;

    @Before
    public void setUp() {
        realCache = new BlobResourceCache<>();
        service = new BlobTransferService(mockErrorRecovery, realCache);
    }

    @Test
    public void testCopyBlobWithMetadata() throws StorageException, URISyntaxException {
        String blobName = "document.pdf";
        HashMap<String, String> metadata = new HashMap<>();
        metadata.put("department", "engineering");
        metadata.put("classification", "internal");

        when(mockSourceContainer.getBlockBlobReference(blobName)).thenReturn(mockSourceBlob);
        when(mockTargetContainer.getBlockBlobReference(blobName)).thenReturn(mockTargetBlob);
        when(mockTargetBlob.startCopy(mockSourceBlob)).thenReturn("copy-id-1");

        CloudBlockBlob result = service.copyBlobWithMetadata(
                mockSourceContainer, mockTargetContainer, blobName, metadata);

        assertSame(mockTargetBlob, result);
        verify(mockTargetBlob).startCopy(mockSourceBlob);
        verify(mockTargetBlob).setMetadata(metadata);
        verify(mockTargetBlob).uploadMetadata();
    }

    @Test
    public void testCopyBlobInvalidatesCacheEntry() throws StorageException, URISyntaxException {
        String blobName = "cached.txt";
        realCache.getOrFetch(blobName, () -> mockSourceBlob);
        assertTrue(realCache.contains(blobName));

        when(mockSourceContainer.getBlockBlobReference(blobName)).thenReturn(mockSourceBlob);
        when(mockTargetContainer.getBlockBlobReference(blobName)).thenReturn(mockTargetBlob);
        when(mockTargetBlob.startCopy(mockSourceBlob)).thenReturn("copy-id-2");

        service.copyBlobWithMetadata(mockSourceContainer, mockTargetContainer,
                blobName, new HashMap<>());

        assertTrue("Cache entry should be invalidated after copy", !realCache.contains(blobName));
    }

    @Test
    public void testCopyAllBlobsSuccess() throws StorageException, URISyntaxException {
        when(mockSourceBlob.getName()).thenReturn("file1.txt");
        List<ListBlobItem> items = Arrays.asList((ListBlobItem) mockSourceBlob);
        when(mockSourceContainer.listBlobs()).thenReturn((Iterable) items);
        when(mockTargetContainer.getBlockBlobReference("file1.txt")).thenReturn(mockTargetBlob);
        when(mockTargetBlob.startCopy(mockSourceBlob)).thenReturn("copy-id-3");

        List<CloudBlockBlob> copied = service.copyAllBlobs(mockSourceContainer, mockTargetContainer);
        assertEquals(1, copied.size());
        assertSame(mockTargetBlob, copied.get(0));
    }

    @Test
    public void testCopyAllBlobsSkipsOnError() throws StorageException, URISyntaxException {
        StorageException skipError = new StorageException("BlobNotFound",
                "Blob not found", 404, null, null);

        when(mockSourceBlob.getName()).thenReturn("missing.txt");
        List<ListBlobItem> items = Arrays.asList((ListBlobItem) mockSourceBlob);
        when(mockSourceContainer.listBlobs()).thenReturn((Iterable) items);
        when(mockTargetContainer.getBlockBlobReference("missing.txt"))
                .thenThrow(skipError);
        when(mockErrorRecovery.determineRecovery(skipError))
                .thenReturn(StorageErrorRecovery.RecoveryAction.SKIP);

        List<CloudBlockBlob> copied = service.copyAllBlobs(mockSourceContainer, mockTargetContainer);
        assertEquals(0, copied.size());
    }

    @Test
    public void testUploadWithErrorHandling() throws StorageException, URISyntaxException, java.io.IOException {
        when(mockSourceContainer.createIfNotExists()).thenReturn(true);
        when(mockSourceContainer.getBlockBlobReference("test.txt")).thenReturn(mockTargetBlob);
        doNothing().when(mockTargetBlob).uploadText("hello");

        CloudBlockBlob result = service.uploadWithErrorHandling(
                mockSourceContainer, "test.txt", "hello");

        assertSame(mockTargetBlob, result);
        verify(mockTargetBlob).uploadText("hello");
    }

    @Test
    public void testBuildBlobUriMap() throws Exception {
        URI blobUri = new URI("https://devaccount.blob.core.windows.net/c/blob1.txt");
        when(mockSourceBlob.getName()).thenReturn("blob1.txt");
        when(mockSourceBlob.getUri()).thenReturn(blobUri);

        List<ListBlobItem> items = Arrays.asList((ListBlobItem) mockSourceBlob);
        when(mockSourceContainer.listBlobs()).thenReturn((Iterable) items);

        Map<String, URI> uriMap = service.buildBlobUriMap(mockSourceContainer);
        assertEquals(1, uriMap.size());
        assertEquals(blobUri, uriMap.get("blob1.txt"));
    }

    @Test
    public void testTransformBlobs() throws StorageException, URISyntaxException {
        when(mockSourceBlob.getName()).thenReturn("data.csv");
        List<ListBlobItem> items = Arrays.asList((ListBlobItem) mockSourceBlob);
        when(mockSourceContainer.listBlobs()).thenReturn((Iterable) items);

        Function<CloudBlockBlob, String> transformer = blob -> "processed:" + blob.getName();
        List<String> results = service.transformBlobs(mockSourceContainer, transformer);

        assertEquals(1, results.size());
        assertEquals("processed:data.csv", results.get(0));
    }
}
