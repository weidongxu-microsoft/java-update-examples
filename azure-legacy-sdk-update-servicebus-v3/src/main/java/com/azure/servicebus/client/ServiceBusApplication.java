package com.azure.servicebus.client;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.util.Arrays;
import java.util.Scanner;

/**
 * Demonstrates sending and receiving Azure Service Bus messages with the v3 SDK.
 */
public class ServiceBusApplication {

    private static final Logger logger = LoggerFactory.getLogger(ServiceBusApplication.class);

    public static void main(String[] args) {
        logger.info("Service Bus v3 Application Starting");

        ServiceBusConfiguration config = new ServiceBusConfiguration();
        if (!config.isValid()) {
            logger.error("Invalid configuration. Set SERVICE_BUS_CONNECTION_STRING and SERVICE_BUS_QUEUE_NAME.");
            System.exit(1);
        }

        ServiceBusSender sender = null;
        ServiceBusReceiver receiver = null;

        try {
            // Initialize sender
            sender = new ServiceBusSender(config.getConnectionString(), config.getQueueName());
            sender.initialize();

            // Initialize receiver with message handler
            receiver = new ServiceBusReceiver(config.getConnectionString(), config.getQueueName());
            receiver.setMessageHandler(message ->
                    logger.info("Processing received message: {}", message));
            receiver.setErrorHandler(error ->
                    logger.error("Receiver error: {}", error.getMessage()));
            receiver.startReceiving();

            // Send individual messages
            sender.sendMessage("Order placed: ORD-1001");
            sender.sendMessage("Order placed: ORD-1002");

            // Send a message with custom properties
            sender.sendMessageWithProperties(
                    "Priority order: ORD-1003",
                    "high-priority",
                    java.time.Duration.ofMinutes(30));

            // Async send
            sender.sendMessageAsync("Order update: ORD-1001 shipped")
                    .thenRun(() -> logger.info("Async send completed"))
                    .exceptionally(error -> {
                        logger.error("Async send failed", error);
                        return null;
                    })
                    .get();

            // Batch send
            sender.sendBatch(Arrays.asList(
                    "Inventory update: SKU-A restocked",
                    "Inventory update: SKU-B restocked",
                    "Inventory update: SKU-C restocked"));

            // Schedule a message for future delivery
            sender.scheduleMessageAsync("Reminder: check pending orders", Instant.now().plusSeconds(60))
                    .thenAccept(seqNum -> logger.info("Scheduled message sequence number: {}", seqNum))
                    .get();

            logger.info("All messages sent successfully. Press Enter to stop...");
            new Scanner(System.in).nextLine();

        } catch (Exception e) {
            logger.error("Error in Service Bus application", e);
        } finally {
            if (receiver != null) {
                receiver.close();
            }
            if (sender != null) {
                sender.close();
            }
            logger.info("Application stopped");
        }
    }
}
