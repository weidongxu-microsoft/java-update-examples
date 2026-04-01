# Upgrade Plan: servicebus-order-processor (azure-sdk-upgrade-20260401-075625)

- **Generated**: 2026-04-01T07:56:25Z
- **HEAD Branch**: java-upgrade/azure-sdk-upgrade-20260401-073021
- **HEAD Commit ID**: bef01ceb2b9e967cbd13431e83ffa2691fcea9a5

## Available Tools

**JDKs**
- JDK 17.0.17: Default `java`/`javac` (Temurin-17.0.17+10)

**Build Tools**
- Maven 3.8.1: C:\Users\xiaofeicao\.maven\apache-maven-3.8.1

## Guidelines

- Migration guide: https://aka.ms/azsdk/java/migrate/sb
- ServiceBusException, ServiceBusSenderClient, ServiceBusReceiverClient, ServiceBusReceivedMessage, ServiceBusProcessorClient are all FINAL — cannot be mocked with plain Mockito. Use mockito-inline (already present) or wrapper interfaces.
- Modern SDK splits IMessage into ServiceBusMessage (sending) and ServiceBusReceivedMessage (receiving) — no shared type hierarchy.
- MessageBody/MessageBodyType removed — create application-level replacements.
- ConnectionStringBuilder removed — create application-level parser.
- ExceptionPhase replaced by ServiceBusErrorSource (similar but different names).
- Exception subclasses replaced by ServiceBusException with ServiceBusFailureReason enum.
- `getLabel()` → `getSubject()`, `getProperties()` → `getApplicationProperties()`, `getBody()` returns BinaryData not byte[].

> Note: You can add any specific guidelines or constraints for the upgrade process here if needed, bullet points are preferred.

## Upgrade Goals

- Replace `com.microsoft.azure:azure-servicebus:3.6.7` with `com.azure:azure-messaging-servicebus` (via azure-sdk-bom 1.3.5)
- Migrate all source code to modern Azure SDK APIs
- Create application-level abstractions for removed types (MessageBody, MessageBodyType, ConnectionStringBuilder)
- Create wrapper interfaces for final SDK classes to maintain testability
- Achieve 100% test pass rate (67/67 baseline)

### Technology Stack

| Technology/Dependency | Current | Modern Equivalent | Migration Notes |
| --------------------- | ------- | ----------------- | --------------- |
| com.microsoft.azure:azure-servicebus | 3.6.7 | com.azure:azure-messaging-servicebus | Core migration target |
| com.fasterxml.jackson.core:jackson-databind | 2.13.5 | - | Keep, used for serialization |
| org.slf4j:slf4j-api | 1.7.36 | - | Keep |
| org.slf4j:slf4j-simple | 1.7.36 | - | Keep |
| junit:junit | 4.13.2 | - | Keep |
| org.mockito:mockito-core | 4.11.0 | - | Keep |
| org.mockito:mockito-inline | 4.11.0 | - | Keep, needed for mocking final classes |
| maven-compiler-plugin | 3.11.0 | - | Keep |
| maven-surefire-plugin | 3.0.0 | - | Keep |

### Derived Upgrades

- Add azure-sdk-bom 1.3.5 for centralized version management
- Create application-level MessageBodyType enum (replaces removed SDK enum)
- Create application-level MessageBody class (replaces removed SDK class)
- Create ConnectionStringProperties class (replaces removed ConnectionStringBuilder)
- Create MessageSender/MessageReceiver wrapper interfaces (for testability with final SDK classes)
- Create ErrorPhase enum (replaces removed ExceptionPhase)
- Create MessageHandler interface (replaces removed IMessageHandler)

## Upgrade Steps

- Step 1: Setup Baseline
  - **Rationale**: Establish pre-upgrade compile and test results.
  - **Changes to Make**:
    - [ ] Run baseline compilation with current JDK
    - [ ] Run baseline tests with current JDK
  - **Verification**:
    - Command: `mvn clean test-compile -q && mvn clean test -q`
    - JDK: JDK 17
    - Expected: Document SUCCESS/FAILURE, test pass rate

---

- Step 2: Update pom.xml and Create Application-Level Abstractions
  - **Rationale**: Replace legacy dependency with modern SDK, create abstractions for removed types before migrating source code.
  - **Changes to Make**:
    - [ ] Replace com.microsoft.azure:azure-servicebus with com.azure:azure-messaging-servicebus via azure-sdk-bom
    - [ ] Create MessageBodyType.java enum (BINARY, VALUE, SEQUENCE)
    - [ ] Create MessageBody.java class with factory methods
    - [ ] Create ConnectionStringProperties.java (connection string parser)
    - [ ] Create MessageSender.java and MessageReceiver.java wrapper interfaces
    - [ ] Create ErrorPhase.java enum and MessageHandler.java interface
  - **Verification**:
    - Command: `mvn clean test-compile -q`
    - JDK: JDK 17
    - Expected: Compilation SUCCESS (new files compile, existing files will fail since not yet migrated)

---

- Step 3: Migrate All Source Code
  - **Rationale**: Update all 10 main source files to use modern SDK APIs and application-level abstractions.
  - **Changes to Make**:
    - [ ] Migrate ErrorClassifier.java (use ServiceBusFailureReason instead of exception subclasses)
    - [ ] Migrate MessageTransformer.java (use ServiceBusMessage instead of IMessage)
    - [ ] Migrate MessageCache.java (remove IMessage generic bound, use ServiceBusMessage)
    - [ ] Migrate MessageInspector.java (use custom MessageBody/MessageBodyType, update reflection)
    - [ ] Migrate MessageSerializer.java (use ServiceBusMessage, custom body types)
    - [ ] Migrate MessageRouter.java (use wrapper interfaces, ServiceBusReceivedMessage)
    - [ ] Migrate OrderMessageHandler.java (implement custom MessageHandler, use ErrorPhase)
    - [ ] Migrate QueueSessionManager.java (use ServiceBusProcessorClient)
    - [ ] Migrate ServiceBusClients.java (use ServiceBusClientBuilder, ConnectionStringProperties)
    - [ ] Migrate Application.java (use modern client builder APIs)
  - **Verification**:
    - Command: `mvn clean test-compile -q`
    - JDK: JDK 17
    - Expected: Compilation SUCCESS

---

- Step 4: Migrate All Test Code
  - **Rationale**: Update all 8 test files to use modern SDK types and constructors.
  - **Changes to Make**:
    - [ ] Migrate ErrorClassifierTest.java (use ServiceBusException with ServiceBusFailureReason)
    - [ ] Migrate MessageCacheTest.java (use ServiceBusMessage)
    - [ ] Migrate MessageInspectorTest.java (use custom MessageBody/MessageBodyType)
    - [ ] Migrate MessageRouterTest.java (mock wrapper interfaces, use ServiceBusReceivedMessage)
    - [ ] Migrate MessageSerializerTest.java (use ServiceBusMessage)
    - [ ] Migrate OrderMessageHandlerTest.java (use ServiceBusMessage, ErrorPhase)
    - [ ] Migrate QueueSessionManagerTest.java (use ConnectionStringProperties)
    - [ ] Migrate ServiceBusClientsTest.java (use ConnectionStringProperties)
  - **Verification**:
    - Command: `mvn clean test-compile -q`
    - JDK: JDK 17
    - Expected: Compilation SUCCESS

---

- Step 5: Final Validation
  - **Rationale**: Verify all upgrade goals met, all tests pass, no legacy references remain.
  - **Changes to Make**:
    - [ ] Verify no legacy com.microsoft.azure.* dependencies remain
    - [ ] Resolve ALL TODOs and temporary workarounds from previous steps
    - [ ] Clean rebuild with current JDK
    - [ ] Fix any remaining compilation errors
    - [ ] Run full test suite and fix ALL test failures (iterative fix loop until 100% pass)
  - **Verification**:
    - Command: `mvn clean test -q`
    - JDK: JDK 17
    - Expected: Compilation SUCCESS + 100% tests pass (67/67)

## Key Challenges

- **Final SDK Classes**: ServiceBusSenderClient, ServiceBusReceiverClient, ServiceBusReceivedMessage, ServiceBusException are final. Tests that mock IMessageSender/IMessageReceiver need wrapper interfaces. Tests that mock IMessage need mockito-inline for ServiceBusReceivedMessage.
- **MessageBody/MessageBodyType Removal**: Code uses MessageBody.fromValueData(), fromSequenceData(), getBodyType() extensively in MessageInspector and MessageSerializer. Need full application-level replacements.
- **IMessage Generic Bound**: MessageCache<T extends IMessage> uses generics bounded by IMessage. Modern SDK has no shared interface between ServiceBusMessage and ServiceBusReceivedMessage. Must redesign to use ServiceBusMessage.
- **Exception Hierarchy Change**: ErrorClassifier uses instanceof chains for exception subclasses. Must convert to ServiceBusFailureReason-based switch. Tests construct exception subclasses directly.
- **Reflection on Message Internals**: MessageInspector uses reflection to access Message.messageBody field. Modern SDK's ServiceBusMessage has different internal structure.
- **Lock Token API Change**: Legacy uses UUID lockToken for complete/abandon/deadLetter. Modern SDK passes the message object directly.
