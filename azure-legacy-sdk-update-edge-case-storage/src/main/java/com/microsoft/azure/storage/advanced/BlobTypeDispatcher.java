package com.microsoft.azure.storage.advanced;

import com.microsoft.azure.storage.StorageException;
import com.microsoft.azure.storage.blob.CloudAppendBlob;
import com.microsoft.azure.storage.blob.CloudBlobContainer;
import com.microsoft.azure.storage.blob.CloudBlobDirectory;
import com.microsoft.azure.storage.blob.CloudBlockBlob;
import com.microsoft.azure.storage.blob.CloudPageBlob;
import com.microsoft.azure.storage.blob.ListBlobItem;

import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Function;

public class BlobTypeDispatcher {

    private final CloudBlobContainer container;

    public BlobTypeDispatcher(CloudBlobContainer container) {
        this.container = container;
    }

    public String classifyBlob(ListBlobItem item) {
        if (item instanceof CloudBlockBlob) {
            CloudBlockBlob block = (CloudBlockBlob) item;
            return "block:" + block.getName() + ":" + block.getProperties().getLength();
        } else if (item instanceof CloudPageBlob) {
            CloudPageBlob page = (CloudPageBlob) item;
            return "page:" + page.getName() + ":" + page.getProperties().getLength();
        } else if (item instanceof CloudAppendBlob) {
            CloudAppendBlob append = (CloudAppendBlob) item;
            return "append:" + append.getName() + ":" + append.getProperties().getLength();
        } else if (item instanceof CloudBlobDirectory) {
            CloudBlobDirectory directory = (CloudBlobDirectory) item;
            try {
                return "directory:" + directory.getPrefix();
            } catch (Exception e) {
                return "directory:unknown";
            }
        }
        return "unknown";
    }

    public Map<String, List<String>> groupBlobsByType()
            throws StorageException, URISyntaxException {
        Map<String, List<String>> grouped = new LinkedHashMap<>();
        grouped.put("block", new ArrayList<>());
        grouped.put("page", new ArrayList<>());
        grouped.put("append", new ArrayList<>());
        grouped.put("directory", new ArrayList<>());

        for (ListBlobItem item : container.listBlobs()) {
            if (item instanceof CloudBlockBlob) {
                grouped.get("block").add(((CloudBlockBlob) item).getName());
            } else if (item instanceof CloudPageBlob) {
                grouped.get("page").add(((CloudPageBlob) item).getName());
            } else if (item instanceof CloudAppendBlob) {
                grouped.get("append").add(((CloudAppendBlob) item).getName());
            } else if (item instanceof CloudBlobDirectory) {
                try {
                    grouped.get("directory").add(((CloudBlobDirectory) item).getPrefix());
                } catch (Exception e) {
                    grouped.get("directory").add("unknown");
                }
            }
        }
        return grouped;
    }

    public void forEachBlockBlob(BlobProcessor processor)
            throws StorageException, URISyntaxException {
        for (ListBlobItem item : container.listBlobs()) {
            if (item instanceof CloudBlockBlob) {
                processor.process((CloudBlockBlob) item);
            }
        }
    }

    public <R> List<R> mapBlockBlobs(Function<CloudBlockBlob, R> mapper)
            throws StorageException, URISyntaxException {
        List<R> results = new ArrayList<>();
        for (ListBlobItem item : container.listBlobs()) {
            if (item instanceof CloudBlockBlob) {
                results.add(mapper.apply((CloudBlockBlob) item));
            }
        }
        return results;
    }

    public void processWithCallback(Consumer<CloudBlockBlob> onBlock,
                                     Consumer<CloudPageBlob> onPage,
                                     Consumer<CloudAppendBlob> onAppend)
            throws StorageException, URISyntaxException {
        for (ListBlobItem item : container.listBlobs()) {
            if (item instanceof CloudBlockBlob) {
                onBlock.accept((CloudBlockBlob) item);
            } else if (item instanceof CloudPageBlob) {
                onPage.accept((CloudPageBlob) item);
            } else if (item instanceof CloudAppendBlob) {
                onAppend.accept((CloudAppendBlob) item);
            }
        }
    }
}
