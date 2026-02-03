package com.azure.eventhubs.client;

import com.microsoft.azure.eventhubs.EventData;
import com.microsoft.azure.eventprocessorhost.CloseReason;
import com.microsoft.azure.eventprocessorhost.EventProcessorHost;
import com.microsoft.azure.eventprocessorhost.EventProcessorOptions;
import com.microsoft.azure.eventprocessorhost.IEventProcessor;
import com.microsoft.azure.eventprocessorhost.PartitionContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ExecutionException;
import java.util.function.Consumer;

/**
 * Receives events from Azure Event Hub using Event Processor Host (EPH).
 */
public class EventHubReceiver {
    
    private static final Logger logger = LoggerFactory.getLogger(EventHubReceiver.class);
    
    private final String eventHubConnectionString;
    private final String eventHubName;
    private final String consumerGroupName;
    private final String storageConnectionString;
    private final String storageContainerName;
    private final String hostName;
    private EventProcessorHost host;
    private Consumer<String> messageHandler;

    /**
     * Creates a new EventHubReceiver.
     *
     * @param eventHubConnectionString the Event Hub connection string
     * @param eventHubName the Event Hub name
     * @param consumerGroupName the consumer group name
     * @param storageConnectionString the Storage account connection string for checkpointing
     * @param storageContainerName the Storage container name for checkpointing
     * @param hostName the unique host name for this processor
     */
    public EventHubReceiver(String eventHubConnectionString, String eventHubName, 
                           String consumerGroupName, String storageConnectionString,
                           String storageContainerName, String hostName) {
        this.eventHubConnectionString = eventHubConnectionString;
        this.eventHubName = eventHubName;
        this.consumerGroupName = consumerGroupName;
        this.storageConnectionString = storageConnectionString;
        this.storageContainerName = storageContainerName;
        this.hostName = hostName;
    }

    /**
     * Sets the message handler to process received events.
     *
     * @param handler the consumer function to handle messages
     */
    public void setMessageHandler(Consumer<String> handler) {
        this.messageHandler = handler;
    }

    /**
     * Starts receiving events from Event Hub.
     *
     * @throws ExecutionException if registration fails
     * @throws InterruptedException if registration is interrupted
     */
    public void startReceiving() throws ExecutionException, InterruptedException {
        String hostNameStr = EventProcessorHost.createHostName(hostName);
        EventProcessorHost actualHost = EventProcessorHost.EventProcessorHostBuilder
                .newBuilder(hostNameStr, consumerGroupName)
                .useAzureStorageCheckpointLeaseManager(storageConnectionString, storageContainerName, null)
                .useEventHubConnectionString(eventHubConnectionString, eventHubName)
                .build();
        
        this.host = actualHost;

        logger.info("Registering Event Processor Host");
        
        EventProcessorOptions options = new EventProcessorOptions();
        options.setExceptionNotification(new ErrorNotificationHandler());
        
        actualHost.registerEventProcessor(EventProcessor.class, options)
                .whenComplete((unused, e) -> {
                    if (e != null) {
                        logger.error("Failed to register Event Processor", e);
                    } else {
                        logger.info("Event Processor Host registered successfully");
                    }
                })
                .get();
    }

    /**
     * Stops receiving events and unregisters the Event Processor Host.
     *
     * @throws ExecutionException if unregistration fails
     * @throws InterruptedException if unregistration is interrupted
     */
    public void stopReceiving() throws ExecutionException, InterruptedException {
        if (host != null) {
            logger.info("Unregistering Event Processor Host");
            host.unregisterEventProcessor()
                    .whenComplete((unused, e) -> {
                        if (e != null) {
                            logger.error("Failed to unregister Event Processor", e);
                        } else {
                            logger.info("Event Processor Host unregistered successfully");
                        }
                    })
                    .get();
        }
    }

    /**
     * Event processor implementation for handling received events.
     */
    public class EventProcessor implements IEventProcessor {
        private int checkpointBatchingCount = 0;

        @Override
        public void onOpen(PartitionContext context) throws Exception {
            logger.info("Partition {} is opening", context.getPartitionId());
        }

        @Override
        public void onClose(PartitionContext context, CloseReason reason) throws Exception {
            logger.info("Partition {} is closing with reason {}", context.getPartitionId(), reason);
        }

        @Override
        public void onError(PartitionContext context, Throwable error) {
            logger.error("Error on partition {}: {}", context.getPartitionId(), error.getMessage());
        }

        @Override
        public void onEvents(PartitionContext context, Iterable<EventData> events) throws Exception {
            logger.debug("Batch received on partition {}", context.getPartitionId());

            int eventCount = 0;
            for (EventData data : events) {
                String message = new String(data.getBytes(), "UTF8");
                logger.info("Received message from partition {}: {}", 
                        context.getPartitionId(), message);
                
                // Process the message using the handler if set
                if (messageHandler != null) {
                    messageHandler.accept(message);
                }
                
                eventCount++;
                checkpointBatchingCount++;
            }

            logger.debug("Received {} events from partition {}", eventCount, context.getPartitionId());

            // Checkpoint every 10 messages
            if (checkpointBatchingCount >= 10) {
                context.checkpoint().get();
                checkpointBatchingCount = 0;
                logger.debug("Checkpointed partition {}", context.getPartitionId());
            }
        }
    }

    /**
     * Error notification handler for Event Processor Host.
     */
    private static class ErrorNotificationHandler implements Consumer<com.microsoft.azure.eventprocessorhost.ExceptionReceivedEventArgs> {
        @Override
        public void accept(com.microsoft.azure.eventprocessorhost.ExceptionReceivedEventArgs exceptionReceivedEventArgs) {
            logger.error("Host {} received general error notification during {}: {}",
                    exceptionReceivedEventArgs.getHostname(),
                    exceptionReceivedEventArgs.getAction(),
                    exceptionReceivedEventArgs.getException().toString());
        }
    }
}
