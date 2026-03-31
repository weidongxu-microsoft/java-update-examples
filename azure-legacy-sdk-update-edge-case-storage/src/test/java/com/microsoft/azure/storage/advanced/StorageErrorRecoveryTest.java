package com.microsoft.azure.storage.advanced;

import com.microsoft.azure.storage.StorageErrorCodeStrings;
import com.microsoft.azure.storage.StorageException;

import org.junit.Before;
import org.junit.Test;

import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class StorageErrorRecoveryTest {

    private StorageErrorRecovery recovery;

    @Before
    public void setUp() {
        recovery = new StorageErrorRecovery();
    }

    @Test
    public void testContainerNotFoundRecovery() {
        StorageException ex = new StorageException(
                StorageErrorCodeStrings.CONTAINER_NOT_FOUND.toString(),
                "Container not found", 404, null, null);
        assertEquals(StorageErrorRecovery.RecoveryAction.CREATE_CONTAINER,
                recovery.determineRecovery(ex));
    }

    @Test
    public void testBlobNotFoundRecovery() {
        StorageException ex = new StorageException(
                StorageErrorCodeStrings.BLOB_NOT_FOUND.toString(),
                "Blob not found", 404, null, null);
        assertEquals(StorageErrorRecovery.RecoveryAction.SKIP,
                recovery.determineRecovery(ex));
    }

    @Test
    public void testLeaseIdMissingRecovery() {
        StorageException ex = new StorageException(
                StorageErrorCodeStrings.LEASE_ID_MISSING.toString(),
                "Lease ID missing", 412, null, null);
        assertEquals(StorageErrorRecovery.RecoveryAction.ACQUIRE_LEASE,
                recovery.determineRecovery(ex));
    }

    @Test
    public void testServerBusyRecovery() {
        StorageException ex = new StorageException(
                StorageErrorCodeStrings.SERVER_BUSY.toString(),
                "Server busy", 503, null, null);
        assertEquals(StorageErrorRecovery.RecoveryAction.RETRY_WITH_BACKOFF,
                recovery.determineRecovery(ex));
    }

    @Test
    public void testContainerAlreadyExistsRecovery() {
        StorageException ex = new StorageException(
                StorageErrorCodeStrings.CONTAINER_ALREADY_EXISTS.toString(),
                "Container exists", 409, null, null);
        assertEquals(StorageErrorRecovery.RecoveryAction.SKIP,
                recovery.determineRecovery(ex));
    }

    @Test
    public void testBlobAlreadyExistsRecovery() {
        StorageException ex = new StorageException(
                StorageErrorCodeStrings.BLOB_ALREADY_EXISTS.toString(),
                "Blob exists", 409, null, null);
        assertEquals(StorageErrorRecovery.RecoveryAction.RECREATE_BLOB,
                recovery.determineRecovery(ex));
    }

    @Test
    public void testConditionNotMetRecovery() {
        StorageException ex = new StorageException(
                StorageErrorCodeStrings.CONDITION_NOT_MET.toString(),
                "Condition not met", 412, null, null);
        assertEquals(StorageErrorRecovery.RecoveryAction.RETRY_WITH_BACKOFF,
                recovery.determineRecovery(ex));
    }

    @Test
    public void testContainerDisabledRecovery() {
        StorageException ex = new StorageException(
                StorageErrorCodeStrings.CONTAINER_DISABLED.toString(),
                "Container disabled", 403, null, null);
        assertEquals(StorageErrorRecovery.RecoveryAction.ABORT,
                recovery.determineRecovery(ex));
    }

    @Test
    public void testServerErrorFallbackToRetry() {
        StorageException ex = new StorageException("UnknownError",
                "Internal error", 500, null, null);
        assertEquals(StorageErrorRecovery.RecoveryAction.RETRY_WITH_BACKOFF,
                recovery.determineRecovery(ex));
    }

    @Test
    public void testClientErrorFallbackToAbort() {
        StorageException ex = new StorageException("UnknownClientError",
                "Bad request", 400, null, null);
        assertEquals(StorageErrorRecovery.RecoveryAction.ABORT,
                recovery.determineRecovery(ex));
    }

    @Test
    public void testGetRecoveryDescription() {
        StorageException ex = new StorageException(
                StorageErrorCodeStrings.BLOB_NOT_FOUND.toString(),
                "The specified blob does not exist.", 404, null, null);
        String description = recovery.getRecoveryDescription(ex);
        assertTrue(description.contains("BlobNotFound"));
        assertTrue(description.contains("404"));
        assertTrue(description.contains("SKIP"));
    }

    @Test
    public void testHandleWithRecoverySkip() {
        StorageException ex = new StorageException(
                StorageErrorCodeStrings.BLOB_NOT_FOUND.toString(),
                "Not found", 404, null, null);
        AtomicBoolean retryRan = new AtomicBoolean(false);
        recovery.handleWithRecovery(ex, () -> retryRan.set(true));
        assertFalse("SKIP should not run retry action", retryRan.get());
    }

    @Test(expected = RuntimeException.class)
    public void testHandleWithRecoveryAbort() {
        StorageException ex = new StorageException(
                StorageErrorCodeStrings.CONTAINER_DISABLED.toString(),
                "Disabled", 403, null, null);
        recovery.handleWithRecovery(ex, () -> {});
    }

    @Test
    public void testIsRetryable() {
        StorageException retryable = new StorageException(
                StorageErrorCodeStrings.SERVER_BUSY.toString(), "Busy", 503, null, null);
        StorageException notRetryable = new StorageException(
                StorageErrorCodeStrings.BLOB_NOT_FOUND.toString(), "Not found", 404, null, null);

        assertTrue(recovery.isRetryable(retryable));
        assertFalse(recovery.isRetryable(notRetryable));
    }

    @Test
    public void testIsSkippable() {
        StorageException skippable = new StorageException(
                StorageErrorCodeStrings.BLOB_NOT_FOUND.toString(), "Not found", 404, null, null);
        StorageException notSkippable = new StorageException(
                StorageErrorCodeStrings.SERVER_BUSY.toString(), "Busy", 503, null, null);

        assertTrue(recovery.isSkippable(skippable));
        assertFalse(recovery.isSkippable(notSkippable));
    }

    @Test
    public void testGetErrorRecoveryMap() {
        Map<String, StorageErrorRecovery.RecoveryAction> map =
                StorageErrorRecovery.getErrorRecoveryMap();
        assertNotNull(map);
        assertEquals(8, map.size());
        assertEquals(StorageErrorRecovery.RecoveryAction.CREATE_CONTAINER,
                map.get(StorageErrorCodeStrings.CONTAINER_NOT_FOUND));
    }

    @Test
    public void testRecoveryActionEnum() {
        StorageErrorRecovery.RecoveryAction[] actions = StorageErrorRecovery.RecoveryAction.values();
        assertEquals(6, actions.length);
        assertNotNull(StorageErrorRecovery.RecoveryAction.valueOf("CREATE_CONTAINER"));
        assertNotNull(StorageErrorRecovery.RecoveryAction.valueOf("SKIP"));
        assertNotNull(StorageErrorRecovery.RecoveryAction.valueOf("ACQUIRE_LEASE"));
        assertNotNull(StorageErrorRecovery.RecoveryAction.valueOf("RETRY_WITH_BACKOFF"));
        assertNotNull(StorageErrorRecovery.RecoveryAction.valueOf("ABORT"));
        assertNotNull(StorageErrorRecovery.RecoveryAction.valueOf("RECREATE_BLOB"));
    }

    @Test
    public void testExceptionMessagePreserved() {
        StorageException ex = new StorageException("BlobNotFound",
                "The specified blob does not exist.", 404, null, null);
        assertTrue(ex.getMessage().contains("The specified blob does not exist."));
        assertEquals(404, ex.getHttpStatusCode());
        assertEquals("BlobNotFound", ex.getErrorCode());
        assertNotNull(recovery.getRecoveryDescription(ex));
    }
}
