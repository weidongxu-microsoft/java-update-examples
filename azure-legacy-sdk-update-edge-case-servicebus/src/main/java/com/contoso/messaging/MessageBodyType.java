package com.contoso.messaging;

/**
 * Application-level enum replacing the removed com.microsoft.azure.servicebus.MessageBodyType.
 * The modern Azure Service Bus SDK (com.azure:azure-messaging-servicebus) uses BinaryData
 * exclusively; this enum preserves the legacy body type semantics at the application level.
 */
public enum MessageBodyType {
    BINARY,
    VALUE,
    SEQUENCE
}
