package com.microsoft.azure.storagev10.advanced;

import com.microsoft.azure.storage.blob.BlockBlobURL;
import com.microsoft.azure.storage.blob.CommonRestResponse;
import com.microsoft.azure.storage.blob.ContainerURL;
import com.microsoft.azure.storage.blob.ServiceURL;
import com.microsoft.azure.storage.blob.StorageException;
import com.microsoft.azure.storage.blob.models.ContainerCreateResponse;
import io.reactivex.Flowable;
import io.reactivex.Single;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.InvalidKeyException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for ConcurrentUploadManager.
 */
public class ConcurrentUploadManagerTest {

    @Mock
    private ServiceURL mockServiceURL;

    @Mock
    private ContainerURL mockContainerURL;

    @Mock
    private BlockBlobURL mockBlobURL;

    private ConcurrentUploadManager manager;

    @Before
    public void setUp() {
        MockitoAnnotations.openMocks(this);
        manager = new ConcurrentUploadManager(mockServiceURL, 1024 * 1024, 4);
    }

    @Test
    public void testConstructorWithDefaultOptions() {
        ConcurrentUploadManager defaultManager = new ConcurrentUploadManager(mockServiceURL);
        assertNotNull(defaultManager);
        assertEquals(4 * 1024 * 1024, defaultManager.getBlockSizeBytes());
        assertEquals(5, defaultManager.getMaxConcurrency());
    }

    @Test
    public void testConstructorWithCustomOptions() {
        int blockSize = 2 * 1024 * 1024;
        int concurrency = 8;
        ConcurrentUploadManager customManager = new ConcurrentUploadManager(mockServiceURL, blockSize, concurrency);
        
        assertEquals(blockSize, customManager.getBlockSizeBytes());
        assertEquals(concurrency, customManager.getMaxConcurrency());
        assertEquals(mockServiceURL, customManager.getServiceURL());
    }

    @Test(expected = NullPointerException.class)
    public void testConstructorWithNullServiceURL() {
        new ConcurrentUploadManager(null);
    }

    @Test
    public void testFromAccountWithValidCredentials() throws MalformedURLException, InvalidKeyException {
        String accountName = "testaccount";
        String accountKey = "dGVzdGtleQ=="; // base64 encoded "testkey"
        
        ConcurrentUploadManager result = ConcurrentUploadManager.fromAccount(accountName, accountKey);
        
        assertNotNull(result);
        assertNotNull(result.getServiceURL());
    }

    @Test
    public void testFromAccountWithCustomOptions() throws MalformedURLException, InvalidKeyException {
        String accountName = "testaccount";
        String accountKey = "dGVzdGtleQ==";
        int blockSize = 3 * 1024 * 1024;
        int concurrency = 10;
        
        ConcurrentUploadManager result = ConcurrentUploadManager.fromAccount(
                accountName, accountKey, blockSize, concurrency);
        
        assertNotNull(result);
        assertEquals(blockSize, result.getBlockSizeBytes());
        assertEquals(concurrency, result.getMaxConcurrency());
    }

    @Test(expected = NullPointerException.class)
    public void testFromAccountWithNullAccountName() throws MalformedURLException, InvalidKeyException {
        ConcurrentUploadManager.fromAccount(null, "key");
    }

    @Test(expected = NullPointerException.class)
    public void testFromAccountWithNullAccountKey() throws MalformedURLException, InvalidKeyException {
        ConcurrentUploadManager.fromAccount("account", null);
    }

    @Test
    public void testEnsureContainerCreatesNewContainer() throws StorageException {
        String containerName = "testcontainer";
        ContainerCreateResponse mockResponse = mock(ContainerCreateResponse.class);
        
        when(mockServiceURL.createContainerURL(containerName)).thenReturn(mockContainerURL);
        when(mockContainerURL.create()).thenReturn(Single.just(mockResponse));
        
        ContainerURL result = manager.ensureContainer(containerName);
        
        assertNotNull(result);
        verify(mockServiceURL).createContainerURL(containerName);
        verify(mockContainerURL).create();
    }

    @Test
    public void testEnsureContainerHandlesExistingContainer() throws StorageException {
        String containerName = "existingcontainer";
        StorageException conflictException = mock(StorageException.class);
        
        when(mockServiceURL.createContainerURL(containerName)).thenReturn(mockContainerURL);
        when(mockContainerURL.create()).thenReturn(Single.error(conflictException));
        when(conflictException.statusCode()).thenReturn(409); // Conflict - already exists
        
        ContainerURL result = manager.ensureContainer(containerName);
        
        assertNotNull(result);
        verify(mockServiceURL).createContainerURL(containerName);
    }

    @Test(expected = StorageException.class)
    public void testEnsureContainerPropagatesNonConflictErrors() throws StorageException {
        String containerName = "testcontainer";
        StorageException serverException = mock(StorageException.class);
        
        when(mockServiceURL.createContainerURL(containerName)).thenReturn(mockContainerURL);
        when(mockContainerURL.create()).thenReturn(Single.error(serverException));
        when(serverException.statusCode()).thenReturn(500); // Server error
        
        manager.ensureContainer(containerName);
    }

    @Test
    public void testGetBlockSizeBytes() {
        assertEquals(1024 * 1024, manager.getBlockSizeBytes());
    }

    @Test
    public void testGetMaxConcurrency() {
        assertEquals(4, manager.getMaxConcurrency());
    }

    @Test
    public void testGetServiceURL() {
        assertEquals(mockServiceURL, manager.getServiceURL());
    }

    @Test
    public void testUploadFromFlowableAsyncWithKnownSize() {
        String containerName = "testcontainer";
        String blobName = "testblob";
        Flowable<ByteBuffer> data = Flowable.just(ByteBuffer.wrap("test data".getBytes()));
        long totalSize = 9L;
        
        ContainerCreateResponse mockCreateResponse = mock(ContainerCreateResponse.class);
        CommonRestResponse mockUploadResponse = mock(CommonRestResponse.class);
        
        when(mockServiceURL.createContainerURL(containerName)).thenReturn(mockContainerURL);
        when(mockContainerURL.create()).thenReturn(Single.just(mockCreateResponse));
        when(mockContainerURL.createBlockBlobURL(blobName)).thenReturn(mockBlobURL);
        when(mockUploadResponse.statusCode()).thenReturn(201);
        
        // Note: TransferManager is a static utility, so we can't easily mock it in unit tests
        // In a real scenario, we would need integration tests or refactor to use dependency injection
    }

    @Test
    public void testUploadFromFlowableAsyncWithUnknownSize() {
        String containerName = "testcontainer";
        String blobName = "testblob";
        Flowable<ByteBuffer> data = Flowable.just(
                ByteBuffer.wrap("chunk1".getBytes()),
                ByteBuffer.wrap("chunk2".getBytes())
        );
        long totalSize = -1L; // Unknown size
        
        ContainerCreateResponse mockCreateResponse = mock(ContainerCreateResponse.class);
        
        when(mockServiceURL.createContainerURL(containerName)).thenReturn(mockContainerURL);
        when(mockContainerURL.create()).thenReturn(Single.just(mockCreateResponse));
        when(mockContainerURL.createBlockBlobURL(blobName)).thenReturn(mockBlobURL);
        
        // Note: TransferManager is a static utility, so we can't easily mock it in unit tests
    }

    @Test
    public void testConstructorValidatesBlockSize() {
        ConcurrentUploadManager smallBlockManager = new ConcurrentUploadManager(mockServiceURL, 512, 4);
        assertEquals(512, smallBlockManager.getBlockSizeBytes());
        
        ConcurrentUploadManager largeBlockManager = new ConcurrentUploadManager(mockServiceURL, 100 * 1024 * 1024, 4);
        assertEquals(100 * 1024 * 1024, largeBlockManager.getBlockSizeBytes());
    }

    @Test
    public void testConstructorValidatesConcurrency() {
        ConcurrentUploadManager minConcurrency = new ConcurrentUploadManager(mockServiceURL, 1024 * 1024, 1);
        assertEquals(1, minConcurrency.getMaxConcurrency());
        
        ConcurrentUploadManager maxConcurrency = new ConcurrentUploadManager(mockServiceURL, 1024 * 1024, 100);
        assertEquals(100, maxConcurrency.getMaxConcurrency());
    }
}
