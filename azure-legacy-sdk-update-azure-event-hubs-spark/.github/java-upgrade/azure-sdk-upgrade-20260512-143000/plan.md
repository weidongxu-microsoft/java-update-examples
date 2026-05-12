# Upgrade Plan: azure-legacy-sdk-update-azure-event-hubs-spark (azure-sdk-upgrade-20260512-143000)

- **Generated**: May 12, 2026 14:30:00
- **HEAD Branch**: main
- **HEAD Commit ID**: (pulled from main)

## Available Tools

**JDKs**
- OpenJDK 25.0.1: C:\Program Files\Microsoft\jdk-25.0.1.8-hotspot (project JDK, used for all steps)

**Build Tools**
- Maven 3.9.11: C:\apache-maven-3.9.11

## Guidelines

- Migrate all `com.microsoft.azure.eventhubs.*` dependencies to `com.azure.messaging.eventhubs.*`
- Replace legacy authentication (msal4j) with `com.azure:azure-identity`
- Use azure-sdk-bom for centralized version management
- Preserve original package declarations in source files (no moving files)
- Maintain functional equivalence and behavior

## Upgrade Goals

- Replace all `com.microsoft.azure.eventhubs.*` dependencies with `com.azure.messaging.eventhubs.*` equivalents
- Migrate source code (Scala) to use modern Azure SDK APIs
- Migrate legacy authentication (msal4j) to azure-identity
- Maintain 100% test pass rate after migration

### Technology Stack

| Technology/Dependency | Current | Modern Equivalent | Migration Notes |
| --------------------- | ------- | ----------------- | --------------- |
| com.microsoft.azure:azure-eventhubs | 3.3.0 | com.azure:azure-messaging-eventhubs | Use azure-sdk-bom for version management |
| com.microsoft.azure:msal4j | 1.7.0 | com.azure:azure-identity | Use ClientSecretCredential or DefaultAzureCredential |
| Maven | 3.9.11 | - | Compatible with Java 25 |
| Scala | 2.11 | - | No changes required |
| Spark | 2.3.3 | - | No changes required |

### Derived Upgrades

- Add azure-sdk-bom (version 1.3.0+) for centralized version management of com.azure.* dependencies
- Replace msal4j with azure-identity for AAD authentication
- Update all imports from `com.microsoft.azure.eventhubs.*` to `com.azure.messaging.eventhubs.*` and related packages
- Update EventPosition imports to use `com.azure.messaging.eventhubs.models.EventPosition`

## Upgrade Steps

### Step 1: Upgrade Build Tools for Java 11+ Compatibility

- **Rationale**: Scala 2.11 and scala-maven-plugin 3.2.2 are incompatible with Java 11+, causing "object java.lang.Object not found" errors during compilation. Upgrading to Scala 2.12 and modern scala-maven-plugin is required to enable building with available Java versions. This is a prerequisite for the Azure SDK migration.
- **Changes to Make**:
  - [ ] Update parent pom.xml scala.binary.version from 2.11 to 2.12
  - [ ] Update parent pom.xml scala-maven-plugin from 3.2.2 to 4.8.1
  - [ ] Update Spark version from 2.3.3 to 2.4.8 (last version supporting Scala 2.12)
  - [ ] Update dependent versions (mockito, scalatest) for compatibility
  - [ ] Fix POM warnings about expressions in artifactIds
- **Verification**:
  - Command: `mvn clean compile test-compile`
  - JDK: OpenJDK 11.0.29 (or higher)
  - Expected: Compilation SUCCESS with no Scala reflection errors

### Step 1.5: Setup Baseline (Post Build Tool Upgrade)

- **Rationale**: Establish pre-upgrade compile and test results to measure Azure SDK upgrade success against (after build tools are working).
- **Changes to Make**:
  - [ ] Run baseline compilation after build tool upgrades
  - [ ] Run baseline tests with current JDK
  - [ ] Document current dependency tree
- **Verification**:
  - Command: `mvn clean compile test-compile && mvn clean test`
  - JDK: OpenJDK 11.0.29
  - Expected: Document SUCCESS/FAILURE and test pass rate (forms acceptance criteria)

### Step 2: Upgrade Maven Dependencies - Azure SDK and Authentication

- **Rationale**: Replace legacy com.microsoft.azure dependencies with modern com.azure equivalents. Start with dependency upgrades before code changes to isolate compilation errors.
- **Changes to Make**:
  - [ ] Add azure-sdk-bom (version 1.3.0+) to dependencyManagement section in parent pom.xml
  - [ ] Replace com.microsoft.azure:azure-eventhubs with com.azure:azure-messaging-eventhubs
  - [ ] Replace com.microsoft.azure:msal4j with com.azure:azure-identity
  - [ ] Run `mvn clean dependency:tree` to verify new dependencies
- **Verification**:
  - Command: `mvn clean test-compile`
  - JDK: OpenJDK 25.0.1
  - Expected: Compilation SUCCESS (code changes will follow in step 3)

### Step 3: Migrate Source Code Imports and EventData Usage

- **Rationale**: Update all imports and basic API usages in Scala source files to use modern SDK classes.
- **Changes to Make**:
  - [ ] Update imports: `com.microsoft.azure.eventhubs.EventData` → `com.azure.messaging.eventhubs.EventData`
  - [ ] Update imports: `com.microsoft.azure.eventhubs.EventHubException` → `com.azure.messaging.eventhubs.* exceptions`
  - [ ] Update imports: `com.microsoft.azure.eventhubs.EventPosition` → `com.azure.messaging.eventhubs.models.EventPosition`
  - [ ] Fix EventPosition usage patterns (factory methods remain similar)
  - [ ] Update authentication imports for msal4j → azure-identity
- **Verification**:
  - Command: `mvn clean test-compile`
  - JDK: OpenJDK 25.0.1
  - Expected: Compilation SUCCESS

### Step 4: Migrate Internal Implementation Classes and Complex APIs

- **Rationale**: Replace legacy internal implementation classes (EventHubClient, PartitionReceiver, etc.) with modern SDK equivalents. This is complex as internal APIs differ significantly.
- **Changes to Make**:
  - [ ] Replace `com.microsoft.azure.eventhubs.EventHubClient` usage with `com.azure.messaging.eventhubs.EventHubProducerAsyncClient` or `EventHubConsumerAsyncClient`
  - [ ] Replace `com.microsoft.azure.eventhubs.PartitionReceiver` with `EventHubConsumerAsyncClient`
  - [ ] Replace `com.microsoft.azure.eventhubs.EventHubClientOptions` with modern client builder configuration
  - [ ] Migrate internal test utilities from `com.microsoft.azure.eventhubs.impl.*` to modern equivalents
  - [ ] Update AAD authentication from `AzureActiveDirectoryTokenProvider.AuthenticationCallback` to `TokenCredential` implementations
  - [ ] Fix async/await patterns if needed for Scala interop
- **Verification**:
  - Command: `mvn clean test-compile`
  - JDK: OpenJDK 25.0.1
  - Expected: Compilation SUCCESS

### Step 5: Final Validation

- **Rationale**: Verify all upgrade goals met, project compiles successfully, and all tests pass. Perform iterative fixing of test failures.
- **Changes to Make**:
  - [ ] Verify no legacy `com.microsoft.azure.eventhubs.*` imports remain via grep
  - [ ] Verify no legacy `msal4j` imports remain
  - [ ] Verify azure-sdk-bom is used for version management
  - [ ] Full clean rebuild
  - [ ] Run full test suite and fix ALL test failures (iterative loop)
  - [ ] Resolve any remaining TODOs from previous steps
- **Verification**:
  - Command: `mvn clean test`
  - JDK: OpenJDK 25.0.1
  - Expected: Compilation SUCCESS + 100% tests pass (or ≥ baseline pass rate if pre-existing flaky tests)

## Key Challenges

- **Internal Implementation Classes**: Legacy SDK exposes internal implementation classes (`EventHubClient`, `PartitionReceiver`, `EventDataImpl`) that don't have direct modern equivalents. May require significant refactoring of test utilities and simulation code.

- **Authentication Pattern Changes**: Legacy code uses msal4j directly. Modern SDK prefers TokenCredential implementations. Need to refactor `AadAuthenticationCallback` to use modern Azure Identity patterns.

- **Async/Scala Interoperability**: Scala code may interact with Java async APIs differently. Need to verify Scala futures work correctly with modern Reactor-based async APIs.

- **Test Utilities Migration**: The `SimulatedEventHubs`, `SimulatedClient`, and related test utilities are built on legacy internal APIs. May need to rewrite these using mock frameworks or modern SDK testing patterns.

## Plan Review

This plan covers the complete migration of azure-event-hubs-spark from legacy Azure SDK (com.microsoft.azure.eventhubs) to modern SDK (com.azure.messaging.eventhubs). All source files are Scala, so migration focuses on import and API usage changes while preserving file locations and package declarations. The main complexity lies in migrating internal test utilities and authentication patterns.
