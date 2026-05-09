# Complete Migration Guide: Track 1 → Track 2 Event Hubs Spark Connector

## Overview

This document provides a detailed guide for completing the migration of the Azure Event Hubs Spark Connector from Track 1 (`com.microsoft.azure:azure-eventhubs`) to Track 2 (`com.azure:azure-messaging-eventhubs`).

**Current Status:**
- Phase 1 ✅: Dependencies updated
- Phase 2 🟡: Partially started (EventPosition, package constants)
- Phases 3-5 🔲: Not started

---

## Architecture Changes: Track 1 vs Track 2

### Track 1 Architecture
- **Single EventHubClient** - manages all partitions and operations
- **Epoch Receivers** - exclusive receivers per partition (prevents concurrent access)
- **PartitionSender** - dedicated sender per partition
- **Synchronous APIs** - blocking method calls (`.sync()` suffixes)
- **Connection String Parsing** - custom implementation in `ConnectionStringBuilder`
- **Authentication** - via connection string, SAS keys, or AAD callback

### Track 2 Architecture
- **Separate Clients** - `EventHubsConsumerClient` and `EventHubsProducerClient`
- **Consumer Checkpoints** - offset/sequence number tracking (no exclusive receivers)
- **Producer Partitioning** - batch send with optional partition ID
- **Async-First Design** - `CompletableFuture` based (no `.sync()` calls)
- **Connection String Parsing** - Track 2 built-in via `ConnectionStringProperties`
- **Authentication** - via `TokenCredential` (azure-identity) + connection string or AMQP

---

## Detailed File-by-File Migration

### Priority 1: Core Client Files

#### 1. **ClientConnectionPool.scala** (HIGH PRIORITY)
**Current Track 1 Usage:**
```scala
import com.microsoft.azure.eventhubs.{ EventHubClient, EventHubClientOptions, RetryPolicy }

private var _client: EventHubClient = _

def getOrCreateClient(): EventHubClient = {
  EventHubClient.createSync(connectionString)
}
```

**Migration to Track 2:**
```scala
import com.azure.messaging.eventhubs.{ EventHubsClientBuilder, EventHubsProducerClient, EventHubsConsumerClient }
import com.azure.identity.DefaultAzureCredential

// Create producer
val producerClient = new EventHubsClientBuilder()
  .connectionString(connectionString)
  .buildProducerClient()

// Create consumer (with credential if needed)
val consumerClient = new EventHubsClientBuilder()
  .connectionString(connectionString)
  .buildConsumerClient()

// Or with DefaultAzureCredential
val credential = new DefaultAzureCredential()
val consumerClient = new EventHubsClientBuilder()
  .fullyQualifiedNamespace(namespace)
  .credential(credential)
  .consumerGroup(consumerGroup)
  .buildConsumerClient()
```

**Key Changes:**
- Replace `EventHubClient.createSync()` with `EventHubsClientBuilder`
- Split into separate producer/consumer clients
- Use `EventHubsClientBuilder` for all configuration
- Support `TokenCredential` from azure-identity
- No more `RetryPolicy` (Track 2 handles retries internally)

#### 2. **EventHubsClient.scala** (HIGHEST PRIORITY - Most Complex)
**Current Track 1 Code Pattern:**
```scala
private var _client: EventHubClient = _
private var partitionSender: PartitionSender = _

def send(event: EventData): Unit = {
  partitionSender.sendSync(event)
}

def receive(partition: Int, position: EventPosition): Seq[EventData] = {
  val receiver = client.createEpochReceiver(partition, position, epoch)
  receiver.receiveSync(batchSize)
}
```

**Migration to Track 2:**
```scala
// One producer client per connector instance
private var _producerClient: EventHubsProducerClient = _
// One consumer client per partition
private var _consumerClients: Map[Int, EventHubsConsumerClient] = Map()

def send(event: com.azure.messaging.eventhubs.EventData, partition: Option[Int]): Unit = {
  val batchOptions = new CreateBatchOptions()
  partition.foreach(p => batchOptions.setPartitionId(p.toString))
  
  val batch = _producerClient.createBatch(batchOptions)
  batch.tryAdd(event)
  _producerClient.send(batch)
}

def receive(partition: Int, position: EventPosition): Seq[com.azure.messaging.eventhubs.EventData] = {
  // Track 2: Use checkpoint-based or position-based receiving
  val consumer = _consumerClients.getOrElse(partition, {
    val client = new EventHubsClientBuilder()
      .connectionString(connectionString)
      .consumerGroup(consumerGroup)
      .buildConsumerClient()
    _consumerClients = _consumerClients + (partition -> client)
    client
  })
  
  // Position conversion: use the convert method from EventPosition
  val track2Position = position.convert // Already updated to Track 2
  val events = consumer.receiveFromPartition(partition, track2Position, batchSize, Duration.ofSeconds(30))
  events.toSeq
}
```

**Key Changes:**
- Replace `EventHubClient` with `EventHubsProducerClient` + `EventHubsConsumerClient`
- Remove epoch receiver logic (Track 2 doesn't use epochs)
- Use `CreateBatchOptions` for partition-aware sending
- Track 2 consumer returns an `IterableStream<EventData>`
- No more `.sync()` calls - Track 2 is async-first
- Exception handling: `EventHubException` → `AmqpException` or other Track 2 exceptions

**Exception Mapping (Important for RetryUtils.scala):**

Track 1:
```scala
case eh: EventHubException if eh.getIsTransient()
```

Track 2:
```scala
case ae: com.azure.core.amqp.exception.AmqpException =>
  // Check if transient based on error condition
  val isTransient = ae.getErrorCondition match {
    case "com.microsoft:server-busy" => true
    case "com.microsoft:operation-timeout" => true
    case _ => false
  }
  if (isTransient) { /* retry */ }

case _ => Throwable => // Other exceptions
```

#### 3. **ConnectionStringBuilder.scala**
**Current Implementation:**
- Custom parsing using Track 1's `StringUtil`
- Builds connection strings manually

**Migration to Track 2:**
```scala
import com.azure.messaging.eventhubs.{ ConnectionStringProperties, EventHubsClientBuilder }

// Track 2 provides built-in connection string parsing
val properties = new ConnectionStringProperties(connectionString)
val namespace = properties.getFullyQualifiedNamespace  // e.g., "mynamespace.servicebus.windows.net"
val eventHubName = properties.getEventHubName
val sharedAccessKeyName = properties.getSharedAccessKeyName
val sharedAccessKey = properties.getSharedAccessKey

// Building simplified
val builder = new EventHubsClientBuilder()
  .connectionString(connectionString)
  .consumerGroup(consumerGroup)
```

**Recommendation:**
- Use `ConnectionStringProperties` from Track 2 for parsing
- Simplify custom builder (Track 2's builder does most of the work)
- Validation can leverage Track 2's built-in checks

#### 4. **EventHubsConf.scala**
**Changes Needed:**
- Remove AAD callback support (Track 2 uses `TokenCredential`)
- Add support for `TokenCredential`
- Simplify configuration (Track 2 builder is more powerful)

```scala
import com.azure.identity.{ TokenCredential, DefaultAzureCredential, ClientSecretCredential }

class EventHubsConf {
  var connectionString: String = _
  var tokenCredential: Option[TokenCredential] = None  // NEW
  var consumerGroup: String = "$Default"
  
  def withTokenCredential(credential: TokenCredential): EventHubsConf = {
    this.tokenCredential = Some(credential)
    this
  }
}
```

---

### Priority 2: Supporting Files

#### 5. **EventHubsUtils.scala**
**Track 1:**
```scala
import com.microsoft.azure.eventhubs.{
  AmqpConstants,
  EventData,
  EventPosition
}
```

**Track 2:**
```scala
import com.azure.messaging.eventhubs.{
  EventData as track2EventData,
  EventPosition as track2EventPosition
}
// AmqpConstants mostly removed - use direct values instead
val PARTITION_KEY_PROP = "x-opt-partition-key"
```

#### 6. **RetryUtils.scala** (IMPORTANT)
**Critical Change:** Exception transience detection

Track 1: `eh.getIsTransient()` method
Track 2: Must check error condition codes

```scala
def isTransientError(exception: Throwable): Boolean = exception match {
  case ae: com.azure.core.amqp.exception.AmqpException =>
    ae.getErrorCondition match {
      case "com.microsoft:server-busy" => true
      case "com.microsoft:operation-timeout" => true
      case "com.microsoft:timeout" => true
      case _ => false
    }
  case ie: java.util.concurrent.TimeoutException => true
  case _ => false
}
```

#### 7. **Client.scala** (Interface)
**Update method signatures to match Track 2 patterns:**
```scala
trait Client {
  // Replace EventData with Track 2 version
  def send(event: com.azure.messaging.eventhubs.EventData, ...): Unit
  def receive(...): Seq[com.azure.messaging.eventhubs.EventData]
}
```

---

### Priority 3: Receiver/Sender Adapters

#### 8. **CachedEventHubsReceiver.scala**
**Remove epoch receiver logic, use consumer checkpoint patterns instead**

#### 9. **package.scala** ✅ (DONE)
Already updated with Track 2 constants

---

### Priority 4: Streaming & SQL Integration

#### 10. **EventHubsSource.scala** (Structured Streaming)
- Update to use `EventHubsConsumerClient` instead of `EventHubClient`
- Update checkpoint management to Track 2 patterns

#### 11. **EventHubsSink.scala** (Structured Streaming)
- Update to use `EventHubsProducerClient`

#### 12. **EventHubsDirectDStream.scala** (Spark Streaming)
- Update receiver creation to use Track 2 consumer client

#### 13. **EventHubsRDD.scala**
- Update partition reading to use Track 2 consumer client

---

### Priority 5: Test Files

#### Test Updates Needed:
1. **EventHubsClientSuite.scala**
   - Mock `EventHubsConsumerClient` / `EventHubsProducerClient`
   - Update assertions for async patterns

2. **EventHubsRDDSuite.scala**
   - Update partition reader tests

3. **EventHubsSinkSuite.scala**
4. **EventHubsSourceSuite.scala**
5. **EventHubsDirectDStreamSuite.scala**

---

## Key API Mappings

### Event Creation

**Track 1:**
```scala
new EventData(payload)
event.setProperties(props)
```

**Track 2:**
```scala
new com.azure.messaging.eventhubs.EventData(payload)
event.getProperties.put(key, value)
```

### Receiving Events

**Track 1:**
```scala
val receiver = client.createEpochReceiver(partitionId, position, epoch)
val events = receiver.receiveSync(100)
```

**Track 2:**
```scala
val consumer = new EventHubsClientBuilder()
  .connectionString(connStr)
  .buildConsumerClient()
val events = consumer.receiveFromPartition(partitionId, position, 100)
```

### Sending Events

**Track 1:**
```scala
val sender = client.createPartitionSenderSync(partitionId)
sender.sendSync(event)
```

**Track 2:**
```scala
val producer = new EventHubsClientBuilder()
  .connectionString(connStr)
  .buildProducerClient()
val batch = producer.createBatch(CreateBatchOptions().setPartitionId(partitionId))
batch.tryAdd(event)
producer.send(batch)
```

### Connection Management

**Track 1:**
```scala
client.getEventHubName()
client.getPartitionIds()
```

**Track 2:**
```scala
val props = new ConnectionStringProperties(connStr)
props.getEventHubName()
consumer.getPartitionIds() // or producer.getPartitionIds()
```

---

## Testing Strategy

### Unit Test Approach

1. **Mock Track 2 Clients:**
   ```scala
   val mockProducer = mock[EventHubsProducerClient]
   val mockConsumer = mock[EventHubsConsumerClient]
   ```

2. **Test Async Patterns:**
   - Use `CompletableFuture` test utilities
   - Create futures that complete successfully or with exceptions

3. **Exception Testing:**
   - Create `AmqpException` instances with various error conditions
   - Verify retry logic uses correct transience detection

### Integration Test Approach

- Use mock Event Hubs or Testcontainers
- Verify end-to-end scenarios with Track 2 clients

### Testing Environment

**IMPORTANT**: Build and tests require **JDK 8** (not Java 25) due to Scala 2.11 compatibility.

Workaround options:
1. Use JDK 8 for this project
2. Upgrade to Scala 2.12+ (major refactoring)
3. Use Docker container with Java 8 for testing

---

## Validation Checklist

After migration, verify:

- [ ] No `com.microsoft.azure.*` imports remain
- [ ] All Track 1 exception types replaced with Track 2 equivalents
- [ ] All `.sync()` method calls removed
- [ ] `EventHubClient` completely replaced with `EventHubsProducerClient` + `EventHubsConsumerClient`
- [ ] `PartitionSender` replaced with producer batch logic
- [ ] Epoch receiver logic removed (replaced with checkpoint-based approach)
- [ ] Compilation succeeds: `mvn clean compile`
- [ ] All unit tests pass: `mvn clean test`
- [ ] No deprecation warnings from Track 2 SDK
- [ ] Examples in documentation updated
- [ ] `MIGRATION_PLAN.md` marked complete

---

## Resources

- **Track 2 SDK Repo:** https://github.com/Azure/azure-sdk-for-java/tree/main/sdk/eventhubs/azure-messaging-eventhubs
- **Migration Guide:** https://github.com/Azure/azure-sdk-for-java/blob/main/sdk/eventhubs/azure-messaging-eventhubs/MIGRATION.md
- **API Reference:** https://docs.microsoft.com/en-us/java/api/com.azure.messaging.eventhubs
- **Azure Identity:** https://github.com/Azure/azure-sdk-for-java/tree/main/sdk/identity/azure-identity

---

## Common Pitfalls & Solutions

### Pitfall 1: Forgetting About Async Patterns
**Problem:** Trying to use `.get()` on every operation blocks threads
**Solution:** Use `CompletableFuture` chains or async-await patterns in Scala

### Pitfall 2: Exclusive Receivers Not Available
**Problem:** Expecting epoch receiver behavior from Track 2
**Solution:** Track 2 uses checkpoints; partition IDs alone suffice for receiving

### Pitfall 3: Exception Handling
**Problem:** Track 1's `getIsTransient()` doesn't exist in Track 2
**Solution:** Check `AmqpException.getErrorCondition()` for transience indicators

### Pitfall 4: Configuration Complexity
**Problem:** Over-complicated configuration
**Solution:** Leverage `EventHubsClientBuilder` for most settings

---

## Next Steps

1. Start with **ClientConnectionPool.scala** - enables client creation
2. Continue with **EventHubsClient.scala** - core logic
3. Update **RetryUtils.scala** - exception handling
4. Complete **other source files**
5. Update **test files**
6. Final validation and commit

Estimated effort: 3-5 days for experienced developer
