package com.example.blobmanager.service;

import com.example.blobmanager.model.BlobInfo;
import com.microsoft.azure.storage.StorageException;
import com.microsoft.azure.storage.blob.BlobProperties;
import com.microsoft.azure.storage.blob.CloudBlobClient;
import com.microsoft.azure.storage.blob.CloudBlobContainer;
import com.microsoft.azure.storage.blob.CloudBlockBlob;
import com.microsoft.azure.storage.blob.ListBlobItem;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BlobStorageServiceTest {

    @Mock
    private CloudBlobClient blobClient;

    private BlobStorageService service;

    @BeforeEach
    void setUp() {
        service = new BlobStorageService();
        service.setBlobClient(blobClient);
    }

    @Test
    void listContainers_returnsContainerNames() {
        CloudBlobContainer c1 = mock(CloudBlobContainer.class);
        CloudBlobContainer c2 = mock(CloudBlobContainer.class);
        when(c1.getName()).thenReturn("photos");
        when(c2.getName()).thenReturn("documents");
        when(blobClient.listContainers()).thenReturn(Arrays.asList(c1, c2));

        List<String> result = service.listContainers();

        assertEquals(2, result.size());
        assertEquals("photos", result.get(0));
        assertEquals("documents", result.get(1));
    }

    @Test
    void listContainers_emptyWhenNone() {
        when(blobClient.listContainers()).thenReturn(Collections.emptyList());
        assertTrue(service.listContainers().isEmpty());
    }

    @Test
    void createContainer_callsCreateIfNotExists() throws Exception {
        CloudBlobContainer container = mock(CloudBlobContainer.class);
        when(blobClient.getContainerReference("my-container")).thenReturn(container);

        service.createContainer("my-container");

        verify(container).createIfNotExists();
    }

    @Test
    void deleteContainer_callsDeleteIfExists() throws Exception {
        CloudBlobContainer container = mock(CloudBlobContainer.class);
        when(blobClient.getContainerReference("old-container")).thenReturn(container);

        service.deleteContainer("old-container");

        verify(container).deleteIfExists();
    }

    @Test
    void listBlobs_returnsInfoForEachBlob() throws Exception {
        CloudBlobContainer container = mock(CloudBlobContainer.class);
        when(blobClient.getContainerReference("files")).thenReturn(container);
        when(container.exists()).thenReturn(true);

        CloudBlockBlob blob = mock(CloudBlockBlob.class);
        BlobProperties props = mock(BlobProperties.class);
        when(blob.getName()).thenReturn("readme.txt");
        when(blob.getUri()).thenReturn(new URI("https://store.blob.core.windows.net/files/readme.txt"));
        when(blob.getProperties()).thenReturn(props);
        when(props.getLength()).thenReturn(1024L);
        when(props.getContentType()).thenReturn("text/plain");
        when(props.getLastModified()).thenReturn(new Date(1700000000000L));

        List<ListBlobItem> items = Collections.singletonList(blob);
        when(container.listBlobs()).thenReturn(items);

        List<BlobInfo> result = service.listBlobs("files");

        assertEquals(1, result.size());
        BlobInfo info = result.get(0);
        assertEquals("readme.txt", info.getName());
        assertEquals(1024L, info.getSize());
        assertEquals("text/plain", info.getContentType());
    }

    @Test
    void listBlobs_throwsIfContainerNotFound() throws Exception {
        CloudBlobContainer container = mock(CloudBlobContainer.class);
        when(blobClient.getContainerReference("missing")).thenReturn(container);
        when(container.exists()).thenReturn(false);

        assertThrows(IllegalArgumentException.class, () -> service.listBlobs("missing"));
    }

    @Test
    void uploadBlob_uploadsWithContentType() throws Exception {
        CloudBlobContainer container = mock(CloudBlobContainer.class);
        CloudBlockBlob blob = mock(CloudBlockBlob.class);
        BlobProperties props = mock(BlobProperties.class);
        when(blobClient.getContainerReference("uploads")).thenReturn(container);
        when(container.getBlockBlobReference("photo.jpg")).thenReturn(blob);
        when(blob.getProperties()).thenReturn(props);

        InputStream data = new ByteArrayInputStream("image-data".getBytes());
        service.uploadBlob("uploads", "photo.jpg", data, 10L, "image/jpeg");

        verify(props).setContentType("image/jpeg");
        verify(blob).upload(eq(data), eq(10L));
        verify(container).createIfNotExists();
    }

    @Test
    void downloadBlob_writesToOutputStream() throws Exception {
        CloudBlobContainer container = mock(CloudBlobContainer.class);
        CloudBlockBlob blob = mock(CloudBlockBlob.class);
        when(blobClient.getContainerReference("docs")).thenReturn(container);
        when(container.getBlockBlobReference("file.txt")).thenReturn(blob);
        when(blob.exists()).thenReturn(true);

        doAnswer(invocation -> {
            ByteArrayOutputStream out = invocation.getArgument(0);
            out.write("hello world".getBytes());
            return null;
        }).when(blob).download(org.mockito.ArgumentMatchers.any());

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        service.downloadBlob("docs", "file.txt", out);

        assertEquals("hello world", out.toString());
    }

    @Test
    void downloadBlob_throwsIfBlobNotFound() throws Exception {
        CloudBlobContainer container = mock(CloudBlobContainer.class);
        CloudBlockBlob blob = mock(CloudBlockBlob.class);
        when(blobClient.getContainerReference("docs")).thenReturn(container);
        when(container.getBlockBlobReference("missing.txt")).thenReturn(blob);
        when(blob.exists()).thenReturn(false);

        assertThrows(IllegalArgumentException.class,
                () -> service.downloadBlob("docs", "missing.txt", new ByteArrayOutputStream()));
    }

    @Test
    void deleteBlob_returnsTrueWhenDeleted() throws Exception {
        CloudBlobContainer container = mock(CloudBlobContainer.class);
        CloudBlockBlob blob = mock(CloudBlockBlob.class);
        when(blobClient.getContainerReference("data")).thenReturn(container);
        when(container.getBlockBlobReference("old.csv")).thenReturn(blob);
        when(blob.deleteIfExists()).thenReturn(true);

        assertTrue(service.deleteBlob("data", "old.csv"));
    }

    @Test
    void deleteBlob_returnsFalseWhenNotFound() throws Exception {
        CloudBlobContainer container = mock(CloudBlobContainer.class);
        CloudBlockBlob blob = mock(CloudBlockBlob.class);
        when(blobClient.getContainerReference("data")).thenReturn(container);
        when(container.getBlockBlobReference("nope.csv")).thenReturn(blob);
        when(blob.deleteIfExists()).thenReturn(false);

        assertFalse(service.deleteBlob("data", "nope.csv"));
    }

    @Test
    void getBlobInfo_returnsInfo() throws Exception {
        CloudBlobContainer container = mock(CloudBlobContainer.class);
        CloudBlockBlob blob = mock(CloudBlockBlob.class);
        BlobProperties props = mock(BlobProperties.class);
        when(blobClient.getContainerReference("info")).thenReturn(container);
        when(container.getBlockBlobReference("report.pdf")).thenReturn(blob);
        when(blob.exists()).thenReturn(true);
        when(blob.getProperties()).thenReturn(props);
        when(blob.getName()).thenReturn("report.pdf");
        when(blob.getUri()).thenReturn(new URI("https://store.blob.core.windows.net/info/report.pdf"));
        when(props.getLength()).thenReturn(5000L);
        when(props.getContentType()).thenReturn("application/pdf");
        when(props.getLastModified()).thenReturn(null);

        BlobInfo info = service.getBlobInfo("info", "report.pdf");

        assertEquals("report.pdf", info.getName());
        assertEquals(5000L, info.getSize());
        assertEquals("application/pdf", info.getContentType());
    }
}
