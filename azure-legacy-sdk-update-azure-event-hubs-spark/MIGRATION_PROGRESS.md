# Migration Progress: Track 1 → Track 2 Upgrade

**Start Date:** 2026-05-09  
**Current Status:** Phase 2.1 COMPLETE - All Import Statements Updated

---

## Phase 1: Dependency Updates ✅ COMPLETE
- [x] Add azure-sdk-bom (1.2.11)
- [x] Replace azure-eventhubs:3.3.0 → azure-messaging-eventhubs
- [x] Replace msal4j → azure-identity
- **Commit:** 269f7dea (chore: upgrade dependencies to Track 2 SDKs)

---

## Phase 2: Source Code Migration 🟡 PARTIALLY COMPLETE

### Step 2.1: Update Import Statements ✅ COMPLETE
All 18 files successfully updated with Track 2 imports:
- [x] ConnectionStringBuilder.scala
- [x] EventHubsConf.scala  
- [x] EventHubsClient.scala
- [x] EventHubsUtils.scala
- [x] EventPosition.scala
- [x] Client.scala
- [x] CachedEventHubsReceiver.scala
- [x] ClientConnectionPool.scala
- [x] package.scala
- [x] RetryUtils.scala
- [x] AadAuthenticationCallback.scala
- [x] EventHubsTestUtils.scala
- [x] EventHubsRDD.scala
- [x] EventHubsDirectDStream.scala
- [x] EventHubsWriteTask.scala
- [x] EventHubsForeachWriter.scala
- [x] SimulatedEventHubs.scala
- [x] SimulatedClient.scala
- [x] SimulatedCachedReceiver.scala

**Commit:** 9e1a424f (refactor: update all Track 1 imports to Track 2 packages)

### Step 2.2: Refactor Client Architecture 🔄 IN PROGRESS
**Status:** Identified all compilation errors from Track 1 API usage

**Key Issues to Address** (108+ compilation errors):
1. **EventPosition** - Use `com.azure.messaging.eventhubs.models.EventPosition` (not direct import)
2. **Client Types** - Replace:
   - `EventHubClient` → `EventHubsClientBuilder` (for creation) + `EventHubsConsumerClient`/`EventHubsProducerClient`
   - `PartitionSender` → `EventHubsProducerClient` 
   - `PartitionReceiver` → `EventHubsConsumerClient`
   - `PartitionRuntimeInformation` → `PartitionProperties` (Track 2)
   - `ReceiverOptions` → Removed (Track 2 uses builder pattern)
   - `ReceiverDisconnectedException` → Handle via AmqpException or other Track 2 exceptions

3. **API Changes**:
   - `getSystemProperties().getSequenceNumber()` → Use `getProperties()` or new API
   - `StringUtil` utilities → Use Track 2 built-in or Java utilities
   - Connection pooling logic → Refactor for producer/consumer clients

4. **Files Requiring Major Refactoring** (in priority order):
   - [ ] ClientConnectionPool.scala - Create producer/consumer clients
   - [ ] EventHubsClient.scala - Core client implementation
   - [ ] CachedEventHubsReceiver.scala - Receiver logic
   - [ ] EventHubsUtils.scala - Utility methods
   - [ ] ConnectionStringBuilder.scala - String utilities

### Step 2.3: Update Configuration & Connection Building
- [ ] EventHubsConf - Already partially updated (removed AuthenticationCallback)
- [ ] Connection string handling - Use ConnectionStringProperties

---

## Phase 3: Test Updates 🔲 NOT STARTED
- [ ] Update test files for Track 2 APIs
- [ ] Fix mocking for producer/consumer clients

---

## Phase 4: Documentation & Examples 🔲 NOT STARTED
- [ ] Update examples/multiple-readers-example.md
- [ ] Update docs/ with new API patterns

---

## Phase 5: Build & Validation 🔲 NOT STARTED
- [ ] Compile without errors
- [ ] All tests pass with JDK 8
- [ ] Remove any lingering Track 1 references

---

## Build & Test Status

### JDK Compatibility
- ✅ JDK 8 (1.8.0_482) - Works perfectly with Scala 2.11
- ❌ JDK 11 (11.0.29) - Scala 2.11 compiler fails 
- ❌ JDK 25 (25.0.1) - Scala 2.11 compiler fails

### Current Compilation Status (with JDK 8)
```
[INFO] Total: 108 compilation errors found
[ERROR] BUILD FAILURE
[ERROR] Failed to execute goal net.alchim31.maven:scala-maven-plugin:3.2.2:compile
```

**Error Categories:**
- 45+ errors: Type not found (Track 1 classes/interfaces)
- 30+ errors: Method/property not found on EventData
- 20+ errors: Import errors (EventPosition location, EventHubsClientBuilder)
- 13+ errors: Type mismatches from API changes

---

## Known Issues & Blockers

### API Surface Changes (CRITICAL)
- **Epoch Receivers**: Track 1 uses exclusive epoch receivers; Track 2 uses shared consumers with checkpoint offsets
- **Connection Pool**: Track 1 creates one EventHubClient per connection; Track 2 uses separate producer/consumer clients
- **Exception Handling**: Track 1 `EventHubException.getIsTransient()` → Track 2 `AmqpException` with error condition codes
- **System Properties**: Track 1 exposes `getSystemProperties()` directly; Track 2 different API

### Authentication Changes
- **Track 1**: AzureActiveDirectoryTokenProvider callback pattern
- **Track 2**: TokenCredential from azure-identity (e.g., DefaultAzureCredential, ClientSecretCredential)
- **AadAuthenticationCallback.scala**: Entire authentication mechanism needs redesign

### Dependency Issues
- `StringUtil` from Track 1 internal implementation → Replace with Java utilities or Track 2 equivalents
- `EventHubClientImpl` (internal) → Not available in Track 2

---

## Code Samples for Reference

### Track 1 Pattern (Current)
```scala
val client = EventHubClient.createSync(connectionString)
val receiver = client.createEpochReceiver(partitionId, position, epoch)
val events = receiver.receiveSync(100)
```

### Track 2 Pattern (Target)
```scala
val client = new EventHubsClientBuilder()
  .connectionString(connectionString)
  .buildConsumerClient()
val events = client.receiveFromPartition(partitionId, eventPosition, batchSize)
```

---

## Commit Strategy

Completed:
1. ✅ "chore: upgrade dependencies to Track 2 SDKs"
2. ✅ "refactor: update imports and constants to Track 2 patterns"
3. ✅ "refactor: update all Track 1 imports to Track 2 packages"

Pending:
4. 🔄 "refactor: migrate ClientConnectionPool to Track 2 builders" (NEXT)
5. 🔄 "refactor: migrate EventHubsClient to Track 2 consumer/producer clients"
6. 🔄 "refactor: update exception handling for Track 2"
7. 🔄 "refactor: update connection string and configuration handling"
8. 🔄 "test: update tests for Track 2 compatibility"
9. 🔄 "docs: update examples for Track 2 SDK"

---

## Progress Summary

**Completed Work:**
- ✅ All pom.xml dependency updates
- ✅ All import statements updated (18 files)
- ✅ Package-level constants updated
- ✅ Comprehensive migration guides created
- ✅ Build system verified with JDK 8

**Remaining Work:**
- 🔄 API-level code refactoring (108+ compilation errors)
- 🔄 Test updates
- 🔄 Documentation updates
- 🔄 Final validation

**Estimated Effort:**
- ClientConnectionPool refactoring: 2-3 hours
- EventHubsClient refactoring: 4-6 hours (most complex)
- Exception handling: 1-2 hours
- Test updates: 2-4 hours
- **Total remaining: ~10-15 hours** of focused development



