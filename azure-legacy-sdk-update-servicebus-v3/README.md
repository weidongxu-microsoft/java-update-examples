# Azure Service Bus v3 Client

This project demonstrates sending and receiving messages using the Azure Service Bus v3 SDK (`com.microsoft.azure:azure-servicebus`).

## SDK Dependency

| Group ID | Artifact ID | Version |
|---|---|---|
| `com.microsoft.azure` | `azure-servicebus` | `3.6.7` |

## Features

- **Send messages** – synchronous and asynchronous (CompletableFuture) single-message sends via `IMessageSender`.
- **Batch send** – transmit a collection of messages in one AMQP transfer.
- **Scheduled delivery** – enqueue messages for future delivery with `scheduleMessageAsync`.
- **Push-based receive** – register an `IMessageHandler` on a `QueueClient` with explicit PEEKLOCK completion.
- **Error handling** – pluggable error callback for receive-side exceptions.

## Project Structure

```
src/main/java/com/azure/servicebus/client/
├── ServiceBusApplication.java   – entry point
├── ServiceBusConfiguration.java – environment-based config
├── ServiceBusSender.java        – send / batch / schedule
└── ServiceBusReceiver.java      – push-model receiver

src/test/java/com/azure/servicebus/client/
├── ServiceBusConfigurationTest.java
├── ServiceBusSenderTest.java
└── ServiceBusReceiverTest.java
```

## Build

```bash
mvn clean install
```

## Configuration

Set environment variables before running:

| Variable | Description |
|---|---|
| `SERVICE_BUS_CONNECTION_STRING` | Service Bus namespace connection string |
| `SERVICE_BUS_QUEUE_NAME` | Target queue name (default: `orders-queue`) |

## Run

```bash
mvn exec:java
```
