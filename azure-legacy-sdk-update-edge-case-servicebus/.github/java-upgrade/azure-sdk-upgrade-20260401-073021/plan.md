# Upgrade Plan: servicebus-order-processor (azure-sdk-upgrade-20260401-073021)

- **Generated**: 2026-04-01T07:30:21Z
- **HEAD Branch**: java-upgrade/azure-sdk-upgrade-20260401-073021
- **HEAD Commit ID**: 2a45ab39

## Available Tools

**JDKs**
- JDK 17.0.17: C:\Users\xiaofeicao\.jdks\jdk-21.0.6+7 (default JDK via JAVA_HOME, used by all steps)

**Build Tools**
- Maven 3.8.1: C:\Users\xiaofeicao\.maven\apache-maven-3.8.1 (system installation)

## Guidelines

> Note: You can add any specific guidelines or constraints for the upgrade process here if needed, bullet points are preferred.

- Follow the migration guide at https://aka.ms/azsdk/java/migrate/sb for API mappings
- Use `ServiceBusFailureReason` enum instead of individual exception subclasses
- Replace `IMessage`/`Message` with `ServiceBusMessage` for sending and transformation
- Replace `ConnectionStringBuilder` with plain connection strings (no equivalent in modern SDK)
- `message.getLabel()` → `message.getSubject()`; `message.getProperties()` → `message.getApplicationProperties()`
- `message.getBody()` returning `byte[]` → `message.getBody()` returning `BinaryData`
- `MessageBody`/`MessageBodyType` have no equivalent in modern SDK; simplify to `BinaryData`
- `IMessageHandler` replaced by consumer callbacks; `ExceptionPhase` → `ServiceBusErrorSource`
- For each legacy client using `ProviderRegistrationInterceptor`: ServiceBusManager is a premium client, so no need for `ProviderRegistrationPolicy`

## Upgrade Goals

- Replace all `com.microsoft.azure.*` dependencies with `com.azure.*` equivalents
- Migrate source code to use modern Azure SDK APIs (builder pattern, BinaryData, ServiceBusFailureReason)
- Maintain functional equivalence — same message routing, transformation, caching, and error classification behavior

### Technology Stack

| Technology/Dependency | Current | Modern Equivalent | Migration Notes |
| --------------------- | ------- | ----------------- | --------------- |
| com.microsoft.azure:azure-servicebus | 3.6.7 | com.azure:azure-messaging-servicebus | Use azure-sdk-bom for version management |
| com.fasterxml.jackson.core:jackson-databind | 2.13.5 | - | Keep as-is |
| org.slf4j:slf4j-api | 1.7.36 | - | Keep as-is |
| org.slf4j:slf4j-simple | 1.7.36 | - | Keep as-is |
| junit:junit | 4.13.2 | - | Keep as-is |
| org.mockito:mockito-core | 4.11.0 | - | Keep as-is |
| org.mockito:mockito-inline | 4.11.0 | - | Keep — needed to mock final classes in modern SDK |
| maven-compiler-plugin | 3.11.0 | - | Keep as-is |
| maven-surefire-plugin | 3.0.0 | - | Keep as-is |
| Maven (system) | 3.8.1 | - | Compatible with JDK 17 |

### Derived Upgrades

- Add `com.azure:azure-sdk-bom:1.3.5` for centralized version management
- Replace `com.microsoft.azure:azure-servicebus:3.6.7` with `com.azure:azure-messaging-servicebus` (version managed by BOM)

## Upgrade Steps

- **Step 1: Setup Baseline**
  - **Rationale**: Establish pre-upgrade compile and test results to measure upgrade success against.
  - **Changes to Make**:
    - [ ] Run baseline compilation with current JDK
    - [ ] Run baseline tests with current JDK
  - **Verification**:
    - Command: `mvn clean test-compile -q && mvn clean test`
    - JDK: C:\Users\xiaofeicao\.jdks\jdk-21.0.6+7
    - Expected: Document SUCCESS/FAILURE, test pass rate

- **Step 2: Migrate Azure Service Bus SDK**
  - **Rationale**: Replace legacy `com.microsoft.azure:azure-servicebus` with modern `com.azure:azure-messaging-servicebus`. Requires updating all source and test files simultaneously since API surface changes completely.
  - **Changes to Make**:
    - [ ] Update pom.xml: add azure-sdk-bom, replace azure-servicebus with azure-messaging-servicebus
    - [ ] Migrate ErrorClassifier: use ServiceBusException with ServiceBusFailureReason instead of exception subclasses
    - [ ] Migrate MessageTransformer: change from IMessage to ServiceBusMessage
    - [ ] Migrate MessageCache: change from IMessage/Message to ServiceBusMessage
    - [ ] Migrate MessageInspector: remove MessageBody/MessageBodyType, use BinaryData
    - [ ] Migrate MessageSerializer: adapt serialization for ServiceBusMessage/BinaryData
    - [ ] Migrate MessageRouter: use ServiceBusSenderClient/ServiceBusReceiverClient
    - [ ] Migrate OrderMessageHandler: remove IMessageHandler, use consumer callbacks pattern
    - [ ] Migrate QueueSessionManager: use ServiceBusProcessorClient instead of QueueClient
    - [ ] Migrate ServiceBusClients: use ServiceBusClientBuilder, replace ConnectionStringBuilder
    - [ ] Migrate Application: update to use new client APIs
    - [ ] Migrate all 8 test files for the above changes
  - **Verification**:
    - Command: `mvn clean test-compile -q`
    - JDK: C:\Users\xiaofeicao\.jdks\jdk-21.0.6+7
    - Expected: Compilation SUCCESS

- **Step 3: Final Validation**
  - **Rationale**: Verify all upgrade goals met, project compiles successfully, all tests pass.
  - **Changes to Make**:
    - [ ] Verify no legacy com.microsoft.azure.* dependencies remain
    - [ ] Resolve ALL TODOs and temporary workarounds from previous steps
    - [ ] Clean rebuild with current JDK
    - [ ] Fix any remaining compilation errors
    - [ ] Run full test suite and fix ALL test failures (iterative fix loop until 100% pass)
  - **Verification**:
    - Command: `mvn clean test`
    - JDK: C:\Users\xiaofeicao\.jdks\jdk-21.0.6+7
    - Expected: Compilation SUCCESS + 100% tests pass (67/67)

## Key Challenges

- **MessageBody/MessageBodyType Removal**: Legacy SDK has BINARY, VALUE, SEQUENCE body types; modern SDK uses only BinaryData. MessageInspector and MessageSerializer need significant rework. Tests that verify body type behavior must be adapted.
  - **Strategy**: Simplify body handling to BinaryData. Keep bodyType field in serialized JSON for compatibility but always treat as string content.

- **IMessage Interface Elimination**: Legacy IMessage is used everywhere (transformers, cache, router, handler). Modern SDK has separate ServiceBusMessage (mutable, for sending) and ServiceBusReceivedMessage (immutable, received).
  - **Strategy**: Use ServiceBusMessage throughout for transformers and cache. Convert ServiceBusReceivedMessage to ServiceBusMessage when needed for transformation.

- **Exception Hierarchy Flattening**: Legacy SDK has specific exception subclasses (MessageLockLostException, etc.). Modern SDK uses single ServiceBusException with ServiceBusFailureReason enum.
  - **Strategy**: Change instanceof checks to switch on ServiceBusFailureReason. Tests create exceptions with specific reasons.

- **ConnectionStringBuilder Removal**: No equivalent in modern SDK.
  - **Strategy**: Use plain strings and provide utility methods for parsing connection string components.

- **CompletableFuture vs Reactor**: Modern SDK uses Project Reactor (Mono/Flux) instead of CompletableFuture. MessageRouter uses CompletableFuture extensively.
  - **Strategy**: Keep CompletableFuture-based APIs by wrapping sync client calls in CompletableFuture.supplyAsync(). Tests mock sync clients.
