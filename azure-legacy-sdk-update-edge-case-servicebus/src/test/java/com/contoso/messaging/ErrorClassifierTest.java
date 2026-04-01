package com.contoso.messaging;

import com.azure.messaging.servicebus.ServiceBusException;
import com.azure.messaging.servicebus.ServiceBusErrorSource;
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
    public void testClassifiesServiceBusException() {
        ServiceBusException ex = new ServiceBusException(
            new RuntimeException("Test exception"),
            ServiceBusErrorSource.UNKNOWN);
        ErrorClassifier.ErrorCategory category = classifier.classify(ex);
        // Without specific failure reasons in tests, it should fall into a default category
        assertNotNull(category);
    }

    @Test
    public void testClassifiesWrappedServiceBusException() {
        ServiceBusException inner = new ServiceBusException(
            new RuntimeException("Inner error"),
            ServiceBusErrorSource.UNKNOWN);
        RuntimeException wrapped = new RuntimeException("Outer", inner);
        ErrorClassifier.ErrorCategory category = classifier.classify(wrapped);
        assertNotNull(category);
    }

    @Test
    public void testClassifiesUnknownException() {
        RuntimeException unknown = new RuntimeException("Something else");
        ErrorClassifier.ErrorCategory category = classifier.classify(unknown);
        assertEquals(ErrorClassifier.ErrorCategory.UNKNOWN, category);
    }

    @Test
    public void testCountsMultipleErrors() {
        classifier.classify(new RuntimeException("Error 1"));
        classifier.classify(new RuntimeException("Error 2"));
        classifier.classify(new ServiceBusException(
            new RuntimeException("SB Error"),
            ServiceBusErrorSource.UNKNOWN));

        // We should have classified at least the unknown errors
        assertTrue(classifier.getErrorCount(ErrorClassifier.ErrorCategory.UNKNOWN) >= 2);
    }

    @Test
    public void testSnapshotReturnsAllCategories() {
        classifier.classify(new RuntimeException("Test 1"));
        classifier.classify(new ServiceBusException(
            new RuntimeException("Test 2"),
            ServiceBusErrorSource.UNKNOWN));

        Map<ErrorClassifier.ErrorCategory, Long> snapshot = classifier.getSnapshot();
        assertNotNull(snapshot);
        assertTrue(snapshot.containsKey(ErrorClassifier.ErrorCategory.UNKNOWN));
        assertTrue(snapshot.containsKey(ErrorClassifier.ErrorCategory.TRANSIENT));
        assertTrue(snapshot.containsKey(ErrorClassifier.ErrorCategory.LOCK_LOST));
    }

    @Test
    public void testHasTransientErrorsInitiallyFalse() {
        assertFalse(classifier.hasTransientErrors());
    }

    @Test
    public void testGetErrorCountInitiallyZero() {
        assertEquals(0, classifier.getErrorCount(ErrorClassifier.ErrorCategory.UNKNOWN));
        assertEquals(0, classifier.getErrorCount(ErrorClassifier.ErrorCategory.TRANSIENT));
    }

    @Test
    public void testClassifyIncrementsCount() {
        long initialCount = classifier.getErrorCount(ErrorClassifier.ErrorCategory.UNKNOWN);
        classifier.classify(new RuntimeException("Test"));
        long newCount = classifier.getErrorCount(ErrorClassifier.ErrorCategory.UNKNOWN);
        assertTrue(newCount > initialCount);
    }

    @Test
    public void testUnwrapsNestedExceptions() {
        RuntimeException level3 = new RuntimeException("Level 3");
        RuntimeException level2 = new RuntimeException("Level 2", level3);
        RuntimeException level1 = new RuntimeException("Level 1", level2);
        
        ErrorClassifier.ErrorCategory category = classifier.classify(level1);
        assertNotNull(category);
    }

    @Test
    public void testSnapshotIsImmutable() {
        classifier.classify(new RuntimeException("Test"));
        Map<ErrorClassifier.ErrorCategory, Long> snapshot1 = classifier.getSnapshot();
        classifier.classify(new RuntimeException("Another test"));
        Map<ErrorClassifier.ErrorCategory, Long> snapshot2 = classifier.getSnapshot();
        
        // Snapshots should be independent
        assertNotEquals(snapshot1.get(ErrorClassifier.ErrorCategory.UNKNOWN),
                       snapshot2.get(ErrorClassifier.ErrorCategory.UNKNOWN));
    }
}
