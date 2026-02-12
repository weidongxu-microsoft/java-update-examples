package com.azure.servicebus.client;

import com.microsoft.azure.servicebus.ExceptionPhase;
import com.microsoft.azure.servicebus.IMessage;
import com.microsoft.azure.servicebus.IMessageHandler;
import com.microsoft.azure.servicebus.MessageHandlerOptions;
import com.microsoft.azure.servicebus.QueueClient;
import com.microsoft.azure.servicebus.ReceiveMode;
import com.microsoft.azure.servicebus.primitives.ConnectionStringBuilder;
import com.microsoft.azure.servicebus.primitives.ServiceBusException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;

/**
 * Receives messages from an Azure Service Bus queue using the v3 SDK.
 * Uses the push-based {@link IMessageHandler} model with asynchronous message
 * completion via {@link CompletableFuture}.
 */
public class ServiceBusReceiver {

    private static final Logger logger = LoggerFactory.getLogger(ServiceBusReceiver.class);

    private final String connectionString;
    private final String queueName;
    private final int maxConcurrentCalls;
    private QueueClient queueClient;
    private ExecutorService executorService;
    private Consumer<String> messageHandler;
    private Consumer<Throwable> errorHandler;

    public ServiceBusReceiver(String connectionString, String queueName) {
        this(connectionString, queueName, 3);
    }

    public ServiceBusReceiver(String connectionString, String queueName, int maxConcurrentCalls) {
        this.connectionString = connectionString;
        this.queueName = queueName;
        this.maxConcurrentCalls = maxConcurrentCalls;
    }

    public void setMessageHandler(Consumer<String> handler) {
        this.messageHandler = handler;
    }

    public void setErrorHandler(Consumer<Throwable> handler) {
        this.errorHandler = handler;
    }

    /**
     * Starts receiving messages using the {@link QueueClient} push model.
     * Messages are dispatched to the registered {@code messageHandler} callback.
     * Each message is explicitly completed (PEEKLOCK mode, auto-complete disabled).
     */
    public void startReceiving() throws ServiceBusException, InterruptedException {
        ConnectionStringBuilder connStr = new ConnectionStringBuilder(connectionString, queueName);
        queueClient = new QueueClient(connStr, ReceiveMode.PEEKLOCK);

        executorService = Executors.newWorkStealingPool(maxConcurrentCalls);

        MessageHandlerOptions options = new MessageHandlerOptions(
                maxConcurrentCalls, false, Duration.ofMinutes(5), Duration.ofSeconds(10));

        queueClient.registerMessageHandler(
                new ServiceBusMessageHandler(), options, executorService);

        logger.info("Started receiving messages from queue: {}", queueName);
    }

    /**
     * Stops receiving and releases the AMQP connection.
     */
    public void close() {
        if (queueClient != null) {
            try {
                queueClient.close();
                logger.info("Queue client closed");
            } catch (ServiceBusException e) {
                logger.error("Error closing queue client", e);
            }
        }
        if (executorService != null) {
            executorService.shutdown();
        }
    }

    /**
     * Inner handler that implements the v3 {@link IMessageHandler} contract.
     * {@code onMessageAsync} returns a {@link CompletableFuture} that completes
     * the message when processing succeeds, or abandons it on failure.
     */
    public class ServiceBusMessageHandler implements IMessageHandler {

        @Override
        public CompletableFuture<Void> onMessageAsync(IMessage message) {
            try {
                byte[] body = message.getBody();
                String content = (body != null) ? new String(body, StandardCharsets.UTF_8) : "";
                logger.info("Received message: id={}, body={}", message.getMessageId(), content);

                if (messageHandler != null) {
                    messageHandler.accept(content);
                }

                return queueClient.completeAsync(message.getLockToken());
            } catch (Exception e) {
                logger.error("Error processing message: id={}", message.getMessageId(), e);
                return queueClient.abandonAsync(message.getLockToken());
            }
        }

        @Override
        public void notifyException(Throwable exception, ExceptionPhase phase) {
            logger.error("Error in phase {}: {}", phase, exception.getMessage());
            if (errorHandler != null) {
                errorHandler.accept(exception);
            }
        }
    }
}
