package com.azure.eventhubs.client;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Scanner;
import java.util.concurrent.ExecutionException;

/**
 * Main application class demonstrating Event Hubs send and receive operations.
 */
public class EventHubsApplication {
    
    private static final Logger logger = LoggerFactory.getLogger(EventHubsApplication.class);

    public static void main(String[] args) {
        logger.info("Event Hubs v3 Application Starting");
        
        // Configuration - in a real application, these would come from environment variables or config files
        EventHubConfiguration config = new EventHubConfiguration();
        
        if (!config.isValid()) {
            logger.error("Invalid configuration. Please set the required environment variables.");
            System.exit(1);
        }

        EventHubSender sender = null;
        EventHubReceiver receiver = null;

        try {
            // Initialize sender
            sender = new EventHubSender(config.getEventHubConnectionString(), config.getEventHubName());
            sender.initialize();
            logger.info("Sender initialized successfully");

            // Initialize receiver
            receiver = new EventHubReceiver(
                    config.getEventHubConnectionString(),
                    config.getEventHubName(),
                    config.getConsumerGroupName(),
                    config.getStorageConnectionString(),
                    config.getStorageContainerName(),
                    "eventhubs-processor-host");
            
            // Set up message handler
            receiver.setMessageHandler(message -> {
                logger.info("Processing received message: {}", message);
            });
            
            receiver.startReceiving();
            logger.info("Receiver started successfully");

            // Send some test messages
            logger.info("Sending test messages...");
            sender.sendMessage("Hello from Event Hubs v3!");
            sender.sendMessage("This is message number 2");
            sender.sendMessage("Processing events in real-time");

            // Send batch of messages
            String[] batchMessages = {
                "Batch message 1",
                "Batch message 2",
                "Batch message 3"
            };
            sender.sendBatch(batchMessages);

            // Send message to specific partition
            sender.sendMessageToPartition("Message for partition 0", "0");

            logger.info("All messages sent successfully. Press Enter to stop...");
            
            // Wait for user input
            Scanner scanner = new Scanner(System.in);
            scanner.nextLine();
            
            logger.info("Stopping application...");

        } catch (Exception e) {
            logger.error("Error in Event Hubs application", e);
        } finally {
            // Clean up resources
            if (receiver != null) {
                try {
                    receiver.stopReceiving();
                } catch (ExecutionException | InterruptedException e) {
                    logger.error("Error stopping receiver", e);
                }
            }
            
            if (sender != null) {
                sender.close();
            }
            
            logger.info("Application stopped");
        }
    }
}
