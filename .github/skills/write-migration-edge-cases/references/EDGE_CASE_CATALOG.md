# Edge-Case Pattern Catalog

Patterns ordered by difficulty. Higher tiers subsume lower tiers.
**Combine 3+ patterns** per class for maximum effect — isolated patterns are easier to migrate.

---

## Tier 1 — Moderate (baseline traps)

### 1.1 Deep Fluent Chain with Side Effects

Store intermediate builder objects in variables, pass them to helper methods, or conditionally branch them. Modern SDK has different builder signatures and return types at each stage.

```java
// Store intermediate builder — type changes in modern SDK
VirtualMachine.DefinitionStages.WithCreate vmBuilder =
    azure.virtualMachines().define(vmName)
         .withRegion(region)
         .withExistingResourceGroup(rgName)
         .withExistingPrimaryNetworkInterface(nic);
applyConditionalConfig(vmBuilder);  // helper accepts the specific stage type
```

### 1.2 Legacy Exception Type Hierarchy

Catch legacy-specific exception types (`StorageException`, `DocumentClientException`, `EventHubException`) and branch on their status codes, error codes, or messages.

```java
try {
    container.createIfNotExists();
} catch (StorageException e) {
    if (e.getErrorCode().equals("ContainerAlreadyExists")) {
        handleConflict(e.getExtendedErrorInformation());
    }
}
```

### 1.3 Mock-Heavy Tests Bound to Legacy Types

Tests using `@Mock` with legacy SDK types, `ArgumentCaptor<EventData>` (legacy), `verify()` on legacy method signatures. Migration must rewrite every mock and assertion.

```java
@Mock private CloudBlobContainer mockContainer;
@Mock private CloudBlockBlob mockBlob;

@Test
public void testUpload() throws StorageException {
    when(mockContainer.getBlockBlobReference("key")).thenReturn(mockBlob);
    doNothing().when(mockBlob).uploadText("value");
    manager.upload("key", "value");
    verify(mockBlob).uploadText("value");
}
```

---

## Tier 2 — Hard (structural traps)

### 2.1 Subclassing Legacy SDK Classes

Extend legacy SDK abstract classes or implement legacy interfaces. Modern SDK often seals these or restructures the hierarchy.

```java
public class CustomCredentials extends KeyVaultCredentials {
    @Override
    public String doAuthenticate(String authorization, String resource, String scope) {
        return fetchTokenFromCustomProvider(authorization, resource);
    }
}
```

```java
public class CustomEventProcessor implements IEventProcessor {
    @Override public void onOpen(PartitionContext context) { /* ... */ }
    @Override public void onEvents(PartitionContext context, Iterable<EventData> messages) { /* ... */ }
    @Override public void onClose(PartitionContext context, CloseReason reason) { /* ... */ }
    @Override public void onError(PartitionContext context, Throwable error) { /* ... */ }
}
```

### 2.2 Reflection into Legacy SDK Internals

Use `java.lang.reflect` to access private fields or non-public methods. Modern SDK classes have completely different internals.

```java
Field policyField = CloudBlobClient.class.getDeclaredField("defaultRequestOptions");
policyField.setAccessible(true);
BlobRequestOptions opts = (BlobRequestOptions) policyField.get(blobClient);
opts.setMaximumExecutionTimeInMs(30000);
```

### 2.3 Generic Type Parameters Bound to Legacy Types

Generics with bounds on legacy SDK types. Migration requires changing every type parameter, cascading compiler errors.

```java
public class AzureResourceCache<T extends CloudBlob> {
    private final Map<String, T> cache = new ConcurrentHashMap<>();
    public T getOrFetch(String key, Supplier<T> fetcher) {
        return cache.computeIfAbsent(key, k -> fetcher.get());
    }
    public URI getBlobUri(T blob) {
        return blob.getUri();  // CloudBlob.getUri() — different in modern SDK
    }
}
```

### 2.4 Custom Serialization of Legacy SDK Objects

Jackson custom serializers/deserializers that depend on legacy class structure.

```java
public class EventDataSerializer extends JsonSerializer<EventData> {
    @Override
    public void serialize(EventData value, JsonGenerator gen, SerializerProvider prov)
            throws IOException {
        gen.writeStartObject();
        gen.writeStringField("body", new String(value.getBytes(), StandardCharsets.UTF_8));
        gen.writeObjectField("properties", value.getProperties());
        gen.writeObjectField("systemProperties", value.getSystemProperties());
        gen.writeEndObject();
    }
}
```

### 2.5 instanceof Chains on Legacy Types

`instanceof` checks on legacy SDK types for dispatch. Type hierarchy is entirely different in modern SDKs.

```java
public String getBlobType(ListBlobItem item) {
    if (item instanceof CloudBlockBlob) {
        return "block:" + ((CloudBlockBlob) item).getProperties().getLength();
    } else if (item instanceof CloudPageBlob) {
        return "page:" + ((CloudPageBlob) item).getProperties().getLength();
    } else if (item instanceof CloudAppendBlob) {
        return "append:" + ((CloudAppendBlob) item).getProperties().getLength();
    } else if (item instanceof CloudBlobDirectory) {
        return "directory:" + ((CloudBlobDirectory) item).getPrefix();
    }
    return "unknown";
}
```

### 2.6 SAS Token Generation via Legacy APIs

`CloudStorageAccount.generateSharedAccessSignature()` with `SharedAccessAccountPolicy`. Modern SDK uses completely different SAS generation classes.

```java
SharedAccessAccountPolicy policy = new SharedAccessAccountPolicy();
policy.setPermissionsFromString("rwdl");
policy.setSharedAccessExpiryTime(Date.from(Instant.now().plus(Duration.ofHours(1))));
policy.setResourceTypeFromString("sco");
policy.setSharedAccessStartTime(new Date());
String sasToken = account.generateSharedAccessSignature(policy);
```

---

## Tier 3 — Maximum (compounding traps)

### 3.1 Cross-Type Entanglement

Methods that accept AND return legacy SDK types. Both input and output types change in modern SDK, forcing rewrite of all callers.

```java
public CloudBlockBlob copyBlobWithMetadata(
        CloudBlobContainer source, CloudBlobContainer target,
        String blobName, HashMap<String, String> metadata)
        throws StorageException, URISyntaxException {
    CloudBlockBlob sourceBlob = source.getBlockBlobReference(blobName);
    CloudBlockBlob targetBlob = target.getBlockBlobReference(blobName);
    targetBlob.startCopy(sourceBlob);
    targetBlob.setMetadata(metadata);
    targetBlob.uploadMetadata();
    return targetBlob;
}
```

### 3.2 Annotation Processing with Legacy Types

Custom annotations whose runtime processors inspect legacy SDK types via reflection.

```java
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface AzureBlob {
    String container();
    String path();
}

public class AzureBlobInjector {
    public void inject(Object target) throws Exception {
        for (Field f : target.getClass().getDeclaredFields()) {
            AzureBlob ann = f.getAnnotation(AzureBlob.class);
            if (ann != null) {
                CloudBlockBlob blob = container.getBlockBlobReference(ann.path());
                f.setAccessible(true);
                f.set(target, blob);  // Field type is CloudBlockBlob
            }
        }
    }
}
```

### 3.3 Transitive Dependency Exploitation

Use classes from transitive dependencies of the legacy SDK that are NOT in the modern SDK's dependency tree. Removing the legacy SDK removes these classes.

```java
// com.microsoft.rest classes come transitively via legacy azure SDK
import com.microsoft.rest.ServiceResponseBuilder;
import com.microsoft.rest.serializer.JacksonAdapter;

public class AzureHttpHelper {
    private final JacksonAdapter adapter = new JacksonAdapter();
    public <T> T deserialize(String json, Type type) throws IOException {
        return adapter.deserialize(json, type);
    }
}
```

### 3.4 Enum/Constant Mapping Tables

Lookup tables or switch statements keyed by legacy SDK enum values. Modern SDK renames or restructures these.

```java
private static final Map<StorageErrorCodeStrings, RecoveryAction> ERROR_RECOVERY =
    Map.of(
        StorageErrorCodeStrings.CONTAINER_NOT_FOUND, RecoveryAction.CREATE_CONTAINER,
        StorageErrorCodeStrings.BLOB_NOT_FOUND, RecoveryAction.SKIP,
        StorageErrorCodeStrings.LEASE_ID_MISSING, RecoveryAction.ACQUIRE_LEASE,
        StorageErrorCodeStrings.SERVER_BUSY, RecoveryAction.RETRY_WITH_BACKOFF
    );
```

### 3.5 CompletableFuture Pipelines Tied to Legacy Async

Multi-stage `CompletableFuture` pipelines returning legacy SDK types. Modern SDK uses Reactor (`Mono`/`Flux`), making translation non-trivial.

```java
public CompletableFuture<Void> processEventsAsync(EventHubClient client) {
    return client.createPartitionSender("0")
        .thenCompose(sender -> {
            EventData event = EventData.create("data".getBytes());
            return sender.send(event);
        })
        .thenCompose(v -> client.createReceiver("$Default", "0",
                EventPosition.fromStartOfStream()))
        .thenCompose(receiver -> receiver.receive(100)
            .thenApply(events -> {
                events.forEach(e -> process(e.getBytes()));
                return null;
            }))
        .exceptionally(ex -> {
            if (ex.getCause() instanceof EventHubException) {
                handleEventHubError((EventHubException) ex.getCause());
            }
            return null;
        });
}
```

### 3.6 Mixed Legacy + Non-Azure Library Integration

Tightly couple legacy SDK types with third-party libraries (Guava, Spring, etc.) so migration requires changing the integration seam, not just Azure code.

```java
private final LoadingCache<String, CloudBlockBlob> blobCache = CacheBuilder.newBuilder()
    .maximumSize(100)
    .expireAfterWrite(5, TimeUnit.MINUTES)
    .build(new CacheLoader<String, CloudBlockBlob>() {
        @Override
        public CloudBlockBlob load(String blobName) throws Exception {
            return container.getBlockBlobReference(blobName);
        }
    });
```

### 3.7 Multi-Service Entanglement

Single class using multiple legacy Azure services (Storage + Key Vault + Event Hubs), passing objects between them. Migrating one service breaks the others.

```java
public void secureUpload(KeyVaultClient kvClient, CloudBlobContainer container,
        EventHubClient ehClient, String secretName, String data) throws Exception {
    SecretBundle secret = kvClient.getSecret("https://vault.azure.net/", secretName);
    byte[] encrypted = encrypt(data, secret.value());

    CloudBlockBlob blob = container.getBlockBlobReference("encrypted-" + secretName);
    blob.uploadFromByteArray(encrypted, 0, encrypted.length);

    EventData notification = EventData.create(
        String.format("{\"blob\":\"%s\",\"uri\":\"%s\"}", blob.getName(), blob.getUri())
            .getBytes(StandardCharsets.UTF_8));
    ehClient.sendSync(notification);
}
```

### 3.8 Legacy SDK Behavioral Quirks in Test Assertions

Tests asserting on behavioral quirks of the legacy SDK (exception messages, null vs empty, property ordering) that differ in modern SDK.

```java
@Test
public void testStorageExceptionMessage() {
    StorageException ex = new StorageException("BlobNotFound",
        "The specified blob does not exist.", 404, null, null);
    assertTrue(ex.getMessage().contains("The specified blob does not exist."));
    assertEquals(404, ex.getHttpStatusCode());
    assertEquals("BlobNotFound", ex.getErrorCode());
    assertNull(ex.getExtendedErrorInformation());
}
```

### 3.9 Static Factory Methods Returning Legacy Types

Static factories returning legacy SDK types. All callers depend on the return type.

```java
public final class AzureClients {
    public static CloudBlobClient createBlobClient(String connectionString)
            throws URISyntaxException, InvalidKeyException {
        return CloudStorageAccount.parse(connectionString).createCloudBlobClient();
    }
    public static CloudQueueClient createQueueClient(String connectionString)
            throws URISyntaxException, InvalidKeyException {
        return CloudStorageAccount.parse(connectionString).createCloudQueueClient();
    }
    public static CloudTableClient createTableClient(String connectionString)
            throws URISyntaxException, InvalidKeyException {
        return CloudStorageAccount.parse(connectionString).createCloudTableClient();
    }
}
```

### 3.10 Functional Interfaces Typed to Legacy Classes

`@FunctionalInterface` / `Function<>` / `Consumer<>` parameterized with legacy SDK types, propagating through method signatures and lambdas.

```java
@FunctionalInterface
public interface BlobProcessor {
    void process(CloudBlockBlob blob) throws StorageException;
}

public void forEachBlob(CloudBlobContainer container, BlobProcessor processor)
        throws StorageException, URISyntaxException {
    for (ListBlobItem item : container.listBlobs()) {
        if (item instanceof CloudBlockBlob) {
            processor.process((CloudBlockBlob) item);
        }
    }
}
```
