# Event Hubs v3 Client

This project demonstrates how to send and receive events using Azure Event Hubs SDK v3 (`azure-eventhubs` and `azure-eventhubs-eph`).

## Features

- **Send Events**: Send individual messages, batches, and messages to specific partitions
- **Receive Events**: Receive events using Event Processor Host (EPH) with checkpointing
- **Configuration**: Flexible configuration via environment variables or code
- **Unit Tests**: Comprehensive test coverage using JUnit 4 and Mockito

## Project Structure

```
src/
├── main/java/com/microsoft/azure/eventhubs/
│   ├── EventHubsApplication.java     - Main application entry point
│   ├── EventHubSender.java           - Handles sending events to Event Hub
│   ├── EventHubReceiver.java         - Handles receiving events via EPH
│   └── EventHubConfiguration.java    - Configuration management
└── test/java/com/microsoft/azure/eventhubs/
    ├── EventHubSenderTest.java       - Unit tests for sender
    ├── EventHubReceiverTest.java     - Unit tests for receiver
    └── EventHubConfigurationTest.java - Unit tests for configuration
```

## Dependencies

- `azure-eventhubs` v3.3.0 - Core Event Hubs client library
- `azure-eventhubs-eph` v3.3.0 - Event Processor Host for scalable event processing
- `azure-storage` v8.6.6 - Storage for EPH checkpointing
- JUnit 4.13.2 and Mockito 4.11.0 for testing

## Configuration

The application can be configured using environment variables:

- `EVENT_HUB_CONNECTION_STRING` - Event Hub namespace connection string
- `EVENT_HUB_NAME` - Event Hub name
- `EVENT_HUB_CONSUMER_GROUP` - Consumer group (defaults to $Default)
- `STORAGE_CONNECTION_STRING` - Storage account connection string for checkpointing
- `STORAGE_CONTAINER_NAME` - Storage container name for checkpoints

## Building

```bash
mvn clean package
```

## Running Tests

```bash
mvn test
```

## Running the Application

```bash
mvn exec:java
```

Or run the assembled JAR:

```bash
java -jar target/eventhubs-v3-0.0.1-SNAPSHOT-jar-with-dependencies.jar
```

## Usage Examples

### Sending Events

```java
EventHubSender sender = new EventHubSender(connectionString, eventHubName);
sender.initialize();

// Send a single message
sender.sendMessage("Hello Event Hub!");

// Send a batch of messages
String[] messages = {"Message 1", "Message 2", "Message 3"};
sender.sendBatch(messages);

// Send to specific partition
sender.sendMessageToPartition("Partition message", "0");

sender.close();
```

### Receiving Events

```java
EventHubReceiver receiver = new EventHubReceiver(
    eventHubConnectionString,
    eventHubName,
    consumerGroupName,
    storageConnectionString,
    storageContainerName,
    "host-name");

// Set message handler
receiver.setMessageHandler(message -> {
    System.out.println("Received: " + message);
});

receiver.startReceiving();

// ... process events ...

receiver.stopReceiving();
```

## Key Concepts

### Event Processor Host (EPH)

EPH provides automatic partition management, load balancing, and checkpointing:

- **Partition Management**: Automatically distributes partitions across instances
- **Checkpointing**: Stores progress in Azure Storage for recovery
- **Load Balancing**: Rebalances partitions when instances are added or removed

### Sending Patterns

1. **Direct Send**: Messages distributed across partitions using round-robin
2. **Partition Key**: Related messages sent to same partition
3. **Specific Partition**: Target a particular partition directly
4. **Batching**: Efficient sending of multiple messages

## License

This project is provided as an example for migrating Azure SDK code.
