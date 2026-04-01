package com.contoso.messaging;

import com.azure.messaging.servicebus.ServiceBusMessage;

@FunctionalInterface
public interface MessageTransformer {
    ServiceBusMessage transform(ServiceBusMessage source);
}
