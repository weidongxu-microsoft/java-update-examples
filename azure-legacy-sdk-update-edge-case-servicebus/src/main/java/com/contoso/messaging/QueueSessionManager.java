package com.contoso.messaging;

import com.azure.messaging.servicebus.ServiceBusClientBuilder;
import com.azure.messaging.servicebus.ServiceBusProcessorClient;
import com.azure.messaging.servicebus.ServiceBusSenderClient;
import com.azure.messaging.servicebus.models.ServiceBusReceiveMode;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class QueueSessionManager {

    private final Map<String, ServiceBusProcessorClient> activeProcessors;
    private final ErrorClassifier errorClassifier;

    public QueueSessionManager(ErrorClassifier errorClassifier) {
        this.activeProcessors = new ConcurrentHashMap<>();
        this.errorClassifier = errorClassifier;
    }

    public ServiceBusProcessorClient createAndRegister(
            String connectionString,
            String queueName,
            OrderMessageHandler handler,
            int maxConcurrentCalls,
            Duration maxAutoRenewDuration) {
        
        ServiceBusProcessorClient processor = new ServiceBusClientBuilder()
            .connectionString(connectionString)
            .processor()
            .queueName(queueName)
            .receiveMode(ServiceBusReceiveMode.PEEK_LOCK)
            .maxConcurrentCalls(maxConcurrentCalls)
            .maxAutoLockRenewDuration(maxAutoRenewDuration)
            .processMessage(context -> {
                handler.accept(context.getMessage());
                context.complete();
            })
            .processError(context -> {
                handler.handleError(context.getException());
            })
            .buildProcessorClient();
        
        processor.start();
        activeProcessors.put(queueName, processor);
        return processor;
    }

    public ServiceBusProcessorClient createWithTransformer(
            String connectionString,
            String queueName,
            MessageTransformer transformer,
            ServiceBusSenderClient forwardSender) {
        OrderMessageHandler handler = new OrderMessageHandler(forwardSender, errorClassifier);
        handler.addTransformer(transformer);
        return createAndRegister(connectionString, queueName, handler, 1, Duration.ofMinutes(5));
    }

    public ServiceBusProcessorClient getQueue(String queueName) {
        return activeProcessors.get(queueName);
    }

    public void closeAll() {
        for (Map.Entry<String, ServiceBusProcessorClient> entry : activeProcessors.entrySet()) {
            try {
                entry.getValue().close();
            } catch (Exception e) {
                errorClassifier.classify(e);
            }
        }
        activeProcessors.clear();
    }

    public int getActiveQueueCount() {
        return activeProcessors.size();
    }
}
