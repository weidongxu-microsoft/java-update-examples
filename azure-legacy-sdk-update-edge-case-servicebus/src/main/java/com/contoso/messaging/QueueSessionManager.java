package com.contoso.messaging;

import com.microsoft.azure.servicebus.IMessageHandler;
import com.microsoft.azure.servicebus.IMessageSender;
import com.microsoft.azure.servicebus.IQueueClient;
import com.microsoft.azure.servicebus.MessageHandlerOptions;
import com.microsoft.azure.servicebus.QueueClient;
import com.microsoft.azure.servicebus.ReceiveMode;
import com.microsoft.azure.servicebus.primitives.ConnectionStringBuilder;
import com.microsoft.azure.servicebus.primitives.ServiceBusException;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class QueueSessionManager {

    private final Map<String, IQueueClient> activeQueues;
    private final ErrorClassifier errorClassifier;

    public QueueSessionManager(ErrorClassifier errorClassifier) {
        this.activeQueues = new ConcurrentHashMap<>();
        this.errorClassifier = errorClassifier;
    }

    public IQueueClient createAndRegister(
            ConnectionStringBuilder connectionStringBuilder,
            IMessageHandler handler,
            int maxConcurrentCalls,
            Duration maxAutoRenewDuration) throws InterruptedException, ServiceBusException {
        QueueClient client = new QueueClient(connectionStringBuilder, ReceiveMode.PEEKLOCK);
        MessageHandlerOptions options = new MessageHandlerOptions(
            maxConcurrentCalls, true, maxAutoRenewDuration);
        client.registerMessageHandler(handler, options);
        activeQueues.put(connectionStringBuilder.getEntityPath(), client);
        return client;
    }

    public IQueueClient createWithTransformer(
            ConnectionStringBuilder connectionStringBuilder,
            MessageTransformer transformer,
            IMessageSender forwardSender) throws InterruptedException, ServiceBusException {
        OrderMessageHandler handler = new OrderMessageHandler(forwardSender, errorClassifier);
        handler.addTransformer(transformer);
        return createAndRegister(connectionStringBuilder, handler, 1, Duration.ofMinutes(5));
    }

    public IQueueClient getQueue(String entityPath) {
        return activeQueues.get(entityPath);
    }

    public void closeAll() throws ServiceBusException {
        for (Map.Entry<String, IQueueClient> entry : activeQueues.entrySet()) {
            try {
                entry.getValue().close();
            } catch (ServiceBusException e) {
                errorClassifier.classify(e);
            }
        }
        activeQueues.clear();
    }

    public int getActiveQueueCount() {
        return activeQueues.size();
    }
}
