# Legacy Azure SDK Reference

## Artifact Map

| Service | Legacy Artifact | Typical Version |
|---------|----------------|-----------------|
| Storage Blob/Queue/Table/File | `com.microsoft.azure:azure-storage` | 8.6.6 |
| Event Hubs | `com.microsoft.azure:azure-eventhubs` | 3.3.0 |
| Event Hubs EPH | `com.microsoft.azure:azure-eventhubs-eph` | 3.3.0 |
| Service Bus | `com.microsoft.azure:azure-servicebus` | 3.6.7 |
| Key Vault | `com.microsoft.azure:azure-keyvault` | 1.2.6 |
| Cosmos DB | `com.microsoft.azure:azure-documentdb` | 2.4.3 |
| Event Grid | `com.microsoft.azure:azure-eventgrid` | 1.2.0 |
| Management (all services) | `com.microsoft.azure:azure` | 1.36.3 |
| Batch | `com.microsoft.azure:azure-batch` | 11.2.0 |
| Auth (ADAL) | `com.microsoft.azure:adal4j` | 1.6.7 |
| REST runtime | `com.microsoft.azure:azure-client-runtime` | 1.7.14 |

## Strategy Notes

When writing edge-case code:

1. **Combine patterns** — a single class should use 3+ patterns together. Isolated patterns are easier to migrate one at a time; entangled patterns create cascading failures.
2. **Make tests depend on legacy types** — if production code can be migrated but tests cannot compile, the migration still fails.
3. **Use checked exceptions** — legacy Azure SDK throws checked exceptions (`StorageException`, `URISyntaxException`, `InvalidKeyException`). Method signatures with `throws` clauses force every caller to handle them. Modern SDK uses unchecked exceptions, so removing `throws` clauses changes the method contract.
4. **Avoid wrapping** — do NOT wrap legacy SDK types behind interfaces/adapters. Expose them directly in method signatures, fields, and return types. Wrapping makes migration easier.
5. **Thread legacy types through layers** — pass legacy SDK objects through multiple methods/classes so migration requires touching many files simultaneously.

## Anti-Patterns (what NOT to do)

- Do NOT write code that fails to compile with the legacy SDK.
- Do NOT use "sample", "edge-case", "legacy", "migration" in class/method/package names.
- Do NOT include comments hinting at migration difficulty.
- Do NOT use `com.azure` (modern) SDKs anywhere in the project.
- Do NOT mock everything — some tests should exercise real logic paths with legacy types woven through the call chain.
- Do NOT use real Azure credentials or connection strings.
