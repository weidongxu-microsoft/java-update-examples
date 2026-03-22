package com.example.blobmanager.service;

import com.azure.core.http.rest.PagedIterable;
import com.azure.storage.blob.BlobClient;
import com.azure.storage.blob.BlobContainerClient;
import com.azure.storage.blob.BlobServiceClient;
import com.azure.storage.blob.models.BlobContainerItem;
import com.azure.storage.blob.models.BlobHttpHeaders;
import com.azure.storage.blob.models.BlobItem;
import com.azure.storage.blob.models.BlobItemProperties;
import com.azure.storage.blob.models.BlobProperties;
import com.azure.storage.blob.specialized.BlockBlobClient;
import com.example.blobmanager.model.BlobInfo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.time.OffsetDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BlobStorageServiceTest {

    @Mock
    private BlobServiceClient blobServiceClient;

    private BlobStorageService service;

    @BeforeEach
    void setUp() {
        service = new BlobStorageService();
        service.setBlobServiceClient(blobServiceClient);
    }

    @Test
    @SuppressWarnings("unchecked")
    void listContainers_returnsContainerNames() {
        BlobContainerItem c1 = mock(BlobContainerItem.class);
        BlobContainerItem c2 = mock(BlobContainerItem.class);
        when(c1.getName()).thenReturn("photos");
        when(c2.getName()).thenReturn("documents");

        PagedIterable<BlobContainerItem> paged = mock(PagedIterable.class);
        doReturn(Arrays.asList(c1, c2).iterator()).when(paged).iterator();
        when(blobServiceClient.listBlobContainers()).thenReturn(paged);

        List<String> result = service.listContainers();

        assertEquals(2, result.size());
        assertEquals("photos", result.get(0));
        assertEquals("documents", result.get(1));
    }

    @Test
    @SuppressWarnings("unchecked")
    void listContainers_emptyWhenNone() {
        PagedIterable<BlobContainerItem> paged = mock(PagedIterable.class);
        doReturn(Collections.emptyIterator()).when(paged).iterator();
        when(blobServiceClient.listBlobContainers()).thenReturn(paged);

        assertTrue(service.listContainers().isEmpty());
    }

    @Test
    void createContainer_callsCreateIfNotExists() throws Exception {
        service.createContainer("my-container");

        verify(blobServiceClient).createBlobContainerIfNotExists("my-container");
    }

    @Test
    void deleteContainer_callsDeleteIfExists() throws Exception {
        BlobContainerClient containerClient = mock(BlobContainerClient.class);
        when(blobServiceClient.getBlobContainerClient("old-container")).thenReturn(containerClient);

        service.deleteContainer("old-container");

        verify(containerClient).deleteIfExists();
    }

    @Test
    @SuppressWarnings("unchecked")
    void listBlobs_returnsInfoForEachBlob() throws Exception {
        BlobContainerClient containerClient = mock(BlobContainerClient.class);
        when(blobServiceClient.getBlobContainerClient("files")).thenReturn(containerClient);
        when(containerClient.exists()).thenReturn(true);

        BlobItem blobItem = mock(BlobItem.class);
        BlobItemProperties props = mock(BlobItemProperties.class);
        BlobClient blobClient = mock(BlobClient.class);
        when(blobItem.getName()).thenReturn("readme.txt");
        when(blobItem.getProperties()).thenReturn(props);
        when(props.getContentLength()).thenReturn(1024L);
        when(props.getContentType()).thenReturn("text/plain");
        when(props.getLastModified()).thenReturn(OffsetDateTime.now());

        PagedIterable<BlobItem> pagedBlobs = mock(PagedIterable.class);
        doReturn(Collections.singletonList(blobItem).iterator()).when(pagedBlobs).iterator();
        when(containerClient.listBlobs()).thenReturn(pagedBlobs);
        when(containerClient.getBlobClient("readme.txt")).thenReturn(blobClient);
        when(blobClient.getBlobUrl()).thenReturn("https://store.blob.core.windows.net/files/readme.txt");

        List<BlobInfo> result = service.listBlobs("files");

        assertEquals(1, result.size());
        BlobInfo info = result.get(0);
        assertEquals("readme.txt", info.getName());
        assertEquals(1024L, info.getSize());
        assertEquals("text/plain", info.getContentType());
    }

    @Test
    void listBlobs_throwsIfContainerNotFound() throws Exception {
        BlobContainerClient containerClient = mock(BlobContainerClient.class);
        when(blobServiceClient.getBlobContainerClient("missing")).thenReturn(containerClient);
        when(containerClient.exists()).thenReturn(false);

        assertThrows(IllegalArgumentException.class, () -> service.listBlobs("missing"));
    }

    @Test
    void uploadBlob_uploadsWithContentType() throws Exception {
        BlobContainerClient containerClient = mock(BlobContainerClient.class);
        BlobClient blobClient = mock(BlobClient.class);
        BlockBlobClient blockBlobClient = mock(BlockBlobClient.class);
        when(blobServiceClient.getBlobContainerClient("uploads")).thenReturn(containerClient);
        when(containerClient.getBlobClient("photo.jpg")).thenReturn(blobClient);
        when(blobClient.getBlockBlobClient()).thenReturn(blockBlobClient);

        InputStream data = new ByteArrayInputStream("image-data".getBytes());
        service.uploadBlob("uploads", "photo.jpg", data, 10L, "image/jpeg");

        verify(containerClient).createIfNotExists();
        verify(blockBlobClient).upload(eq(data), eq(10L), eq(true));
        ArgumentCaptor<BlobHttpHeaders> headersCaptor = ArgumentCaptor.forClass(BlobHttpHeaders.class);
        verify(blobClient).setHttpHeaders(headersCaptor.capture());
        assertEquals("image/jpeg", headersCaptor.getValue().getContentType());
    }

    @Test
    void downloadBlob_writesToOutputStream() throws Exception {
        BlobContainerClient containerClient = mock(BlobContainerClient.class);
        BlobClient blobClient = mock(BlobClient.class);
        when(blobServiceClient.getBlobContainerClient("docs")).thenReturn(containerClient);
        when(containerClient.getBlobClient("file.txt")).thenReturn(blobClient);
        when(blobClient.exists()).thenReturn(true);

        doAnswer(invocation -> {
            OutputStream out = invocation.getArgument(0);
            out.write("hello world".getBytes());
            return null;
        }).when(blobClient).downloadStream(any());

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        service.downloadBlob("docs", "file.txt", out);

        assertEquals("hello world", out.toString());
    }

    @Test
    void downloadBlob_throwsIfBlobNotFound() throws Exception {
        BlobContainerClient containerClient = mock(BlobContainerClient.class);
        BlobClient blobClient = mock(BlobClient.class);
        when(blobServiceClient.getBlobContainerClient("docs")).thenReturn(containerClient);
        when(containerClient.getBlobClient("missing.txt")).thenReturn(blobClient);
        when(blobClient.exists()).thenReturn(false);

        assertThrows(IllegalArgumentException.class,
                () -> service.downloadBlob("docs", "missing.txt", new ByteArrayOutputStream()));
    }

    @Test
    void deleteBlob_returnsTrueWhenDeleted() throws Exception {
        BlobContainerClient containerClient = mock(BlobContainerClient.class);
        BlobClient blobClient = mock(BlobClient.class);
        when(blobServiceClient.getBlobContainerClient("data")).thenReturn(containerClient);
        when(containerClient.getBlobClient("old.csv")).thenReturn(blobClient);
        when(blobClient.deleteIfExists()).thenReturn(true);

        assertTrue(service.deleteBlob("data", "old.csv"));
    }

    @Test
    void deleteBlob_returnsFalseWhenNotFound() throws Exception {
        BlobContainerClient containerClient = mock(BlobContainerClient.class);
        BlobClient blobClient = mock(BlobClient.class);
        when(blobServiceClient.getBlobContainerClient("data")).thenReturn(containerClient);
        when(containerClient.getBlobClient("nope.csv")).thenReturn(blobClient);
        when(blobClient.deleteIfExists()).thenReturn(false);

        assertFalse(service.deleteBlob("data", "nope.csv"));
    }

    @Test
    void getBlobInfo_returnsInfo() throws Exception {
        BlobContainerClient containerClient = mock(BlobContainerClient.class);
        BlobClient blobClient = mock(BlobClient.class);
        BlobProperties props = mock(BlobProperties.class);
        when(blobServiceClient.getBlobContainerClient("info")).thenReturn(containerClient);
        when(containerClient.getBlobClient("report.pdf")).thenReturn(blobClient);
        when(blobClient.exists()).thenReturn(true);
        when(blobClient.getProperties()).thenReturn(props);
        when(blobClient.getBlobUrl()).thenReturn("https://store.blob.core.windows.net/info/report.pdf");
        when(props.getBlobSize()).thenReturn(5000L);
        when(props.getContentType()).thenReturn("application/pdf");
        when(props.getLastModified()).thenReturn(null);

        BlobInfo info = service.getBlobInfo("info", "report.pdf");

        assertEquals("report.pdf", info.getName());
        assertEquals(5000L, info.getSize());
        assertEquals("application/pdf", info.getContentType());
    }
}
