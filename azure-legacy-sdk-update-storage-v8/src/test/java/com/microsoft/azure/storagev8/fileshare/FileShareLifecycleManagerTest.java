package com.microsoft.azure.storagev8.fileshare;

import com.microsoft.azure.storage.StorageException;
import com.microsoft.azure.storage.file.CloudFile;
import com.microsoft.azure.storage.file.CloudFileClient;
import com.microsoft.azure.storage.file.CloudFileDirectory;
import com.microsoft.azure.storage.file.CloudFileShare;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.io.IOException;
import java.net.URISyntaxException;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertSame;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class FileShareLifecycleManagerTest {

    @Mock
    private CloudFileClient fileClient;

    @Mock
    private CloudFileShare share;

    @Mock
    private CloudFileDirectory rootDirectory;

    @Mock
    private CloudFile cloudFile;

    private FileShareLifecycleManager manager;

    @Before
    public void setUp() throws Exception {
        manager = new FileShareLifecycleManager(fileClient);
        when(fileClient.getShareReference(anyString())).thenReturn(share);
        when(share.getRootDirectoryReference()).thenReturn(rootDirectory);
        when(rootDirectory.getFileReference(anyString())).thenReturn(cloudFile);
    }

    @Test
    public void ensureShareCreatesShareWhenMissing() throws StorageException, URISyntaxException {
        CloudFileShare actual = manager.ensureShare("documents-share");

        verify(share).createIfNotExists();
        assertSame(share, actual);
    }

    @Test
    public void uploadTextFileWritesContent() throws StorageException, URISyntaxException, IOException {
        manager.uploadTextFile("documents-share", "report.txt", "payload");

        verify(cloudFile).uploadText("payload");
    }

    @Test
    public void downloadTextFileReturnsContent() throws StorageException, URISyntaxException, IOException {
        when(cloudFile.downloadText()).thenReturn("payload");

        String result = manager.downloadTextFile("documents-share", "report.txt");

        assertThat(result, is("payload"));
    }

    @Test
    public void deleteFileRemovesEntry() throws StorageException, URISyntaxException {
        when(cloudFile.deleteIfExists()).thenReturn(true);

        boolean deleted = manager.deleteFile("documents-share", "report.txt");

        assertThat(deleted, is(true));
        verify(cloudFile).deleteIfExists();
    }
}
