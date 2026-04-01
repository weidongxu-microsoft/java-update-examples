# Azure Service Bus Edge Case — Legacy SDK Migration Challenge

Order processing system using Azure Service Bus legacy SDK (`com.microsoft.azure:azure-servicebus:3.6.7`) with patterns that resist automated migration.

## Edge-Case Patterns Used

| Pattern | ID | Description |
|---------|-----|-------------|
| Subclassing legacy SDK | 2.1 | `OrderMessageHandler` implements `IMessageHandler` interface |
| Legacy exception hierarchy | 1.2 + 2.5 | `ErrorClassifier` uses `instanceof` chains on 6 exception subclasses (`ServerBusyException`, `MessageLockLostException`, `SessionLockLostException`, `QuotaExceededException`, `AuthorizationFailedException`, `MessagingEntityNotFoundException`) |
| Mock-heavy tests | 1.3 | Tests mock `IMessage`, `IMessageSender`, `IMessageReceiver` with `ArgumentCaptor` verification |
| Custom serialization | 2.4 | Jackson `JsonSerializer`/`JsonDeserializer` for `IMessage` using `MessageBody` and `MessageBodyType` |
| CompletableFuture pipelines | 3.5 | Multi-stage async chains in `MessageRouter` returning `CompletableFuture<IMessage>` |
| Functional interfaces | 3.10 | `MessageTransformer` as `@FunctionalInterface` typed to `IMessage` |
| Reflection into internals | 2.2 | `MessageInspector` accesses `Message.messageBody` private field via reflection |
| Generic type parameters | 2.3 | `MessageCache<T extends IMessage>` with SDK-typed bounds |
| Static factory methods | 3.9 | `ServiceBusClients` returns `ConnectionStringBuilder`, `IMessageSender`, `IMessageReceiver` |
| Behavioral quirk assertions | 3.8 | Tests assert on `isTransient` flags of specific exception subclasses |

Additional SDK-specific types used: `MessageBody`, `MessageBodyType` (BINARY/VALUE/SEQUENCE), `ConnectionStringBuilder` (SAS key/endpoint extraction), `ExceptionPhase`, `ReceiveMode`, `MessageHandlerOptions`.

## Why These Patterns Resist Bare Migration

1. **Final classes in modern SDK**: `ServiceBusException`, `ServiceBusSenderClient`, `ServiceBusReceiverClient`, `ServiceBusReceivedMessage` are all `final` — Mockito cannot mock them. Tests that mock legacy interfaces (`IMessageSender`, `IMessageReceiver`) break completely.

2. **Message type split**: Legacy SDK has one `IMessage` interface. Modern SDK has `ServiceBusMessage` (sending) and `ServiceBusReceivedMessage` (receiving) that do NOT share a type hierarchy. Generic bounds like `T extends IMessage` have no direct equivalent.

3. **Exception subclass elimination**: Legacy SDK has 6+ exception subclasses (`ServerBusyException`, `MessageLockLostException`, etc.) with individual `instanceof` checks. Modern SDK collapses these into a single `ServiceBusException` with `ServiceBusFailureReason` enum.

4. **MessageBody/MessageBodyType removal**: Legacy SDK supports BINARY, VALUE, and SEQUENCE body types. Modern SDK uses `BinaryData` uniformly — no equivalent type system.

5. **ConnectionStringBuilder removal**: Legacy SDK has a rich `ConnectionStringBuilder` with methods like `getSasKey()`, `getSasKeyName()`, `getEndpoint()`, `getEntityPath()`. Modern SDK has no equivalent — connection strings are passed directly to the builder.

## Adversarial Loop Results

| Iteration | Model | Result | Key Finding |
|-----------|-------|--------|-------------|
| 1 | Sonnet (default) | Passed | Agent created abstraction interfaces to decouple from SDK types |
| 2 | Sonnet (default) | Passed | Agent created custom `MessageBodyType` enum wrapper |
| 3 | Sonnet (default) | Passed | Agent created `ConnectionConfig` class replacing `ConnectionStringBuilder` |
| **4** | **Sonnet 4.5** | **FAILED** | **19 test errors — Mockito cannot mock final `ServiceBusException`, `ServiceBusSenderClient`, `ServiceBusReceiverClient`** |

The migration failed at iteration 4 with claude-sonnet-4.5. The combination of mock-heavy tests (pattern 1.3) with the modern SDK's final classes created an insurmountable barrier without guidance.

## How the Migration Guide Addresses the Gap

The migration guide was updated with Service Bus-specific guidelines:

1. **Final class workaround**: Explicitly documents that `ServiceBusException` should be constructed directly (`new ServiceBusException(cause, reason)`) rather than mocked. Sender/receiver need wrapper interfaces.

2. **Message type split**: Documents the `ServiceBusMessage` vs `ServiceBusReceivedMessage` split and advises on generic bound migration.

3. **Exception enum distinction**: Clarifies `ServiceBusFailureReason` (from `getReason()`) vs `ServiceBusErrorSource` (from error context) — two different enums that agents frequently confuse.

### Guided Migration Results

| Attempt | Result | Issue |
|---------|--------|-------|
| 1 | Failed (compile errors) | Generic bounds: `ServiceBusReceivedMessage` not within `T extends ServiceBusMessage` |
| 2 | Failed (27 test errors) | Still trying to mock final `ServiceBusException` |
| **3** | **Passed (62/62 tests)** | With explicit final-class handling instructions, all tests pass |

The guide proved its value: without it, the migration fails; with it (after 2 guide updates), the migration succeeds.

### Guide Updates Made

1. Added "Package-Specific Source Code Guidelines: com.microsoft.azure.servicebus.**" section covering message type split, exception enums, final classes, ConnectionStringBuilder removal, and MessageBody removal.

2. Expanded final class guidance to include `ServiceBusException` with explicit constructor example.

## Project Structure

```
src/main/java/com/contoso/messaging/
├── Application.java              — Main entry point
├── ErrorClassifier.java          — Exception subclass instanceof chain (6 subclasses)
├── MessageCache.java             — Generic type bound to IMessage
├── MessageInspector.java         — MessageBody/MessageBodyType + reflection
├── MessageRouter.java            — CompletableFuture pipelines
├── MessageSerializer.java        — Jackson custom serializer for IMessage
├── MessageTransformer.java       — @FunctionalInterface typed to IMessage
├── OrderMessageHandler.java      — IMessageHandler implementation
├── QueueSessionManager.java      — QueueClient + MessageHandlerOptions
└── ServiceBusClients.java        — Static factories returning ConnectionStringBuilder

src/test/java/com/contoso/messaging/
├── ErrorClassifierTest.java      — 15 tests (behavioral quirk assertions)
├── MessageCacheTest.java         — 10 tests (generics + transforms)
├── MessageInspectorTest.java     — 11 tests (MessageBody types + reflection)
├── MessageRouterTest.java        — 6 tests (async pipeline mocking)
├── MessageSerializerTest.java    — 5 tests (round-trip serialization)
├── OrderMessageHandlerTest.java  — 7 tests (handler + error classification)
├── QueueSessionManagerTest.java  — 6 tests (transformer chaining)
└── ServiceBusClientsTest.java    — 7 tests (ConnectionStringBuilder parsing)
```

**67 tests total**, all passing with the legacy SDK.
