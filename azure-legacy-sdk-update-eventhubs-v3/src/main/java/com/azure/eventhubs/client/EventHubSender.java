package com.azure.eventhubs.client;

import com.microsoft.azure.eventhubs.ConnectionStringBuilder;
import com.microsoft.azure.eventhubs.EventData;
import com.microsoft.azure.eventhubs.EventDataBatch;
import com.microsoft.azure.eventhubs.EventHubClient;
import com.microsoft.azure.eventhubs.EventHubException;
import com.microsoft.azure.eventhubs.PartitionSender;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

/**
 * Sends events to Azure Event Hub using the v3 SDK.
 */
public class EventHubSender {
    
    private static final Logger logger = LoggerFactory.getLogger(EventHubSender.class);
    
    private final String connectionString;
    private final String eventHubName;
    private EventHubClient eventHubClient;
    private ScheduledExecutorService executorService;

    /**
     * Creates a new EventHubSender.
     *
     * @param connectionString the Event Hub namespace connection string
     * @param eventHubName the Event Hub name
     */
    public EventHubSender(String connectionString, String eventHubName) {
        this.connectionString = connectionString;
        this.eventHubName = eventHubName;
    }

    /**
     * Initializes the Event Hub client.
     *
     * @throws IOException if client initialization fails
     * @throws EventHubException if Event Hub specific error occurs
     */
    public void initialize() throws IOException, EventHubException {
        executorService = Executors.newScheduledThreadPool(4);
        
        ConnectionStringBuilder connStr = new ConnectionStringBuilder()
                .setNamespaceName(extractNamespace(connectionString))
                .setEventHubName(eventHubName)
                .setSasKeyName(extractKeyName(connectionString))
                .setSasKey(extractKey(connectionString));
        
        eventHubClient = EventHubClient.createFromConnectionStringSync(connStr.toString(), executorService);
        logger.info("Event Hub client initialized successfully");
    }

    /**
     * Sends a message to Event Hub.
     *
     * @param message the message to send
     * @throws EventHubException if sending fails
     */
    public void sendMessage(String message) throws EventHubException {
        if (eventHubClient == null) {
            throw new IllegalStateException("Event Hub client not initialized. Call initialize() first.");
        }
        
        byte[] payloadBytes = message.getBytes(StandardCharsets.UTF_8);
        EventData sendEvent = EventData.create(payloadBytes);
        
        eventHubClient.sendSync(sendEvent);
        logger.info("Message sent successfully: {}", message);
    }

    /**
     * Sends a message to a specific partition.
     *
     * @param message the message to send
     * @param partitionId the partition ID
     * @throws EventHubException if sending fails
     * @throws ExecutionException if execution fails
     * @throws InterruptedException if interrupted
     */
    public void sendMessageToPartition(String message, String partitionId) 
            throws EventHubException, ExecutionException, InterruptedException {
        if (eventHubClient == null) {
            throw new IllegalStateException("Event Hub client not initialized. Call initialize() first.");
        }
        
        PartitionSender sender = eventHubClient.createPartitionSenderSync(partitionId);
        
        byte[] payloadBytes = message.getBytes(StandardCharsets.UTF_8);
        EventData sendEvent = EventData.create(payloadBytes);
        sendEvent.getProperties().put("timestamp", Instant.now().toString());
        
        sender.sendSync(sendEvent);
        logger.info("Message sent to partition {}: {}", partitionId, message);
    }

    /**
     * Sends multiple messages in a batch.
     *
     * @param messages the messages to send
     * @throws EventHubException if sending fails
     */
    public void sendBatch(String[] messages) throws EventHubException {
        if (eventHubClient == null) {
            throw new IllegalStateException("Event Hub client not initialized. Call initialize() first.");
        }
        
        EventDataBatch batch = eventHubClient.createBatch();
        
        for (String message : messages) {
            EventData event = EventData.create(message.getBytes(StandardCharsets.UTF_8));
            if (!batch.tryAdd(event)) {
                // If batch is full, send it and create a new one
                eventHubClient.sendSync(batch);
                batch = eventHubClient.createBatch();
                batch.tryAdd(event);
            }
        }
        
        if (batch.getSize() > 0) {
            eventHubClient.sendSync(batch);
        }
        
        logger.info("Batch of {} messages sent successfully", messages.length);
    }

    /**
     * Closes the Event Hub client and releases resources.
     */
    public void close() {
        if (eventHubClient != null) {
            try {
                eventHubClient.closeSync();
                logger.info("Event Hub client closed");
            } catch (EventHubException e) {
                logger.error("Error closing Event Hub client", e);
            }
        }
        
        if (executorService != null) {
            executorService.shutdown();
        }
    }

    private String extractNamespace(String connectionString) {
        String[] parts = connectionString.split(";");
        for (String part : parts) {
            if (part.startsWith("Endpoint=")) {
                String endpoint = part.substring("Endpoint=sb://".length());
                return endpoint.substring(0, endpoint.indexOf("."));
            }
        }
        throw new IllegalArgumentException("Invalid connection string format");
    }

    private String extractKeyName(String connectionString) {
        String[] parts = connectionString.split(";");
        for (String part : parts) {
            if (part.startsWith("SharedAccessKeyName=")) {
                return part.substring("SharedAccessKeyName=".length());
            }
        }
        throw new IllegalArgumentException("Invalid connection string format");
    }

    private String extractKey(String connectionString) {
        String[] parts = connectionString.split(";");
        for (String part : parts) {
            if (part.startsWith("SharedAccessKey=")) {
                return part.substring("SharedAccessKey=".length());
            }
        }
        throw new IllegalArgumentException("Invalid connection string format");
    }
}
