package com.contoso.messaging;

/**
 * Application-level enum replacing the removed com.microsoft.azure.servicebus.ExceptionPhase.
 * Maps to ServiceBusErrorSource in the modern SDK but kept as an application-level type
 * to preserve the existing handler interface contract.
 */
public enum ErrorPhase {
    RECEIVE,
    COMPLETE,
    ABANDON,
    RENEW_LOCK,
    ACCEPT_SESSION,
    UNKNOWN
}
