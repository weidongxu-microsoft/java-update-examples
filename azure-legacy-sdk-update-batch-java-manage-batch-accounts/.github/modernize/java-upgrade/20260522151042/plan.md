# Upgrade Plan: azure-legacy-sdk-update-batch-java-manage-batch-accounts (20260522151042)

- **Generated**: 2026-05-22 15:10:42
- **HEAD Branch**: modernize/java-20260522225454
- **HEAD Commit ID**: 6d071296df8929482b0903241a23713a0bb952a4

## Available Tools

**JDKs**
- JDK 21.0.3: /Library/Java/JavaVirtualMachines/jdk-21.jdk/Contents/Home (current environment JDK)

**Build Tools**
- Maven 3.9.10: /opt/homebrew/Cellar/maven/3.9.10/libexec (no wrapper present)

## Guidelines

- Migrate `com.microsoft.azure:azure:1.36.3` (Track 1) to Track 2 Azure SDK for Java
- Use `azure-sdk-bom:1.3.7` (latest stable, resolved from https://raw.githubusercontent.com/Azure/azure-sdk-for-java/main/sdk/boms/azure-sdk-bom/pom.xml)
- Use BOM-managed versions; remove explicit versions for BOM-managed `com.azure` dependencies
- Replace file-based auth (AZURE_AUTH_LOCATION) with DefaultAzureCredential + TODO comment
- Do NOT change Java package declarations or move/rename source files
- Do NOT upgrade JDK (already Java 8 source compatibility target; JDK 21 is used to run Maven)
- Preserve behavior and stdout/stderr order where possible; note unavoidable reordering due to separate storage account creation
- Apply BatchAccount modern patterns: `AutoStorageBaseProperties` for auto-storage linking

> Note: Migration guide URLs used:
> - https://aka.ms/java-track2-migration-guide (Track 1 → Track 2 resource manager)
> - https://raw.githubusercontent.com/Azure/azure-sdk-for-java/main/sdk/batch/azure-compute-batch/MigrationGuide.md (Batch dataplane guide)

## Options

- Working branch: modernize/java-20260522225454
- Run tests before and after the upgrade: true

## Upgrade Goals

- Migrate `com.microsoft.azure:azure:1.36.3` → `com.azure.resourcemanager:azure-resourcemanager:2.62.0` (Track 2, via azure-sdk-bom:1.3.7)
- Add `com.azure:azure-identity:1.18.3` for modern authentication (via azure-sdk-bom:1.3.7)
- **TARGET_AZURE_SDK_BOM_VERSION**: `1.3.7`

## Technology Stack

| Technology/Dependency | Current | Min Compatible | Why Incompatible |
| --------------------- | ------- | -------------- | ---------------- |
| Java (source target) | 1.8 | 1.8 | No change needed |
| `com.microsoft.azure:azure` | 1.36.3 | N/A | Deprecated Track 1 omnibus SDK; replaced by Track 2 |
| `com.azure.resourcemanager:azure-resourcemanager` | (new) | 2.62.0 | Modern management SDK (includes batch, storage, resources) |
| `com.azure:azure-identity` | (new) | 1.18.3 | Modern authentication; replaces file-based auth |
| `com.microsoft.rest:client-runtime` | (transitive) | N/A | Replaced by `azure-core` in Track 2 |
| `commons-net:commons-net` | 3.3 | 3.3 | No change needed |
| `commons-lang:commons-lang` | 2.6 | 2.6 | No change needed |
| `org.apache.commons:commons-lang3` | 3.7 | 3.7 | No change needed |

## Derived Upgrades

- `com.microsoft.azure:azure:1.36.3` → removed; replaced by `azure-resourcemanager` + `azure-identity`
- `azure-sdk-bom:1.3.7` added as `dependencyManagement` import for version governance
- `com.microsoft.rest.LogLevel` → removed (no equivalent in Track 2; HTTP logging via `HttpLogOptions`)
- File-based auth (`AZURE_AUTH_LOCATION`) → `DefaultAzureCredential` with TODO comment

## Impact Analysis

### Dependency Changes

| File | Dependency | Current | Action | Target | Reason |
|------|-----------|---------|--------|--------|--------|
| pom.xml | `com.microsoft.azure:azure` | 1.36.3 | remove | — | Deprecated Track 1 |
| pom.xml | `azure-sdk-bom` | (absent) | add | 1.3.7 (pom import) | Version governance for Track 2 |
| pom.xml | `com.azure.resourcemanager:azure-resourcemanager` | (absent) | add | (BOM-managed, 2.62.0) | Track 2 management plane |
| pom.xml | `com.azure:azure-identity` | (absent) | add | (BOM-managed, 1.18.3) | Track 2 authentication |

### Source Code Changes

**ManageBatchAccount.java**

| Location | Current | Required Change | Reason |
|----------|---------|----------------|--------|
| imports | `com.microsoft.azure.management.Azure` | `com.azure.resourcemanager.AzureResourceManager` | Class renamed |
| imports | `com.microsoft.azure.management.batch.*` | `com.azure.resourcemanager.batch.models.*` | Package moved |
| imports | `com.microsoft.azure.management.resources.fluentcore.arm.Region` | `com.azure.core.management.Region` | Package moved |
| imports | `com.microsoft.azure.management.storage.StorageAccount` | `com.azure.resourcemanager.storage.models.StorageAccount` | Package moved |
| imports | `com.microsoft.azure.management.storage.StorageAccountKey` | `com.azure.resourcemanager.storage.models.StorageAccountKey` | Package moved |
| imports | `com.microsoft.rest.LogLevel` | removed | No equivalent; log level set separately in Track 2 |
| imports | `java.io.File` | removed | No longer needed (no file-based auth) |
| imports | (new) | `com.azure.core.credential.TokenCredential` | Track 2 auth |
| imports | (new) | `com.azure.core.management.AzureEnvironment` | Track 2 auth profile |
| imports | `(new)` | `com.azure.core.management.profile.AzureProfile` | Track 2 auth profile |
| imports | (new) | `com.azure.identity.DefaultAzureCredentialBuilder` | Track 2 auth |
| imports | (new) | `com.azure.resourcemanager.batch.models.AutoStorageBaseProperties` | auto-storage linking |
| `runSample(Azure azure)` | `Azure azure` parameter | `AzureResourceManager azure` | Class renamed |
| Batch account creation | `withNewStorageAccount(storageAccountName)` | Create storage account first; use `withAutoStorage(new AutoStorageBaseProperties().withStorageAccountId(storageAccount.id()))` | Track 2 does not support inline storage account creation via batch account fluent API |
| Batch account creation | `withNewResourceGroup(rgName)` (on batch) | `withExistingResourceGroup(rgName)` (resource group created by storage account) | Storage account created first with `withNewResourceGroup` |
| StorageAccount retrieval | `azure.storageAccounts().getByResourceGroup(rgName, storageAccountName)` | Use existing `storageAccount` variable | Already have reference from prior creation |
| `List<BatchAccount>` | `azure.batchAccounts().list()` returns `List` | Change type to `Iterable<BatchAccount>` | Track 2 returns `PagedIterable` |
| `List<BatchAccount> accounts` | index-based `accounts.get(i)` loop | Replace with enhanced for-loop with manual counter | `PagedIterable` does not support index access |
| Region comparison | `batchAccount.region() == region` | `batchAccount.region().equals(region)` | Track 2 `Region` is `ExpandableStringEnum`; use `.equals()` |
| Second batch account | `withExistingStorageAccount(storageAccount)` | `withAutoStorage(new AutoStorageBaseProperties().withStorageAccountId(storageAccount.id()))` | API changed in Track 2 |
| main() auth | `Azure.configure().withLogLevel(LogLevel.BASIC).authenticate(credFile).withDefaultSubscription()` | `DefaultAzureCredentialBuilder` + `AzureProfile` + `AzureResourceManager.authenticate(credential, profile).withDefaultSubscription()` | File-based auth removed in Track 2 |
| main() auth | `final File credFile = new File(System.getenv("AZURE_AUTH_LOCATION"))` | Replaced by `DefaultAzureCredential`; add TODO comment | File-based auth not supported |

**Utils.java**

| Location | Current | Required Change | Reason |
|----------|---------|----------------|--------|
| imports | `com.microsoft.azure.management.batch.Application` | `com.azure.resourcemanager.batch.models.Application` | Package moved |
| imports | `com.microsoft.azure.management.batch.ApplicationPackage` | `com.azure.resourcemanager.batch.models.ApplicationPackage` | Package moved |
| imports | `com.microsoft.azure.management.batch.BatchAccount` | `com.azure.resourcemanager.batch.models.BatchAccount` | Package moved |
| imports | `com.microsoft.azure.management.batch.BatchAccountKeys` | `com.azure.resourcemanager.batch.models.BatchAccountKeys` | Package moved |
| imports | `com.microsoft.azure.management.storage.StorageAccountKey` | `com.azure.resourcemanager.storage.models.StorageAccountKey` | Package moved |

### Configuration Changes

None required.

### CI/CD Changes

None present in project.

### Risks & Warnings

- **Storage account creation order change**: In Track 2, auto-storage requires an existing storage account ID. The storage account is created before the batch account. This reorders 1 println statement (storage account creation precedes batch account creation). Behavior is identical; output is slightly reordered. **Mitigation**: Added inline comment explaining the ordering change.
- **`withNewStorageAccount` removed**: Track 2 `BatchAccount.DefinitionStages` does not provide `withNewStorageAccount`. Replaced with `withAutoStorage(new AutoStorageBaseProperties().withStorageAccountId(...))`. **Mitigation**: Explicit storage account creation above.
- **File-based auth removed**: `AZURE_AUTH_LOCATION`-based auth is entirely removed from Track 2. DefaultAzureCredential is used with a prominent TODO comment. **Mitigation**: Comment included pointing to the migration guide.
- **No tests in project**: The project has no test classes. Final validation will verify compilation only. Pass rate: N/A (no tests = 100% of 0).

## Upgrade Steps

- Step 1: Setup Environment — Verify JDK/Maven (already available; no installs needed)
  - **Rationale**: Environment is already configured with JDK 21 and Maven 3.9.10
  - **Changes to Make**: None (verification only via env check)
  - **Verification**: `java -version && mvn --version`, Expected: JDK present, Maven present

- Step 2: Setup Baseline — Run baseline compilation with current Track 1 SDK
  - **Rationale**: Establish baseline compile result before migration
  - **Changes to Make**: None
  - **Verification**: `mvn clean compile -q`, JDK: system JDK 21, Expected: SUCCESS (project was previously built)

- Step 3: Migrate pom.xml — Replace Track 1 dependency with Track 2 BOM + dependencies
  - **Rationale**: Remove `com.microsoft.azure:azure:1.36.3`; add `azure-sdk-bom:1.3.7`, `azure-resourcemanager`, `azure-identity`
  - **Changes to Make**: All Dependency Changes from Impact Analysis
  - **Verification**: `mvn dependency:resolve -q`, Expected: dependencies resolve without error

- Step 4: Migrate ManageBatchAccount.java — Replace all Track 1 API usage with Track 2
  - **Rationale**: All source changes for the main sample class
  - **Changes to Make**: All Source Code Changes for ManageBatchAccount.java from Impact Analysis
  - **Verification**: `mvn clean compile -q`, Expected: SUCCESS

- Step 5: Migrate Utils.java — Update imports to Track 2 packages
  - **Rationale**: Helper class uses Track 1 batch/storage types
  - **Changes to Make**: Source Code Changes for Utils.java from Impact Analysis
  - **Verification**: `mvn clean compile -q`, Expected: SUCCESS

- Step 6: CVE Validation & Fix — Scan new dependencies for known CVEs
  - **Rationale**: Security hygiene after adding new dependencies
  - **Changes to Make**: Upgrade any vulnerable dependency versions
  - **Verification**: Re-scan shows no remaining CVEs

- Step 7: Final Validation — Clean rebuild, confirm no legacy refs, confirm BOM version
  - **Rationale**: Confirm all goals met; no legacy SDK refs remain
  - **Changes to Make**: Fix any remaining issues found
  - **Verification**: `mvn clean compile -q` SUCCESS; grep for legacy refs returns nothing; BOM version confirmed
