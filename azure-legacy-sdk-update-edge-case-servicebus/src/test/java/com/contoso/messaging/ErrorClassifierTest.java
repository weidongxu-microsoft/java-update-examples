package com.contoso.messaging;

import com.azure.core.amqp.exception.AmqpErrorCondition;
import com.azure.core.amqp.exception.AmqpException;
import com.azure.messaging.servicebus.ServiceBusErrorSource;
import com.azure.messaging.servicebus.ServiceBusException;
import com.azure.messaging.servicebus.ServiceBusFailureReason;
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
        ServiceBusException transientEx = new ServiceBusException(
            new AmqpException(true, AmqpErrorCondition.TIMEOUT_ERROR, "Temporarily unavailable", null),
            ServiceBusErrorSource.RECEIVE);
        ErrorClassifier.ErrorCategory category = classifier.classify(transientEx);
        assertEquals(ErrorClassifier.ErrorCategory.TRANSIENT, category);
        assertEquals(1, classifier.getErrorCount(ErrorClassifier.ErrorCategory.TRANSIENT));
    }

    @Test
    public void testClassifiesEntityNotFoundException() {
        ServiceBusException notFoundEx = new ServiceBusException(
            new AmqpException(false, AmqpErrorCondition.NOT_FOUND, "orders-queue", null),
            ServiceBusErrorSource.RECEIVE);
        ErrorClassifier.ErrorCategory category = classifier.classify(notFoundEx);
        assertEquals(ErrorClassifier.ErrorCategory.ENTITY_NOT_FOUND, category);
    }

    @Test
    public void testClassifiesServerBusyAsThrottled() {
        ServiceBusException sbe = new ServiceBusException(
            new AmqpException(true, AmqpErrorCondition.SERVER_BUSY_ERROR, "Server is busy", null),
            ServiceBusErrorSource.RECEIVE);
        ErrorClassifier.ErrorCategory category = classifier.classify(sbe);
        assertEquals(ErrorClassifier.ErrorCategory.THROTTLED, category);
    }

    @Test
    public void testServerBusyExceptionIsTransientButClassifiedAsThrottled() {
        ServiceBusException sbe = new ServiceBusException(
            new AmqpException(true, AmqpErrorCondition.SERVER_BUSY_ERROR, "Throttled", null),
            ServiceBusErrorSource.RECEIVE);
        assertTrue(sbe.isTransient());
        assertEquals(ErrorClassifier.ErrorCategory.THROTTLED, classifier.classify(sbe));
    }

    @Test
    public void testClassifiesMessageLockLost() {
        ServiceBusException mlle = new ServiceBusException(
            new AmqpException(false, AmqpErrorCondition.MESSAGE_LOCK_LOST, "Lock expired", null),
            ServiceBusErrorSource.RECEIVE);
        ErrorClassifier.ErrorCategory category = classifier.classify(mlle);
        assertEquals(ErrorClassifier.ErrorCategory.LOCK_LOST, category);
    }

    @Test
    public void testMessageLockLostIsNotTransient() {
        ServiceBusException mlle = new ServiceBusException(
            new AmqpException(false, AmqpErrorCondition.MESSAGE_LOCK_LOST, "Lock expired", null),
            ServiceBusErrorSource.RECEIVE);
        assertFalse(mlle.isTransient());
    }

    @Test
    public void testClassifiesSessionLockLost() {
        ServiceBusException slle = new ServiceBusException(
            new AmqpException(false, AmqpErrorCondition.SESSION_LOCK_LOST, "Session lock lost", null),
            ServiceBusErrorSource.RECEIVE);
        ErrorClassifier.ErrorCategory category = classifier.classify(slle);
        assertEquals(ErrorClassifier.ErrorCategory.LOCK_LOST, category);
    }

    @Test
    public void testClassifiesQuotaExceeded() {
        ServiceBusException qee = new ServiceBusException(
            new AmqpException(false, AmqpErrorCondition.RESOURCE_LIMIT_EXCEEDED, "Quota hit", null),
            ServiceBusErrorSource.RECEIVE);
        ErrorClassifier.ErrorCategory category = classifier.classify(qee);
        assertEquals(ErrorClassifier.ErrorCategory.QUOTA_EXCEEDED, category);
    }

    @Test
    public void testClassifiesAuthorizationFailed() {
        ServiceBusException afe = new ServiceBusException(
            new AmqpException(false, AmqpErrorCondition.UNAUTHORIZED_ACCESS, "No access", null),
            ServiceBusErrorSource.RECEIVE);
        ErrorClassifier.ErrorCategory category = classifier.classify(afe);
        assertEquals(ErrorClassifier.ErrorCategory.AUTHORIZATION, category);
    }

    @Test
    public void testClassifiesWrappedException() {
        ServiceBusException inner = new ServiceBusException(
            new AmqpException(true, AmqpErrorCondition.TIMEOUT_ERROR, "Inner error", null),
            ServiceBusErrorSource.RECEIVE);
        RuntimeException wrapped = new RuntimeException("Outer", inner);
        ErrorClassifier.ErrorCategory category = classifier.classify(wrapped);
        assertEquals(ErrorClassifier.ErrorCategory.TRANSIENT, category);
    }

    @Test
    public void testClassifiesWrappedSubclassException() {
        ServiceBusException inner = new ServiceBusException(
            new AmqpException(true, AmqpErrorCondition.SERVER_BUSY_ERROR, "Busy", null),
            ServiceBusErrorSource.RECEIVE);
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
        classifier.classify(new ServiceBusException(
            new AmqpException(true, AmqpErrorCondition.TIMEOUT_ERROR, "Error 1", null),
            ServiceBusErrorSource.RECEIVE));
        classifier.classify(new ServiceBusException(
            new AmqpException(true, AmqpErrorCondition.TIMEOUT_ERROR, "Error 2", null),
            ServiceBusErrorSource.RECEIVE));
        classifier.classify(new ServiceBusException(
            new AmqpException(true, AmqpErrorCondition.SERVER_BUSY_ERROR, "Busy", null),
            ServiceBusErrorSource.RECEIVE));

        assertEquals(2, classifier.getErrorCount(ErrorClassifier.ErrorCategory.TRANSIENT));
        assertEquals(1, classifier.getErrorCount(ErrorClassifier.ErrorCategory.THROTTLED));
        assertTrue(classifier.hasTransientErrors());
    }

    @Test
    public void testSnapshotReturnsAllCategories() {
        classifier.classify(new ServiceBusException(
            new AmqpException(true, AmqpErrorCondition.TIMEOUT_ERROR, "Transient", null),
            ServiceBusErrorSource.RECEIVE));
        classifier.classify(new ServiceBusException(
            new AmqpException(false, AmqpErrorCondition.NOT_FOUND, "test", null),
            ServiceBusErrorSource.RECEIVE));
        classifier.classify(new ServiceBusException(
            new AmqpException(false, AmqpErrorCondition.MESSAGE_LOCK_LOST, "Lock lost", null),
            ServiceBusErrorSource.RECEIVE));

        Map<ErrorClassifier.ErrorCategory, Long> snapshot = classifier.getSnapshot();
        assertEquals(Long.valueOf(1), snapshot.get(ErrorClassifier.ErrorCategory.TRANSIENT));
        assertEquals(Long.valueOf(1), snapshot.get(ErrorClassifier.ErrorCategory.ENTITY_NOT_FOUND));
        assertEquals(Long.valueOf(1), snapshot.get(ErrorClassifier.ErrorCategory.LOCK_LOST));
        assertEquals(Long.valueOf(0), snapshot.get(ErrorClassifier.ErrorCategory.AUTHORIZATION));
    }

    @Test
    public void testExceptionHierarchyBehavioralQuirks() {
        assertFalse(new ServiceBusException(
            new AmqpException(false, AmqpErrorCondition.NOT_FOUND, "x", null),
            ServiceBusErrorSource.RECEIVE).isTransient());
        assertTrue(new ServiceBusException(
            new AmqpException(true, AmqpErrorCondition.SERVER_BUSY_ERROR, "x", null),
            ServiceBusErrorSource.RECEIVE).isTransient());
        assertFalse(new ServiceBusException(
            new AmqpException(false, AmqpErrorCondition.MESSAGE_LOCK_LOST, "x", null),
            ServiceBusErrorSource.RECEIVE).isTransient());
        assertFalse(new ServiceBusException(
            new AmqpException(false, AmqpErrorCondition.SESSION_LOCK_LOST, "x", null),
            ServiceBusErrorSource.RECEIVE).isTransient());
        assertFalse(new ServiceBusException(
            new AmqpException(false, AmqpErrorCondition.RESOURCE_LIMIT_EXCEEDED, "x", null),
            ServiceBusErrorSource.RECEIVE).isTransient());
        assertFalse(new ServiceBusException(
            new AmqpException(false, AmqpErrorCondition.UNAUTHORIZED_ACCESS, "x", null),
            ServiceBusErrorSource.RECEIVE).isTransient());
    }
}
