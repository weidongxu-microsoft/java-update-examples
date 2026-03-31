package com.microsoft.azure.storage.advanced;

import com.microsoft.azure.storage.StorageException;
import com.microsoft.azure.storage.blob.BlobProperties;
import com.microsoft.azure.storage.blob.CloudAppendBlob;
import com.microsoft.azure.storage.blob.CloudBlobContainer;
import com.microsoft.azure.storage.blob.CloudBlobDirectory;
import com.microsoft.azure.storage.blob.CloudBlockBlob;
import com.microsoft.azure.storage.blob.CloudPageBlob;
import com.microsoft.azure.storage.blob.ListBlobItem;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.function.Function;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class BlobTypeDispatcherTest {

    @Mock private CloudBlobContainer mockContainer;
    @Mock private CloudBlockBlob mockBlockBlob;
    @Mock private CloudPageBlob mockPageBlob;
    @Mock private CloudAppendBlob mockAppendBlob;
    @Mock private CloudBlobDirectory mockDirectory;
    @Mock private BlobProperties mockBlockProperties;
    @Mock private BlobProperties mockPageProperties;
    @Mock private BlobProperties mockAppendProperties;

    private BlobTypeDispatcher dispatcher;

    @Before
    public void setUp() {
        dispatcher = new BlobTypeDispatcher(mockContainer);
    }

    @Test
    public void testClassifyBlockBlob() {
        when(mockBlockBlob.getName()).thenReturn("data.txt");
        when(mockBlockBlob.getProperties()).thenReturn(mockBlockProperties);
        when(mockBlockProperties.getLength()).thenReturn(512L);

        String result = dispatcher.classifyBlob(mockBlockBlob);
        assertEquals("block:data.txt:512", result);
    }

    @Test
    public void testClassifyPageBlob() {
        when(mockPageBlob.getName()).thenReturn("disk.vhd");
        when(mockPageBlob.getProperties()).thenReturn(mockPageProperties);
        when(mockPageProperties.getLength()).thenReturn(1073741824L);

        String result = dispatcher.classifyBlob(mockPageBlob);
        assertEquals("page:disk.vhd:1073741824", result);
    }

    @Test
    public void testClassifyAppendBlob() {
        when(mockAppendBlob.getName()).thenReturn("log.txt");
        when(mockAppendBlob.getProperties()).thenReturn(mockAppendProperties);
        when(mockAppendProperties.getLength()).thenReturn(2048L);

        String result = dispatcher.classifyBlob(mockAppendBlob);
        assertEquals("append:log.txt:2048", result);
    }

    @Test
    public void testClassifyDirectory() throws URISyntaxException {
        when(mockDirectory.getPrefix()).thenReturn("subdir/");

        String result = dispatcher.classifyBlob(mockDirectory);
        assertEquals("directory:subdir/", result);
    }

    @Test
    public void testClassifyUnknownItem() {
        ListBlobItem unknownItem = mock(ListBlobItem.class);
        String result = dispatcher.classifyBlob(unknownItem);
        assertEquals("unknown", result);
    }

    @Test
    public void testGroupBlobsByType() throws StorageException, URISyntaxException {
        when(mockBlockBlob.getName()).thenReturn("file1.txt");
        when(mockAppendBlob.getName()).thenReturn("log.txt");
        when(mockDirectory.getPrefix()).thenReturn("folder/");

        List<ListBlobItem> items = Arrays.asList(mockBlockBlob, mockAppendBlob, mockDirectory);
        when(mockContainer.listBlobs()).thenReturn((Iterable) items);

        Map<String, List<String>> grouped = dispatcher.groupBlobsByType();
        assertEquals(1, grouped.get("block").size());
        assertEquals("file1.txt", grouped.get("block").get(0));
        assertEquals(1, grouped.get("append").size());
        assertEquals("log.txt", grouped.get("append").get(0));
        assertEquals(1, grouped.get("directory").size());
        assertEquals("folder/", grouped.get("directory").get(0));
        assertEquals(0, grouped.get("page").size());
    }

    @Test
    public void testForEachBlockBlob() throws StorageException, URISyntaxException {
        List<ListBlobItem> items = Arrays.asList(
                mockBlockBlob, mockPageBlob, mockAppendBlob);
        when(mockContainer.listBlobs()).thenReturn((Iterable) items);

        AtomicInteger count = new AtomicInteger(0);
        BlobProcessor processor = blob -> count.incrementAndGet();
        dispatcher.forEachBlockBlob(processor);
        assertEquals(1, count.get());
    }

    @Test
    public void testMapBlockBlobs() throws StorageException, URISyntaxException {
        when(mockBlockBlob.getName()).thenReturn("test.txt");
        List<ListBlobItem> items = Arrays.asList(mockBlockBlob, mockPageBlob);
        when(mockContainer.listBlobs()).thenReturn((Iterable) items);

        Function<CloudBlockBlob, String> mapper = CloudBlockBlob::getName;
        List<String> names = dispatcher.mapBlockBlobs(mapper);
        assertEquals(1, names.size());
        assertEquals("test.txt", names.get(0));
    }

    @Test
    public void testProcessWithCallback() throws StorageException, URISyntaxException {
        List<ListBlobItem> items = Arrays.asList(
                mockBlockBlob, mockPageBlob, mockAppendBlob);
        when(mockContainer.listBlobs()).thenReturn((Iterable) items);

        List<String> blockResults = new ArrayList<>();
        List<String> pageResults = new ArrayList<>();
        List<String> appendResults = new ArrayList<>();

        when(mockBlockBlob.getName()).thenReturn("block.txt");
        when(mockPageBlob.getName()).thenReturn("page.vhd");
        when(mockAppendBlob.getName()).thenReturn("append.log");

        Consumer<CloudBlockBlob> onBlock = b -> blockResults.add(b.getName());
        Consumer<CloudPageBlob> onPage = p -> pageResults.add(p.getName());
        Consumer<CloudAppendBlob> onAppend = a -> appendResults.add(a.getName());

        dispatcher.processWithCallback(onBlock, onPage, onAppend);

        assertEquals(1, blockResults.size());
        assertEquals("block.txt", blockResults.get(0));
        assertEquals(1, pageResults.size());
        assertEquals("page.vhd", pageResults.get(0));
        assertEquals(1, appendResults.size());
        assertEquals("append.log", appendResults.get(0));
    }
}
