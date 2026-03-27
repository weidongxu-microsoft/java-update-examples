package com.example.blobmanagerv2.service;

import com.azure.v2.storage.blob.BlobClient;
import com.azure.v2.storage.blob.BlockBlobClient;
import com.azure.v2.storage.blob.ContainerClient;
import com.azure.v2.storage.blob.StorageServiceClient;
import com.azure.v2.storage.blob.models.BlobContainerItem;
import com.azure.v2.storage.blob.models.BlobFlatListSegment;
import com.azure.v2.storage.blob.models.BlobHttpHeaders;
import com.azure.v2.storage.blob.models.BlobItemInternal;
import com.azure.v2.storage.blob.models.BlobItemPropertiesInternal;
import com.azure.v2.storage.blob.models.BlobName;
import com.azure.v2.storage.blob.models.ListBlobsFlatSegmentResponse;
import com.example.blobmanagerv2.model.BlobInfo;
import io.clientcore.core.http.models.HttpHeaderName;
import io.clientcore.core.http.models.HttpHeaders;
import io.clientcore.core.http.models.HttpRequest;
import io.clientcore.core.http.models.HttpResponseException;
import io.clientcore.core.http.models.Response;
import io.clientcore.core.http.paging.PagedIterable;
import io.clientcore.core.http.paging.PagedResponse;
import io.clientcore.core.models.binarydata.BinaryData;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Date;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BlobStorageServiceTest {

    @Mock
    private StorageServiceClient storageServiceClient;

    @Mock
    private ContainerClient containerClient;

    @Mock
    private BlobClient blobClient;

    @Mock
    private BlockBlobClient blockBlobClient;

    private BlobStorageService service;

    @BeforeEach
    void setUp() {
        service = new BlobStorageService();
        service.setClients(storageServiceClient, containerClient, blobClient, blockBlobClient,
                "https://store.blob.core.windows.net");
    }

    @Test
    void listContainers_returnsContainerNames() {
        when(storageServiceClient.listBlobContainersSegment(null, null, null, null, null, null))
                .thenReturn(new PagedIterable<>(ignored -> new PagedResponse<>(request(), 200, new HttpHeaders(), List.of(
                        new BlobContainerItem().setName("photos"),
                        new BlobContainerItem().setName("documents")))));

        List<String> result = service.listContainers();

        assertEquals(2, result.size());
        assertEquals("photos", result.get(0));
        assertEquals("documents", result.get(1));
    }

    @Test
    void listContainers_emptyWhenNone() {
        when(storageServiceClient.listBlobContainersSegment(null, null, null, null, null, null))
                .thenReturn(new PagedIterable<>(ignored -> new PagedResponse<>(request(), 200, new HttpHeaders(), List.of())));
        assertTrue(service.listContainers().isEmpty());
    }

    @Test
    void createContainer_callsCreate() {
        service.createContainer("my-container");

        verify(containerClient).create("my-container", null, null, null, null, null);
    }

    @Test
    void createContainer_ignoresAlreadyExists() {
        doThrow(httpResponseException(409)).when(containerClient)
                .create("my-container", null, null, null, null, null);

        service.createContainer("my-container");
    }

    @Test
    void deleteContainer_callsDelete() {
        service.deleteContainer("old-container");

        verify(containerClient).delete("old-container", null, null, null, null, null);
    }

    @Test
    void deleteContainer_ignoresMissingContainer() {
        doThrow(httpResponseException(404)).when(containerClient)
                .delete("old-container", null, null, null, null, null);

        service.deleteContainer("old-container");
    }

    @Test
    void listBlobs_returnsInfoForEachBlob() {
        when(containerClient.getPropertiesWithResponse(eq("files"), isNull(), isNull(), isNull(), any()))
                .thenReturn(emptyResponse(200));
        when(containerClient.listBlobFlatSegment("files", null, null, null, null, null, null))
            .thenReturn(new ListBlobsFlatSegmentResponse()
                .setSegment(new BlobFlatListSegment()
                    .setBlobItems(List.of(
                        new BlobItemInternal()
                            .setName(new BlobName().setContent("readme.txt"))
                            .setProperties(new BlobItemPropertiesInternal()
                                .setContentLength(1024L)
                                .setContentType("text/plain")
                                .setLastModified(OffsetDateTime.ofInstant(
                                    new Date(1700000000000L).toInstant(), ZoneOffset.UTC)))))));

        List<BlobInfo> result = service.listBlobs("files");

        assertEquals(1, result.size());
        BlobInfo info = result.get(0);
        assertEquals("readme.txt", info.getName());
        assertEquals("https://store.blob.core.windows.net/files/readme.txt", info.getUrl());
        assertEquals(1024L, info.getSize());
        assertEquals("text/plain", info.getContentType());
    }

    @Test
    void listBlobs_throwsIfContainerNotFound() {
        when(containerClient.getPropertiesWithResponse(eq("missing"), isNull(), isNull(), isNull(), any()))
                .thenThrow(httpResponseException(404));

        assertThrows(IllegalArgumentException.class, () -> service.listBlobs("missing"));
    }

    @Test
    void uploadBlob_uploadsWithContentType() throws Exception {
        InputStream data = new ByteArrayInputStream("image-data".getBytes());
        service.uploadBlob("uploads", "photo.jpg", data, 10L, "image/jpeg");

        verify(containerClient).create("uploads", null, null, null, null, null);
        verify(blockBlobClient).upload(eq("uploads"), eq("photo.jpg"), eq(10L), any(BinaryData.class), isNull(),
                isNull(), isNull(), isNull(), isNull(), isNull(), isNull(), isNull(), isNull(), isNull(), isNull(),
                isNull(), isNull(), isNull(), isNull(), isNull(), isNull(), isNull(),
                argThat((BlobHttpHeaders headers) -> "image/jpeg".equals(headers.getContentType())), isNull(),
                isNull());
    }

    @Test
    void downloadBlob_writesToOutputStream() throws Exception {
        when(blobClient.getPropertiesWithResponse(eq("docs"), eq("file.txt"), isNull(), isNull(), isNull(), isNull(),
                isNull(), isNull(), isNull(), isNull(), isNull(), isNull(), isNull(), any()))
                .thenReturn(emptyResponse(200));
        when(blobClient.download("docs", "file.txt", null, null, null, null, null, null, null, null, null, null,
                null, null, null, null, null))
                .thenReturn(new ByteArrayInputStream("hello world".getBytes()));

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        service.downloadBlob("docs", "file.txt", out);

        assertEquals("hello world", out.toString());
    }

    @Test
    void downloadBlob_throwsIfBlobNotFound() {
        when(blobClient.getPropertiesWithResponse(eq("docs"), eq("missing.txt"), isNull(), isNull(), isNull(),
                isNull(), isNull(), isNull(), isNull(), isNull(), isNull(), isNull(), isNull(), any()))
                .thenThrow(httpResponseException(404));

        assertThrows(IllegalArgumentException.class,
                () -> service.downloadBlob("docs", "missing.txt", new ByteArrayOutputStream()));
    }

    @Test
    void deleteBlob_returnsTrueWhenDeleted() {
        assertTrue(service.deleteBlob("data", "old.csv"));
    }

    @Test
    void deleteBlob_returnsFalseWhenNotFound() {
        doThrow(httpResponseException(404)).when(blobClient)
                .delete("data", "nope.csv", null, null, null, null, null, null, null, null, null, null, null, null);

        assertFalse(service.deleteBlob("data", "nope.csv"));
    }

    @Test
    void getBlobInfo_returnsInfo() {
        when(blobClient.getPropertiesWithResponse(eq("info"), eq("report.pdf"), isNull(), isNull(), isNull(),
                isNull(), isNull(), isNull(), isNull(), isNull(), isNull(), isNull(), isNull(), any()))
                .thenReturn(blobInfoResponse(5000L, "application/pdf", null));

        BlobInfo info = service.getBlobInfo("info", "report.pdf");

        assertEquals("report.pdf", info.getName());
        assertEquals("https://store.blob.core.windows.net/info/report.pdf", info.getUrl());
        assertEquals(5000L, info.getSize());
        assertEquals("application/pdf", info.getContentType());
    }

    @Test
    void getBlobInfo_throwsIfMissing() {
        when(blobClient.getPropertiesWithResponse(eq("info"), eq("missing.pdf"), isNull(), isNull(), isNull(),
                isNull(), isNull(), isNull(), isNull(), isNull(), isNull(), isNull(), isNull(), any()))
                .thenThrow(httpResponseException(404));

        assertThrows(IllegalArgumentException.class, () -> service.getBlobInfo("info", "missing.pdf"));
    }

    private static HttpRequest request() {
        return new HttpRequest().setUri("https://example.test");
    }

    private static Response<Void> emptyResponse(int statusCode) {
        return new Response<>(request(), statusCode, new HttpHeaders(), null);
    }

    private static Response<Void> blobInfoResponse(long size, String contentType, String lastModified) {
        HttpHeaders headers = new HttpHeaders()
                .set(HttpHeaderName.CONTENT_LENGTH, String.valueOf(size))
                .set(HttpHeaderName.CONTENT_TYPE, contentType);
        if (lastModified != null) {
            headers.set(HttpHeaderName.LAST_MODIFIED, lastModified);
        }
        return new Response<>(request(), 200, headers, null);
    }

    private static HttpResponseException httpResponseException(int statusCode) {
        return new HttpResponseException("storage error",
                new Response<>(request(), statusCode, new HttpHeaders(), (BinaryData) null),
                (Object) null);
    }
}
