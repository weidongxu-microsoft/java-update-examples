package com.example.blobmanager.controller;

import com.example.blobmanager.model.BlobInfo;
import com.example.blobmanager.service.BlobStorageService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;

import java.io.OutputStream;
import java.time.OffsetDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(BlobController.class)
class BlobControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private BlobStorageService storageService;

    @Test
    void listContainers_returnsOk() throws Exception {
        when(storageService.listContainers()).thenReturn(Arrays.asList("photos", "docs"));

        mockMvc.perform(get("/api/containers"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0]").value("photos"))
                .andExpect(jsonPath("$[1]").value("docs"));
    }

    @Test
    void createContainer_returns201() throws Exception {
        mockMvc.perform(post("/api/containers/my-files"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.message").value("Container created: my-files"));

        verify(storageService).createContainer("my-files");
    }

    @Test
    void deleteContainer_returns204() throws Exception {
        mockMvc.perform(delete("/api/containers/old-stuff"))
                .andExpect(status().isNoContent());

        verify(storageService).deleteContainer("old-stuff");
    }

    @Test
    void listBlobs_returnsBlobs() throws Exception {
        BlobInfo blob = new BlobInfo("readme.txt", "https://store/readme.txt", 1024,
                "text/plain", OffsetDateTime.now());
        when(storageService.listBlobs("docs")).thenReturn(List.of(blob));

        mockMvc.perform(get("/api/containers/docs/blobs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("readme.txt"))
                .andExpect(jsonPath("$[0].size").value(1024));
    }

    @Test
    void listBlobs_returns404ForMissingContainer() throws Exception {
        when(storageService.listBlobs("missing"))
                .thenThrow(new IllegalArgumentException("Container not found: missing"));

        mockMvc.perform(get("/api/containers/missing/blobs"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("Container not found: missing"));
    }

    @Test
    void uploadBlob_returns201() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file", "photo.jpg", "image/jpeg", "fake-image".getBytes());

        BlobInfo info = new BlobInfo("photo.jpg", "https://store/photo.jpg", 10,
                "image/jpeg", OffsetDateTime.now());
        when(storageService.getBlobInfo("uploads", "photo.jpg")).thenReturn(info);

        mockMvc.perform(multipart("/api/containers/uploads/blobs").file(file))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("photo.jpg"));
    }

    @Test
    void downloadBlob_returnsFile() throws Exception {
        BlobInfo info = new BlobInfo("file.txt", "https://store/file.txt", 11,
                "text/plain", OffsetDateTime.now());
        when(storageService.getBlobInfo("docs", "file.txt")).thenReturn(info);

        doAnswer(invocation -> {
            OutputStream out = invocation.getArgument(2);
            out.write("hello world".getBytes());
            return null;
        }).when(storageService).downloadBlob(eq("docs"), eq("file.txt"), any());

        mockMvc.perform(get("/api/containers/docs/blobs/file.txt"))
                .andExpect(status().isOk())
                .andExpect(content().string("hello world"));
    }

    @Test
    void downloadBlob_returns404WhenNotFound() throws Exception {
        when(storageService.getBlobInfo("docs", "missing.txt"))
                .thenThrow(new IllegalArgumentException("Blob not found"));

        mockMvc.perform(get("/api/containers/docs/blobs/missing.txt"))
                .andExpect(status().isNotFound());
    }

    @Test
    void deleteBlob_returns204WhenDeleted() throws Exception {
        when(storageService.deleteBlob("data", "old.csv")).thenReturn(true);

        mockMvc.perform(delete("/api/containers/data/blobs/old.csv"))
                .andExpect(status().isNoContent());
    }

    @Test
    void deleteBlob_returns404WhenNotFound() throws Exception {
        when(storageService.deleteBlob("data", "nope.csv")).thenReturn(false);

        mockMvc.perform(delete("/api/containers/data/blobs/nope.csv"))
                .andExpect(status().isNotFound());
    }
}
