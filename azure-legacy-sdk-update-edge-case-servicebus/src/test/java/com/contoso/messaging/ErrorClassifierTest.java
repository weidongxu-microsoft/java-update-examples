package com.contoso.messaging;

import com.microsoft.azure.servicebus.primitives.AuthorizationFailedException;
import com.microsoft.azure.servicebus.primitives.MessageLockLostException;
import com.microsoft.azure.servicebus.primitives.MessagingEntityNotFoundException;
import com.microsoft.azure.servicebus.primitives.QuotaExceededException;
import com.microsoft.azure.servicebus.primitives.ServerBusyException;
import com.microsoft.azure.servicebus.primitives.ServiceBusException;
import com.microsoft.azure.servicebus.primitives.SessionLockLostException;
import org.junit.Before;
import org.junit.Test;

import java.util.Map;

import static org.junit.Assert.*;

public class ErrorClassifierTest {

    private ErrorClassifier classifier;

    @Before
    public void setUp() {
        classifier = new ErrorClassifier();
    }

    @Test
    public void testClassifiesTransientException() {
        ServiceBusException transientEx = new ServiceBusException(true, "Temporarily unavailable");
        ErrorClassifier.ErrorCategory category = classifier.classify(transientEx);
        assertEquals(ErrorClassifier.ErrorCategory.TRANSIENT, category);
        assertEquals(1, classifier.getErrorCount(ErrorClassifier.ErrorCategory.TRANSIENT));
    }

    @Test
    public void testClassifiesEntityNotFoundException() {
        MessagingEntityNotFoundException notFoundEx =
            new MessagingEntityNotFoundException("orders-queue");
        ErrorClassifier.ErrorCategory category = classifier.classify(notFoundEx);
        assertEquals(ErrorClassifier.ErrorCategory.ENTITY_NOT_FOUND, category);
    }

    @Test
    public void testClassifiesServerBusyAsThrottled() {
        ServerBusyException sbe = new ServerBusyException("Server is busy");
        ErrorClassifier.ErrorCategory category = classifier.classify(sbe);
        assertEquals(ErrorClassifier.ErrorCategory.THROTTLED, category);
    }

    @Test
    public void testServerBusyExceptionIsTransientButClassifiedAsThrottled() {
        ServerBusyException sbe = new ServerBusyException("Throttled");
        assertTrue(sbe.getIsTransient());
        assertTrue(sbe instanceof ServiceBusException);
        assertEquals(ErrorClassifier.ErrorCategory.THROTTLED, classifier.classify(sbe));
    }

    @Test
    public void testClassifiesMessageLockLost() {
        MessageLockLostException mlle = new MessageLockLostException("Lock expired");
        ErrorClassifier.ErrorCategory category = classifier.classify(mlle);
        assertEquals(ErrorClassifier.ErrorCategory.LOCK_LOST, category);
    }

    @Test
    public void testMessageLockLostIsNotTransient() {
        MessageLockLostException mlle = new MessageLockLostException("Lock expired");
        assertFalse(mlle.getIsTransient());
    }

    @Test
    public void testClassifiesSessionLockLost() {
        SessionLockLostException slle = new SessionLockLostException("Session lock lost");
        ErrorClassifier.ErrorCategory category = classifier.classify(slle);
        assertEquals(ErrorClassifier.ErrorCategory.LOCK_LOST, category);
    }

    @Test
    public void testClassifiesQuotaExceeded() {
        QuotaExceededException qee = new QuotaExceededException("Quota hit");
        ErrorClassifier.ErrorCategory category = classifier.classify(qee);
        assertEquals(ErrorClassifier.ErrorCategory.QUOTA_EXCEEDED, category);
    }

    @Test
    public void testClassifiesAuthorizationFailed() {
        AuthorizationFailedException afe = new AuthorizationFailedException("No access");
        ErrorClassifier.ErrorCategory category = classifier.classify(afe);
        assertEquals(ErrorClassifier.ErrorCategory.AUTHORIZATION, category);
    }

    @Test
    public void testClassifiesWrappedException() {
        ServiceBusException inner = new ServiceBusException(true, "Inner error");
        RuntimeException wrapped = new RuntimeException("Outer", inner);
        ErrorClassifier.ErrorCategory category = classifier.classify(wrapped);
        assertEquals(ErrorClassifier.ErrorCategory.TRANSIENT, category);
    }

    @Test
    public void testClassifiesWrappedSubclassException() {
        ServerBusyException inner = new ServerBusyException("Busy");
        RuntimeException wrapped = new RuntimeException("Wrapper", inner);
        ErrorClassifier.ErrorCategory category = classifier.classify(wrapped);
        assertEquals(ErrorClassifier.ErrorCategory.THROTTLED, category);
    }

    @Test
    public void testClassifiesUnknownException() {
        RuntimeException unknown = new RuntimeException("Something else");
        ErrorClassifier.ErrorCategory category = classifier.classify(unknown);
        assertEquals(ErrorClassifier.ErrorCategory.UNKNOWN, category);
    }

    @Test
    public void testCountsMultipleErrors() {
        classifier.classify(new ServiceBusException(true, "Error 1"));
        classifier.classify(new ServiceBusException(true, "Error 2"));
        classifier.classify(new ServerBusyException("Busy"));

        assertEquals(2, classifier.getErrorCount(ErrorClassifier.ErrorCategory.TRANSIENT));
        assertEquals(1, classifier.getErrorCount(ErrorClassifier.ErrorCategory.THROTTLED));
        assertTrue(classifier.hasTransientErrors());
    }

    @Test
    public void testSnapshotReturnsAllCategories() {
        classifier.classify(new ServiceBusException(true, "Transient"));
        classifier.classify(new MessagingEntityNotFoundException("test"));
        classifier.classify(new MessageLockLostException("Lock lost"));

        Map<ErrorClassifier.ErrorCategory, Long> snapshot = classifier.getSnapshot();
        assertEquals(Long.valueOf(1), snapshot.get(ErrorClassifier.ErrorCategory.TRANSIENT));
        assertEquals(Long.valueOf(1), snapshot.get(ErrorClassifier.ErrorCategory.ENTITY_NOT_FOUND));
        assertEquals(Long.valueOf(1), snapshot.get(ErrorClassifier.ErrorCategory.LOCK_LOST));
        assertEquals(Long.valueOf(0), snapshot.get(ErrorClassifier.ErrorCategory.AUTHORIZATION));
    }

    @Test
    public void testExceptionHierarchyBehavioralQuirks() {
        assertFalse(new MessagingEntityNotFoundException("x").getIsTransient());
        assertTrue(new ServerBusyException("x").getIsTransient());
        assertFalse(new MessageLockLostException("x").getIsTransient());
        assertFalse(new SessionLockLostException("x").getIsTransient());
        assertFalse(new QuotaExceededException("x").getIsTransient());
        assertFalse(new AuthorizationFailedException("x").getIsTransient());
    }
}
