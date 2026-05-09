# Migration Plan: azure-event-hubs-spark Track 1 → Track 2

**Project:** Azure Event Hubs Spark Connector  
**Date:** 2026-05-09  
**Migration ID:** eventhubs-spark-20260509  
**Scope:** Upgrade from Track 1 (`com.microsoft.azure:azure-eventhubs:3.3.0`) to Track 2 (`com.azure:azure-messaging-eventhubs`)

---

## Inventory of Legacy Dependencies

| Legacy Artifact | Version | Track 2 Replacement | Status |
|---|---|---|---|
| `com.microsoft.azure:azure-eventhubs` | 3.3.0 | `com.azure:azure-messaging-eventhubs` | To migrate |
| `com.microsoft.azure:msal4j` | 1.7.0 | `com.azure:azure-identity` | To migrate |
| Transitive: `com.microsoft.azure:azure-client-authentication` | 1.7.3 | Removed (absorbed by `azure-identity`) | To remove |
| Transitive: `com.microsoft.azure:azure-client-runtime` | 1.7.3 | Removed (absorbed by modern SDKs) | To remove |
| Transitive: `com.microsoft.azure:adal4j` | 1.6.4 | `com.azure:azure-identity` | To remove |

---

## Migration Phases

### Phase 1: Dependency Updates (pom.xml)
- [ ] Add `azure-sdk-bom` for version management
- [ ] Replace `com.microsoft.azure:azure-eventhubs:3.3.0` → `com.azure:azure-messaging-eventhubs`
- [ ] Replace `com.microsoft.azure:msal4j` → `com.azure:azure-identity`
- [ ] Remove transitive legacy dependencies (will be managed by BOM)
- [ ] **Commit:** "chore: upgrade dependencies to Track 2 SDKs"

### Phase 2: Source Code Migration (Scala)
Main files to refactor:
- `core/src/main/scala/org/apache/spark/eventhubs/ConnectionStringBuilder.scala`
  - [ ] Update connection string parsing for Track 2
  - [ ] Replace legacy credential handling with `TokenCredential`
  
- `core/src/main/scala/org/apache/spark/eventhubs/EventHubsConf.scala`
  - [ ] Update configuration to match Track 2 client initialization
  
- `core/src/main/scala/org/apache/spark/eventhubs/client/*.scala`
  - [ ] Replace `EventHubClient` → `EventHubsConsumerClient` / `EventHubsProducerClient`
  - [ ] Replace epoch receivers with Track 2 consumer patterns
  - [ ] Update exception handling (`com.microsoft.azure.eventhubs.EventHubException` → `com.azure.messaging.eventhubs.models.*`)
  
- `core/src/main/scala/org/apache/spark/sql/eventhubs/*.scala`
  - [ ] Update structured streaming source/sink to use Track 2 client
  
- `core/src/main/scala/org/apache/spark/streaming/eventhubs/*.scala`
  - [ ] Update Spark Streaming DStream adapter to use Track 2 client

- **Commit:** "refactor: update source code to use Track 2 API patterns"

### Phase 3: Test Updates
- [ ] Update test files in `core/src/test/scala/`
- [ ] Verify mock/simulation utilities work with Track 2
- [ ] Fix any test fixture issues
- **Commit:** "test: update tests for Track 2 compatibility"

### Phase 4: Documentation & Examples
- [ ] Update `examples/multiple-readers-example.md` with Track 2 patterns
- [ ] Update `docs/*.md` if API examples reference Track 1 patterns
- **Commit:** "docs: update examples for Track 2 SDK"

### Phase 5: Build & Validation
- [ ] Build with `mvn clean compile`
- [ ] Run tests with `mvn clean test`
- [ ] Fix any remaining issues
- [ ] Final validation commit

---

## References

- **Track 2 EventHubs SDK:** https://github.com/Azure/azure-sdk-for-java/tree/main/sdk/eventhubs/azure-messaging-eventhubs
- **Migration Guide:** https://github.com/Azure/azure-sdk-for-java/blob/main/sdk/eventhubs/azure-messaging-eventhubs/MIGRATION.md
- **Azure Identity:** https://github.com/Azure/azure-sdk-for-java/tree/main/sdk/identity/azure-identity

---

## Known Constraints

- **JDK Version:** Java 8 required for Scala 2.11 compatibility; Java 25 causes Scala compiler errors
- **Spark Version:** 2.3.3 (EOL, but compatible with Track 2 SDKs)
- **Scala Version:** 2.11.8 (will not be upgraded to avoid major refactoring)
- **Test Execution:** Must use JDK 8 or compatible runtime

---

## Success Criteria

- ✅ All `com.microsoft.azure` imports replaced with `com.azure`
- ✅ Zero compilation errors
- ✅ 100% test pass rate
- ✅ No deprecated SDK usages
- ✅ Examples demonstrate Track 2 patterns

---

## Estimated Effort

| Phase | Effort | Blockers |
|---|---|---|
| Phase 1 (Dependencies) | 0.5 days | None |
| Phase 2 (Source Code) | 2-3 days | Complex refactoring of client receiver logic |
| Phase 3 (Tests) | 1 day | May depend on Phase 2 completion |
| Phase 4 (Docs) | 0.5 days | None |
| Phase 5 (Validation) | 0.5 days | JDK 8 runtime availability |

