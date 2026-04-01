package com.contoso.messaging;

import com.azure.messaging.servicebus.ServiceBusClientBuilder;
import com.azure.messaging.servicebus.ServiceBusErrorSource;
import com.azure.messaging.servicebus.ServiceBusMessage;
import com.azure.messaging.servicebus.ServiceBusProcessorClient;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class QueueSessionManager {

    private final Map<String, ServiceBusProcessorClient> activeQueues;
    private final ErrorClassifier errorClassifier;

    public QueueSessionManager(ErrorClassifier errorClassifier) {
        this.activeQueues = new ConcurrentHashMap<>();
        this.errorClassifier = errorClassifier;
    }

    public ServiceBusProcessorClient createAndRegister(
            ConnectionStringProperties connectionProperties,
            MessageHandler handler,
            int maxConcurrentCalls,
            Duration maxAutoRenewDuration) {
        ServiceBusProcessorClient client = new ServiceBusClientBuilder()
            .connectionString(connectionProperties.toConnectionString())
            .processor()
            .queueName(connectionProperties.getEntityPath())
            .maxConcurrentCalls(maxConcurrentCalls)
            .maxAutoLockRenewDuration(maxAutoRenewDuration)
            .processMessage(context -> {
                ServiceBusMessage sendMsg = MessageRouter.toSendMessage(context.getMessage());
                handler.onMessageAsync(sendMsg).join();
            })
            .processError(context -> {
                ErrorPhase phase = ErrorPhase.UNKNOWN;
                ServiceBusErrorSource errorSource = context.getErrorSource();
                if (errorSource == ServiceBusErrorSource.RECEIVE) {
                    phase = ErrorPhase.RECEIVE;
                } else if (errorSource == ServiceBusErrorSource.COMPLETE) {
                    phase = ErrorPhase.COMPLETE;
                } else if (errorSource == ServiceBusErrorSource.ABANDON) {
                    phase = ErrorPhase.ABANDON;
                } else if (errorSource == ServiceBusErrorSource.RENEW_LOCK) {
                    phase = ErrorPhase.RENEW_LOCK;
                } else if (errorSource == ServiceBusErrorSource.ACCEPT_SESSION) {
                    phase = ErrorPhase.ACCEPT_SESSION;
                }
                handler.notifyException(context.getException(), phase);
            })
            .buildProcessorClient();

        client.start();
        activeQueues.put(connectionProperties.getEntityPath(), client);
        return client;
    }

    public ServiceBusProcessorClient createWithTransformer(
            ConnectionStringProperties connectionProperties,
            MessageTransformer transformer,
            MessageSender forwardSender) {
        OrderMessageHandler handler = new OrderMessageHandler(forwardSender, errorClassifier);
        handler.addTransformer(transformer);
        return createAndRegister(connectionProperties, handler, 1, Duration.ofMinutes(5));
    }

    public ServiceBusProcessorClient getQueue(String entityPath) {
        return activeQueues.get(entityPath);
    }

    public void closeAll() {
        for (Map.Entry<String, ServiceBusProcessorClient> entry : activeQueues.entrySet()) {
            try {
                entry.getValue().close();
            } catch (Exception e) {
                errorClassifier.classify(e);
            }
        }
        activeQueues.clear();
    }

    public int getActiveQueueCount() {
        return activeQueues.size();
    }
}
