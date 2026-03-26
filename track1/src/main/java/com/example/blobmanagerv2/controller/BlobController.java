package com.example.blobmanagerv2.controller;

import com.example.blobmanagerv2.model.BlobInfo;
import com.example.blobmanagerv2.service.BlobStorageService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/containers")
public class BlobController {

    private final BlobStorageService storageService;

    public BlobController(BlobStorageService storageService) {
        this.storageService = storageService;
    }

    @GetMapping
    public ResponseEntity<List<String>> listContainers() {
        return ResponseEntity.ok(storageService.listContainers());
    }

    @PostMapping("/{containerName}")
    public ResponseEntity<Map<String, String>> createContainer(@PathVariable String containerName) {
        try {
            storageService.createContainer(containerName);
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(Map.of("message", "Container created: " + containerName));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    @DeleteMapping("/{containerName}")
    public ResponseEntity<Void> deleteContainer(@PathVariable String containerName) {
        try {
            storageService.deleteContainer(containerName);
            return ResponseEntity.noContent().build();
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/{containerName}/blobs")
    public ResponseEntity<?> listBlobs(@PathVariable String containerName) {
        try {
            List<BlobInfo> blobs = storageService.listBlobs(containerName);
            return ResponseEntity.ok(blobs);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/{containerName}/blobs")
    public ResponseEntity<?> uploadBlob(
            @PathVariable String containerName,
            @RequestParam("file") MultipartFile file) {
        try {
            String blobName = file.getOriginalFilename();
            storageService.uploadBlob(
                    containerName,
                    blobName,
                    file.getInputStream(),
                    file.getSize(),
                    file.getContentType()
            );
            BlobInfo info = storageService.getBlobInfo(containerName, blobName);
            return ResponseEntity.status(HttpStatus.CREATED).body(info);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/{containerName}/blobs/{blobName}")
    public ResponseEntity<StreamingResponseBody> downloadBlob(
            @PathVariable String containerName,
            @PathVariable String blobName) {
        try {
            BlobInfo info = storageService.getBlobInfo(containerName, blobName);
            StreamingResponseBody responseBody = outputStream -> {
                try {
                    storageService.downloadBlob(containerName, blobName, outputStream);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            };

            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + blobName + "\"")
                    .contentType(MediaType.parseMediaType(
                            info.getContentType() != null ? info.getContentType() : "application/octet-stream"))
                    .contentLength(info.getSize())
                    .body(responseBody);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @DeleteMapping("/{containerName}/blobs/{blobName}")
    public ResponseEntity<?> deleteBlob(
            @PathVariable String containerName,
            @PathVariable String blobName) {
        try {
            boolean deleted = storageService.deleteBlob(containerName, blobName);
            if (deleted) {
                return ResponseEntity.noContent().build();
            }
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", "Blob not found: " + blobName));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", e.getMessage()));
        }
    }
}
