package com.example.blobmanagerv2.model;

import java.time.OffsetDateTime;

public class BlobInfo {

    private String name;
    private String url;
    private long size;
    private String contentType;
    private OffsetDateTime lastModified;

    public BlobInfo() {
    }

    public BlobInfo(String name, String url, long size, String contentType, OffsetDateTime lastModified) {
        this.name = name;
        this.url = url;
        this.size = size;
        this.contentType = contentType;
        this.lastModified = lastModified;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public long getSize() {
        return size;
    }

    public void setSize(long size) {
        this.size = size;
    }

    public String getContentType() {
        return contentType;
    }

    public void setContentType(String contentType) {
        this.contentType = contentType;
    }

    public OffsetDateTime getLastModified() {
        return lastModified;
    }

    public void setLastModified(OffsetDateTime lastModified) {
        this.lastModified = lastModified;
    }
}
