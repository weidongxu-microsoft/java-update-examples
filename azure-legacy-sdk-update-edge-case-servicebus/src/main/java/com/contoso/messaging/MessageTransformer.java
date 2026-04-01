package com.contoso.messaging;

import com.azure.messaging.servicebus.ServiceBusMessage;
import com.azure.messaging.servicebus.ServiceBusException;

@FunctionalInterface
public interface MessageTransformer {
    ServiceBusMessage transform(ServiceBusMessage source) throws ServiceBusException;
}
