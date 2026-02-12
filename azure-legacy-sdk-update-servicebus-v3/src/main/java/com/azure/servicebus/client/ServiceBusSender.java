package com.azure.servicebus.client;

import com.microsoft.azure.servicebus.ClientFactory;
import com.microsoft.azure.servicebus.IMessage;
import com.microsoft.azure.servicebus.IMessageSender;
import com.microsoft.azure.servicebus.Message;
import com.microsoft.azure.servicebus.primitives.ConnectionStringBuilder;
import com.microsoft.azure.servicebus.primitives.ServiceBusException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

/**
 * Sends messages to an Azure Service Bus queue using the v3 SDK.
 * Supports both synchronous and asynchronous (CompletableFuture) send patterns,
 * batch sending, and scheduled message delivery.
 */
public class ServiceBusSender {

    private static final Logger logger = LoggerFactory.getLogger(ServiceBusSender.class);

    private final String connectionString;
    private final String queueName;
    private IMessageSender messageSender;

    public ServiceBusSender(String connectionString, String queueName) {
        this.connectionString = connectionString;
        this.queueName = queueName;
    }

    /**
     * Initializes the underlying AMQP connection and creates the message sender.
     */
    public void initialize() throws ServiceBusException, InterruptedException {
        ConnectionStringBuilder connStr = new ConnectionStringBuilder(connectionString, queueName);
        messageSender = ClientFactory.createMessageSenderFromConnectionStringBuilder(connStr);
        logger.info("Service Bus sender initialized for queue: {}", queueName);
    }

    /**
     * Sends a single text message synchronously.
     */
    public void sendMessage(String content) throws ServiceBusException, InterruptedException {
        ensureInitialized();
        Message message = createMessage(content);
        messageSender.send(message);
        logger.info("Message sent: id={}", message.getMessageId());
    }

    /**
     * Sends a single text message asynchronously using CompletableFuture.
     */
    public CompletableFuture<Void> sendMessageAsync(String content) {
        ensureInitialized();
        Message message = createMessage(content);
        return messageSender.sendAsync(message)
                .thenRun(() -> logger.info("Message sent async: id={}", message.getMessageId()));
    }

    /**
     * Sends a message with custom properties, label, and time-to-live.
     */
    public void sendMessageWithProperties(String content, String label, Duration timeToLive)
            throws ServiceBusException, InterruptedException {
        ensureInitialized();
        Message message = createMessage(content);
        message.setLabel(label);
        message.setTimeToLive(timeToLive);
        messageSender.send(message);
        logger.info("Message sent with label={}: id={}", label, message.getMessageId());
    }

    /**
     * Sends a batch of text messages synchronously.
     * The v3 SDK transmits the entire collection in one AMQP transfer.
     */
    public void sendBatch(List<String> contents) throws ServiceBusException, InterruptedException {
        ensureInitialized();
        Collection<Message> batch = contents.stream()
                .map(this::createMessage)
                .collect(Collectors.toList());
        messageSender.sendBatch(batch);
        logger.info("Batch of {} messages sent", contents.size());
    }

    /**
     * Sends a batch of text messages asynchronously.
     */
    public CompletableFuture<Void> sendBatchAsync(List<String> contents) {
        ensureInitialized();
        Collection<Message> batch = contents.stream()
                .map(this::createMessage)
                .collect(Collectors.toList());
        return messageSender.sendBatchAsync(batch)
                .thenRun(() -> logger.info("Batch of {} messages sent async", contents.size()));
    }

    /**
     * Schedules a message for future delivery and returns the sequence number asynchronously.
     */
    public CompletableFuture<Long> scheduleMessageAsync(String content, Instant scheduledEnqueueTime) {
        ensureInitialized();
        Message message = createMessage(content);
        return messageSender.scheduleMessageAsync(message, scheduledEnqueueTime)
                .thenApply(sequenceNumber -> {
                    logger.info("Message scheduled: id={}, sequenceNumber={}, enqueueTime={}",
                            message.getMessageId(), sequenceNumber, scheduledEnqueueTime);
                    return sequenceNumber;
                });
    }

    /**
     * Cancels a previously scheduled message by its sequence number.
     */
    public CompletableFuture<Void> cancelScheduledMessageAsync(long sequenceNumber) {
        ensureInitialized();
        return messageSender.cancelScheduledMessageAsync(sequenceNumber)
                .thenRun(() -> logger.info("Scheduled message cancelled: sequenceNumber={}", sequenceNumber));
    }

    public void close() {
        if (messageSender != null) {
            try {
                messageSender.close();
                logger.info("Service Bus sender closed");
            } catch (ServiceBusException e) {
                logger.error("Error closing Service Bus sender", e);
            }
        }
    }

    private void ensureInitialized() {
        if (messageSender == null) {
            throw new IllegalStateException("Sender not initialized. Call initialize() first.");
        }
    }

    private Message createMessage(String content) {
        Message message = new Message(content);
        message.setMessageId(UUID.randomUUID().toString());
        return message;
    }
}
