package com.contoso.messaging;

import com.microsoft.azure.servicebus.primitives.MessagingEntityNotFoundException;
import com.microsoft.azure.servicebus.primitives.ServiceBusException;
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
    public void testClassifiesAuthorizationFailure() {
        ServiceBusException authEx = new ServiceBusException(false, "Unauthorized access");
        ErrorClassifier.ErrorCategory category = classifier.classify(authEx);
        assertEquals(ErrorClassifier.ErrorCategory.AUTHORIZATION, category);
    }

    @Test
    public void testClassifiesQuotaExceeded() {
        ServiceBusException quotaEx = new ServiceBusException(false, "QuotaExceeded for namespace");
        ErrorClassifier.ErrorCategory category = classifier.classify(quotaEx);
        assertEquals(ErrorClassifier.ErrorCategory.QUOTA_EXCEEDED, category);
    }

    @Test
    public void testClassifiesMessageSizeExceeded() {
        ServiceBusException sizeEx = new ServiceBusException(false, "MessageSizeExceeded: message too large");
        ErrorClassifier.ErrorCategory category = classifier.classify(sizeEx);
        assertEquals(ErrorClassifier.ErrorCategory.MESSAGE_SIZE_EXCEEDED, category);
    }

    @Test
    public void testClassifiesWrappedException() {
        ServiceBusException inner = new ServiceBusException(true, "Inner error");
        RuntimeException wrapped = new RuntimeException("Outer", inner);
        ErrorClassifier.ErrorCategory category = classifier.classify(wrapped);
        assertEquals(ErrorClassifier.ErrorCategory.TRANSIENT, category);
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
        classifier.classify(new ServiceBusException(false, "Unknown error"));

        assertEquals(2, classifier.getErrorCount(ErrorClassifier.ErrorCategory.TRANSIENT));
        assertTrue(classifier.hasTransientErrors());
    }

    @Test
    public void testSnapshotReturnsAllCategories() {
        classifier.classify(new ServiceBusException(true, "Transient"));
        classifier.classify(new MessagingEntityNotFoundException("test"));

        Map<ErrorClassifier.ErrorCategory, Long> snapshot = classifier.getSnapshot();
        assertEquals(Long.valueOf(1), snapshot.get(ErrorClassifier.ErrorCategory.TRANSIENT));
        assertEquals(Long.valueOf(1), snapshot.get(ErrorClassifier.ErrorCategory.ENTITY_NOT_FOUND));
        assertEquals(Long.valueOf(0), snapshot.get(ErrorClassifier.ErrorCategory.AUTHORIZATION));
    }
}
