# Upgrade Summary: azure-legacy-sdk-update-azure-event-hubs-spark (azure-sdk-upgrade-20260512-143000)

- **Date**: May 12, 2026
- **Project**: azure-legacy-sdk-update-azure-event-hubs-spark (Azure Event Hubs Spark Connector)
- **Upgrade Scope**: Migrate from legacy Azure SDK (com.microsoft.azure.*) to modern Azure SDK (com.azure.*)

## Upgrade Result

### Status: PARTIAL - Build Tool Incompatibility BLOCKER

The migration of Azure SDK dependencies and imports was successfully started, but encountered a pre-existing build tool compatibility issue that prevents code compilation and testing verification.

### Completed Work

✅ **Dependency Updates (pom.xml)**
- Added azure-sdk-bom 1.3.0 for centralized dependency management
- Replaced com.microsoft.azure:azure-eventhubs 3.3.0 → com.azure:azure-messaging-eventhubs
- Replaced com.microsoft.azure:msal4j 1.7.0 → com.azure:azure-identity
- Updated build tools for Java 11+ support:
  - Scala 2.11 → 2.12
  - scala-maven-plugin 3.2.2 → 4.8.1
  - Spark 2.3.3 → 2.4.8
  - mockito 1.10.8 → 2.23.4
  - scalatest 3.0.3 → 3.2.15

✅ **Source Code Updates (Scala files)**
- Updated imports in 9+ source files:
  - `com.microsoft.azure.eventhubs.EventData` → `com.azure.messaging.eventhubs.EventData`
  - `com.microsoft.azure.eventhubs.EventPosition` → `com.azure.messaging.eventhubs.models.EventPosition`
- Added Java 11+ compatibility flags (--add-opens) to pom.xml

### Issues Encountered

❌ **Build Tool Incompatibility (BLOCKER)**
- **Problem**: Scala 2.12 compiler cannot find java.lang.Object when running on Java 11+
- **Error**: `scala.reflect.internal.MissingRequirementError: object java.lang.Object in compiler mirror not found`
- **Root Cause**: Scala 2.x requires specific Java reflection access that is blocked in Java 11+ module system
- **Available Java versions**: Java 11, Java 17, Java 25 (Java 8 unavailable - required for legacy build tools)
- **Mitigation Attempted**: Added --add-opens JVM flags; did not resolve the issue
- **Solution Required**: Either use Java 8 or upgrade to Scala 2.13+ with Spark 3.0+ (beyond Azure SDK migration scope)
- **Impact**: Cannot compile or run tests to verify migration

### Incomplete Work

⏳ **Client API Migration (Complex - Not Implemented)**
- **Reason**: Requires significant refactoring of client management layer
- **Affected Files**:
  - `EventHubsClient.scala` - Uses EventHubClient, EventHubClientImpl (internal APIs)
  - `ClientConnectionPool.scala` - Complex connection pooling with EventHubClientOptions
  - Authentication callback classes - Requires migration from AuthenticationCallback to TokenCredential
  - Simulated test utilities - Built on internal APIs no longer available in modern SDK
- **Scope**: ~20+ source files depend on legacy client APIs that don't have direct equivalents
- **Complexity**: Architectural redesign needed; not a simple API migration

## Tech Stack Changes

| Component | Before | After | Status |
|-----------|--------|-------|--------|
| Azure SDK Core | com.microsoft.azure:azure-eventhubs 3.3.0 | com.azure:azure-messaging-eventhubs | ✅ Updated |
| Azure Authentication | com.microsoft.azure:msal4j 1.7.0 | com.azure:azure-identity | ✅ Updated |
| Scala | 2.11 | 2.12 | ✅ Updated |
| Scala Compiler Plugin | 3.2.2 | 4.8.1 | ✅ Updated |
| Apache Spark | 2.3.3 | 2.4.8 | ✅ Updated |
| Java Target | 1.8 | 1.8 | ⚠️ Incompatible with available JDKs |

## Validation Checklist

- ❌ **Migrated project passes compilation** - BLOCKED by build tool incompatibility
- ❌ **All tests pass** - Cannot run due to compilation failure
- ✅ **No legacy SDK dependencies/references in pom.xml** - All dependency updates complete
- ⚠️ **No legacy SDK imports in source files** - EventData/EventPosition imports updated; AAD callback and client APIs not addressed
- ❌ **Build tool compatibility with JDK** - BLOCKER issue prevents resolution

## Commits

1. **e081f83a** - "Step 2: Update Azure SDK dependencies and imports to modern SDK - Partial"
   - Updated pom.xml dependencies and build tools
   - Updated EventData and EventPosition imports across multiple Scala files
   - Added build tool upgrade attempt for Java 11+ support
   - Documented AAD callback refactoring as deferred work

## Recommendations

### For This Project to Be Upgraded Successfully

1. **Option A (Recommended)**: Upgrade ecosystem simultaneously
   - Upgrade to Scala 2.13 (released 2019, fully supported)
   - Upgrade to Spark 3.0+ (released 2020, supports Scala 2.13)
   - This would modernize the project more comprehensively

2. **Option B (Alternative)**: Ensure Java 8 availability
   - Current environment only has Java 11, 17, 25
   - If Java 8 can be made available, original build tools would work
   - Would not address longer-term modernization needs

3. **Remaining Work After Build Tools Fixed**:
   - Refactor EventHubsClient.scala to use modern async client builders
   - Update ClientConnectionPool.scala for modern client lifecycle
   - Migrate AAD authentication callback to TokenCredential pattern
   - Rewrite test utilities using modern SDK patterns
   - Estimated effort: 8-16 hours for experienced Azure SDK developer

### General Observations

- This Spark connector project is ~8-10 years old and deeply integrated with legacy Azure SDK internals
- Simply upgrading dependencies is insufficient; the project architecture needs redesign
- The modern Azure SDK uses async/reactive patterns that differ significantly from legacy SDK's blocking APIs
- True modernization should include Java version update and Spark version upgrade as well

## Files Modified

- `pom.xml` - Dependency and build tool updates
- `core/src/main/scala/org/apache/spark/streaming/eventhubs/EventHubsDirectDStream.scala` - EventData import
- `core/src/test/scala/org/apache/spark/streaming/eventhubs/EventHubsDirectDStreamSuite.scala` - EventData import
- `core/src/main/scala/org/apache/spark/sql/eventhubs/EventHubsWriteTask.scala` - EventData import
- `core/src/main/scala/org/apache/spark/sql/eventhubs/EventHubsForeachWriter.scala` - EventData import
- `core/src/main/scala/org/apache/spark/eventhubs/utils/SimulatedEventHubs.scala` - EventData import
- `core/src/main/scala/org/apache/spark/eventhubs/utils/SimulatedClient.scala` - EventData import
- `core/src/main/scala/org/apache/spark/eventhubs/utils/SimulatedCachedReceiver.scala` - EventData import
- `core/src/main/scala/org/apache/spark/eventhubs/utils/EventHubsTestUtils.scala` - EventData/impl imports
- `core/src/main/scala/org/apache/spark/eventhubs/rdd/EventHubsRDD.scala` - EventData import
- `core/src/main/scala/org/apache/spark/eventhubs/utils/RetryUtils.scala` - EventPosition import
- `core/src/test/scala/org/apache/spark/eventhubs/EventPositionSuite.scala` - EventPosition usage
- `.github/java-upgrade/azure-sdk-upgrade-20260512-143000/plan.md` - Migration plan
- `.github/java-upgrade/azure-sdk-upgrade-20260512-143000/progress.md` - Progress tracking
- `.github/java-upgrade/azure-sdk-upgrade-20260512-143000/summary.md` - This file

## Next Steps

1. Resolve Java/Scala compatibility (enable Java 8 or upgrade Scala/Spark)
2. Implement client API refactoring (EventHubsClient → modern builders)
3. Complete AAD authentication migration
4. Update test utilities for modern SDK patterns
5. Full regression testing
6. Performance validation
