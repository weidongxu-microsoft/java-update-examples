package com.microsoft.azure.storage.advanced;

import com.microsoft.azure.storage.StorageErrorCodeStrings;
import com.microsoft.azure.storage.StorageException;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

public class StorageErrorRecovery {

    public enum RecoveryAction {
        CREATE_CONTAINER,
        SKIP,
        ACQUIRE_LEASE,
        RETRY_WITH_BACKOFF,
        ABORT,
        RECREATE_BLOB
    }

    private static final Map<String, RecoveryAction> ERROR_RECOVERY;

    static {
        Map<String, RecoveryAction> map = new LinkedHashMap<>();
        map.put(StorageErrorCodeStrings.CONTAINER_NOT_FOUND, RecoveryAction.CREATE_CONTAINER);
        map.put(StorageErrorCodeStrings.BLOB_NOT_FOUND, RecoveryAction.SKIP);
        map.put(StorageErrorCodeStrings.LEASE_ID_MISSING, RecoveryAction.ACQUIRE_LEASE);
        map.put(StorageErrorCodeStrings.SERVER_BUSY, RecoveryAction.RETRY_WITH_BACKOFF);
        map.put(StorageErrorCodeStrings.CONTAINER_ALREADY_EXISTS, RecoveryAction.SKIP);
        map.put(StorageErrorCodeStrings.BLOB_ALREADY_EXISTS, RecoveryAction.RECREATE_BLOB);
        map.put(StorageErrorCodeStrings.CONDITION_NOT_MET, RecoveryAction.RETRY_WITH_BACKOFF);
        map.put(StorageErrorCodeStrings.CONTAINER_DISABLED, RecoveryAction.ABORT);
        ERROR_RECOVERY = Collections.unmodifiableMap(map);
    }

    public RecoveryAction determineRecovery(StorageException exception) {
        String errorCode = exception.getErrorCode();
        RecoveryAction action = ERROR_RECOVERY.get(errorCode);
        if (action != null) {
            return action;
        }
        if (exception.getHttpStatusCode() >= 500) {
            return RecoveryAction.RETRY_WITH_BACKOFF;
        }
        return RecoveryAction.ABORT;
    }

    public String getRecoveryDescription(StorageException exception) {
        RecoveryAction action = determineRecovery(exception);
        return String.format("Error [%s] (HTTP %d): %s -> %s",
                exception.getErrorCode(),
                exception.getHttpStatusCode(),
                exception.getMessage(),
                action.name());
    }

    public void handleWithRecovery(StorageException exception, Runnable retryAction) {
        RecoveryAction action = determineRecovery(exception);
        switch (action) {
            case RETRY_WITH_BACKOFF:
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return;
                }
                retryAction.run();
                break;
            case SKIP:
                break;
            case ABORT:
                throw new RuntimeException("Unrecoverable storage error: " + exception.getErrorCode(), exception);
            default:
                retryAction.run();
                break;
        }
    }

    public boolean isRetryable(StorageException exception) {
        RecoveryAction action = determineRecovery(exception);
        return action == RecoveryAction.RETRY_WITH_BACKOFF
                || action == RecoveryAction.CREATE_CONTAINER
                || action == RecoveryAction.RECREATE_BLOB;
    }

    public boolean isSkippable(StorageException exception) {
        RecoveryAction action = determineRecovery(exception);
        return action == RecoveryAction.SKIP;
    }

    public static Map<String, RecoveryAction> getErrorRecoveryMap() {
        return ERROR_RECOVERY;
    }
}
