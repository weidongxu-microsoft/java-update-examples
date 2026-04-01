package com.contoso.messaging;

import com.microsoft.azure.servicebus.primitives.AuthorizationFailedException;
import com.microsoft.azure.servicebus.primitives.MessageLockLostException;
import com.microsoft.azure.servicebus.primitives.MessagingEntityNotFoundException;
import com.microsoft.azure.servicebus.primitives.QuotaExceededException;
import com.microsoft.azure.servicebus.primitives.ServerBusyException;
import com.microsoft.azure.servicebus.primitives.ServiceBusException;
import com.microsoft.azure.servicebus.primitives.SessionLockLostException;

import java.util.EnumMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

public class ErrorClassifier {

    public enum ErrorCategory {
        TRANSIENT,
        ENTITY_NOT_FOUND,
        AUTHORIZATION,
        QUOTA_EXCEEDED,
        LOCK_LOST,
        THROTTLED,
        UNKNOWN
    }

    private final Map<ErrorCategory, AtomicLong> errorCounts;

    public ErrorClassifier() {
        this.errorCounts = new EnumMap<>(ErrorCategory.class);
        for (ErrorCategory cat : ErrorCategory.values()) {
            errorCounts.put(cat, new AtomicLong(0));
        }
    }

    public ErrorCategory classify(Throwable error) {
        Throwable cause = unwrap(error);
        ErrorCategory category;

        if (cause instanceof MessageLockLostException) {
            category = ErrorCategory.LOCK_LOST;
        } else if (cause instanceof SessionLockLostException) {
            category = ErrorCategory.LOCK_LOST;
        } else if (cause instanceof ServerBusyException) {
            category = ErrorCategory.THROTTLED;
        } else if (cause instanceof QuotaExceededException) {
            category = ErrorCategory.QUOTA_EXCEEDED;
        } else if (cause instanceof AuthorizationFailedException) {
            category = ErrorCategory.AUTHORIZATION;
        } else if (cause instanceof MessagingEntityNotFoundException) {
            category = ErrorCategory.ENTITY_NOT_FOUND;
        } else if (cause instanceof ServiceBusException) {
            ServiceBusException sbEx = (ServiceBusException) cause;
            if (sbEx.getIsTransient()) {
                category = ErrorCategory.TRANSIENT;
            } else {
                category = ErrorCategory.UNKNOWN;
            }
        } else {
            category = ErrorCategory.UNKNOWN;
        }

        errorCounts.get(category).incrementAndGet();
        return category;
    }

    public long getErrorCount(ErrorCategory category) {
        return errorCounts.get(category).get();
    }

    public boolean hasTransientErrors() {
        return errorCounts.get(ErrorCategory.TRANSIENT).get() > 0;
    }

    public Map<ErrorCategory, Long> getSnapshot() {
        Map<ErrorCategory, Long> snapshot = new EnumMap<>(ErrorCategory.class);
        for (Map.Entry<ErrorCategory, AtomicLong> entry : errorCounts.entrySet()) {
            snapshot.put(entry.getKey(), entry.getValue().get());
        }
        return snapshot;
    }

    private Throwable unwrap(Throwable error) {
        Throwable current = error;
        while (current.getCause() != null && !(current instanceof ServiceBusException)) {
            current = current.getCause();
        }
        return current;
    }
}
