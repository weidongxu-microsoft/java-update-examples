# Migration Progress: Track 1 → Track 2 Upgrade

**Start Date:** 2026-05-09  
**Status:** IN PROGRESS

---

## Phase 1: Dependency Updates ✅ COMPLETE
- [x] Add azure-sdk-bom (1.2.11)
- [x] Replace azure-eventhubs:3.3.0 → azure-messaging-eventhubs
- [x] Replace msal4j → azure-identity
- **Commit:** 269f7dea (chore: upgrade dependencies to Track 2 SDKs)

---

## Phase 2: Source Code Migration 🔄 IN PROGRESS

### Step 2.1: Update Import Statements
**Files to update:**
- [ ] ConnectionStringBuilder.scala - Remove Track 1 imports
- [ ] EventHubsConf.scala - Remove Track 1 imports  
- [ ] EventHubsClient.scala - Update to use Track 2 client APIs
- [ ] EventHubsUtils.scala - Update utilities
- [ ] EventPosition.scala - Update position handling
- [ ] Client.scala - Update interface
- [ ] CachedEventHubsReceiver.scala - Refactor receiver logic
- [ ] ClientConnectionPool.scala - Update connection pooling
- [ ] All related classes in org/apache/spark/eventhubs/**
- [ ] All related classes in org/apache/spark/sql/eventhubs/**
- [ ] All related classes in org/apache/spark/streaming/eventhubs/**

### Step 2.2: Refactor Client Architecture
**Key Changes:**
- [ ] Replace EventHubClient → EventHubsClientBuilder / EventHubsConsumerClient
- [ ] Replace PartitionSender → EventHubsProducerClient
- [ ] Replace EventHubReceiver / PartitionReceiver → EventHubsConsumerClient
- [ ] Update authentication: connection string parsing → TokenCredential (via azure-identity)
- [ ] Replace sync methods with async CompletableFuture patterns
- [ ] Update exception handling: EventHubException → AmqpException / EventHubsException

### Step 2.3: Update Configuration & Connection Building
**Key Changes:**
- [ ] ConnectionStringBuilder - Parse/validate Track 2 connection string format
- [ ] EventHubsConf - Accept TokenCredential or connection string
- [ ] ClientConnectionPool - Use EventHubsClientBuilder patterns

---

## Phase 3: Test Updates 🔲 NOT STARTED
- [ ] Update test files for Track 2 APIs
- [ ] Fix EventHubsClientSuite
- [ ] Fix EventHubsRDDSuite
- [ ] Fix EventHubsSinkSuite
- [ ] Fix EventHubsSourceSuite
- [ ] Fix EventHubsDirectDStreamSuite
- [ ] Verify mock/simulation utilities

---

## Phase 4: Documentation & Examples 🔲 NOT STARTED
- [ ] Update examples/multiple-readers-example.md
- [ ] Update docs/ with new API patterns

---

## Phase 5: Build & Validation 🔲 NOT STARTED
- [ ] Compile without errors
- [ ] All tests pass
- [ ] Remove any lingering Track 1 references

---

## Known Issues & Blockers

### Scala/JDK Compatibility
- **Issue:** Project uses Scala 2.11 with Java 25 (incompatible)
- **Workaround:** Tests must run with JDK 8 or equivalent
- **Status:** Blocks test validation until JDK 8 available

### API Surface Changes
- **Track 1:** EventHubClient (singleton), PartitionSender, PartitionReceiver, epoch-based receivers
- **Track 2:** EventHubsClientBuilder, EventHubsConsumerClient, EventHubsProducerClient, checkpoint-based offsets
- **Impact:** Core client logic requires substantial refactoring
- **Migration Strategy:** Wrapper classes may help abstract differences

---

## Code Samples for Reference

### Track 1 Pattern
```scala
val client = EventHubClient.createSync(connectionString)
val receiver = client.createEpochReceiver(partitionId, epoch)
val events = receiver.receiveSync(100)
```

### Track 2 Pattern
```scala
val client = new EventHubsClientBuilder()
  .connectionString(connectionString)
  .buildConsumerClient()
val events = client.receiveFromPartition(partitionId, eventPosition, batchSize)
```

---

## Commit Strategy

Each logical step will be committed separately:
1. ✅ "chore: upgrade dependencies to Track 2 SDKs"
2. 🔄 "refactor: update imports to Track 2 packages" (coming)
3. 🔲 "refactor: migrate EventHubsClient to Track 2 patterns"
4. 🔲 "refactor: update connection and configuration handling"
5. 🔲 "test: update tests for Track 2 compatibility"
6. 🔲 "docs: update examples for Track 2 SDK"

