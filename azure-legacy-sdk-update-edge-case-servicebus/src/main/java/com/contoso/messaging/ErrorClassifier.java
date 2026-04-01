package com.contoso.messaging;

import com.azure.messaging.servicebus.ServiceBusException;
import com.azure.messaging.servicebus.ServiceBusFailureReason;

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

        if (cause instanceof ServiceBusException) {
            ServiceBusException sbEx = (ServiceBusException) cause;
            ServiceBusFailureReason reason = sbEx.getReason();
            
            if (reason == ServiceBusFailureReason.MESSAGE_LOCK_LOST ||
                reason == ServiceBusFailureReason.SESSION_LOCK_LOST) {
                category = ErrorCategory.LOCK_LOST;
            } else if (reason == ServiceBusFailureReason.SERVICE_BUSY) {
                category = ErrorCategory.THROTTLED;
            } else if (reason == ServiceBusFailureReason.QUOTA_EXCEEDED) {
                category = ErrorCategory.QUOTA_EXCEEDED;
            } else if (reason == ServiceBusFailureReason.UNAUTHORIZED) {
                category = ErrorCategory.AUTHORIZATION;
            } else if (reason == ServiceBusFailureReason.MESSAGING_ENTITY_NOT_FOUND ||
                       reason == ServiceBusFailureReason.MESSAGING_ENTITY_DISABLED) {
                category = ErrorCategory.ENTITY_NOT_FOUND;
            } else if (sbEx.isTransient()) {
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
