package com.contoso.messaging;

import com.microsoft.azure.servicebus.IMessage;
import com.microsoft.azure.servicebus.primitives.ServiceBusException;

@FunctionalInterface
public interface MessageTransformer {
    IMessage transform(IMessage source) throws ServiceBusException;
}
