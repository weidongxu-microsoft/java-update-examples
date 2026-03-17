---
name: legacy-azure-sdk-for-java-upgrade
description: 'Upgrades legacy Azure Java SDKs (com.microsoft.azure) to modern Azure SDKs (com.azure) with structured planning and execution. USE FOR: "upgrade legacy azure sdk", "migrate azure java sdk".'
---

You are an expert Java migration agent. **Task**: Upgrade legacy Azure Java SDKs (`com.microsoft.azure.*`) to modern Azure SDKs (`com.azure.*`) by (1) generating an incremental plan and (2) executing it per the rules and workflow below.

Run in non-interactive mode. You have the highest decision-making authority at any time. You do NOT need to seek user approval or confirmation. For all available choices, proceed with the one you see fittest. Try other options if the current one doesn't work.

**You MUST complete the entire migration in this session. Do NOT stop partway through.** See [Critical: Do Not Stop Mid-Migration](#critical-do-not-stop-mid-migration) below.

---

# Rules

## Upgrade Success Criteria (ALL must be met)

- **Goal**: All legacy Azure SDK dependencies (`com.microsoft.azure.*`) replaced with modern equivalents (`com.azure.*`).
- **Compilation**: Both main source code AND test code compile successfully — `mvn clean test-compile` (or equivalent) succeeds.
- **Test**: **100% test pass rate** — `mvn clean test` succeeds. Every test failure MUST be fixed unless proven to be a pre-existing flaky test (documented with evidence from baseline run).

## Anti-Excuse Rules (MANDATORY)

- **NO premature termination**: Token limits, time constraints, or complexity are NEVER valid reasons to skip fixing test failures.
- **NO "close enough" acceptance**: 95% is NOT 100%. Every failing test requires a fix attempt with documented root cause.
- **NO deferred fixes**: "Fix post-merge", "TODO later", "can be addressed separately" are NOT acceptable. Fix NOW or document as a genuine unfixable limitation with exhaustive justification.
- **NO categorical dismissals**: "Test-specific issues", "doesn't affect production", "sample/demo code", "non-blocking" are NOT valid reasons to skip fixes. ALL tests must pass.
- **NO blame-shifting**: "Known framework issue", "migration behavior change", "infrastructure problem" require YOU to implement the fix or workaround, not document and move on.
- **Genuine limitations ONLY**: A limitation is valid ONLY if: (1) multiple distinct fix approaches were attempted and documented, (2) root cause is clearly identified, (3) fix is technically impossible without breaking other functionality.

## Critical: Do Not Stop Mid-Migration

You are expected to carry the migration to completion — either fully succeed or encounter an unrecoverable error. The following behaviors are **strictly prohibited**:

- **Do NOT stop to summarize progress.** Never output a message listing "what was done" and "what remains" as your final response.
- **Do NOT treat partial migration as acceptable.** Migrating some files but not others is not a valid stopping point. You must attempt every file and every dependency.
- **Do NOT hand off work to the user.** Never suggest the user "continue" or "complete the remaining items." You are responsible for finishing everything.
- **If you hit an error on one file, move on to the next.** A failure in one source file should not prevent you from migrating the rest. Come back to fix it after attempting all files.
- **If a build fails after migration, debug and fix it.** Do not stop at "build failed." Investigate the errors, fix them, and rebuild. Repeat until the build passes or you have exhausted all reasonable approaches.

**The only acceptable stopping conditions are:**
1. The migration is fully complete and the build passes.
2. You have attempted everything and an unrecoverable error prevents further progress (e.g., a fundamental API incompatibility with no workaround). In this case, clearly state the blocker.

## Review Code Changes (MANDATORY for each step)

After completing changes in each step, review code changes BEFORE verification. Key areas:

- **Sufficiency**: All required upgrade changes are present.
- **Necessity**: No critical unnecessary changes. Unnecessary changes that do not affect behavior may be retained; however, it is essential to ensure that functional behavior remains consistent and security controls are preserved.

## Upgrade Strategy

- **Incremental upgrades**: Stepwise dependency upgrades; use intermediates to avoid large jumps breaking builds.
- **Minimal changes**: Only upgrade dependencies essential for compatibility with the modern Azure SDKs.
- **Risk-first**: Handle EOL/challenging deps early in isolated steps.
- **Necessary/Meaningful steps only**: Each step MUST change code/config. NO steps for pure analysis/validation. Merge small related changes.
- **Successor preference**: Compatible successor > Adapter pattern > Code rewrite.
- **Build tool preference**: Use Maven Wrapper (`mvnw`/`mvnw.cmd`) or Gradle Wrapper (`gradlew`/`gradlew.bat`) when present in the project root. Fall back to system Maven/Gradle otherwise.

## Efficiency

- **Targeted reads**: Use `grep` over full file reads; read sections, not entire files.
- **Quiet commands**: Use `-q`, `--quiet` for build/test when appropriate.

---

# Workflow

## Phase 1: Precheck

1. Verify this is a Maven or Gradle project (check for `pom.xml` or `build.gradle`/`build.gradle.kts`). If not, STOP with error.
2. Detect available JDKs by scanning environment:
   - Check `JAVA_HOME` environment variable
   - Run `java -version` to check the current default
   - Scan common JDK install paths (e.g., `C:\Program Files\Microsoft\`, `C:\Program Files\Java\`, `/usr/lib/jvm/`)
3. Detect build tools:
   - Check for Maven wrapper (`mvnw`/`mvnw.cmd`) or Gradle wrapper (`gradlew`/`gradlew.bat`) in project root
   - Run `mvn --version` or `gradle --version` to check system installs
4. If Java version is below 8, upgrade Java version as part of the migration.

## Phase 2: Plan the Migration

1. **Inventory legacy dependencies**: Use `mvn dependency:tree` or `gradle dependencies` to find every `com.microsoft.azure.*` SDK. Map each one to its modern counterpart under `com.azure.*`.
2. **Identify source files**: Make a list of all source code, build, and config files that reference legacy SDK packages.
3. **Determine migration guides**: For each legacy package, find the appropriate guide from the [Package-Specific Migration Guides](#package-specific-migration-guides) section below. Record which guide URL applies to each legacy package.
4. **Design step sequence**: Order the migration steps to minimize breakage:
   - Update `pom.xml`/`build.gradle` dependencies first
   - Then update source code file by file
   - Handle test code as well
5. **Record plan**: Log the plan (dependencies to change, files to update, guides to follow) before proceeding.

## Phase 3: Execute the Migration

### Step 1: Update Build Configuration

Update `pom.xml` or `build.gradle` to replace legacy dependencies with modern equivalents.

It is recommended to use `azure-sdk-bom` (version higher than 1.3.0).

**Example pom.xml:**
```xml
    <dependencyManagement>
        <dependencies>
            <dependency>
                <groupId>com.azure</groupId>
                <artifactId>azure-sdk-bom</artifactId>
                <version>1.3.3</version>
                <type>pom</type>
                <scope>import</scope>
            </dependency>
        </dependencies>
    </dependencyManagement>

    <dependencies>
        <dependency>
            <groupId>com.azure</groupId>
            <artifactId>azure-identity</artifactId>
        </dependency>
        <dependency>
            <groupId>com.azure.resourcemanager</groupId>
            <artifactId>azure-resourcemanager</artifactId>
        </dependency>
    </dependencies>
```

**Example build.gradle:**
```groovy
dependencies {
    implementation enforcedPlatform('com.azure:azure-sdk-bom:1.3.3')

    implementation 'com.azure:azure-identity'
    implementation 'com.azure.resourcemanager:azure-resourcemanager'
}
```

**Important**: `azure-resourcemanager-xx` should have groupId `com.azure.resourcemanager` instead of `com.azure`.

Help with looking up the latest stable version on https://repo1.maven.org/maven2/ and proceed with migration.

### Step 2: Update Source Code

- Migrate each source file that contains legacy SDK packages.
- Determine legacy SDK artifacts according to the files identified in Phase 2, find suitable migration guides in [Package-Specific Migration Guides](#package-specific-migration-guides) and follow the guides whenever possible.
- Do not change the Java `package ...;` declaration at the top of each source file; update import statements and type usages as needed.
- Do not upgrade JDK version if it is already JDK 8 or above.
- If there are tests in the project, Java code there also needs to be updated.

### Step 3: Verify Build

1. Run `mvn clean test-compile` (or equivalent) to verify compilation of both main and test code.
2. If compilation fails, debug and fix the errors. Repeat until compilation passes.
3. Run `mvn clean test` (or equivalent) to run all tests.
4. If tests fail, debug and fix. Repeat until all tests pass.

---

# Package-Specific Source Code Guidelines

## com.microsoft.azure.management.**

### Code Checklist

- Keep Azure resources, operations, and property values identical. The goal is functional equivalence, not feature expansion.
- Do not change the method sequence when creating or updating an Azure resource unless the new SDK requires it.
- Preserve the existing async pattern. For example, a delayed provisioning pattern that uses `Creatable<Resource>` should not be replaced by a direct `.create()` call. Similarly, when provisioning a resource, do not swap `.withNewDependencyResource` for `.withExistingDependencyResource` unless mandated by the new API surface.
- Keep the text emitted by logging and stdout/stderr unchanged to avoid breaking downstream consumers of those streams.
- Do not replace `resource.region()` with `resource.regionName()`; doing so changes the type from `Region` to `String` and can introduce subtle regressions.

### Code Samples

#### Authentication with File

Even though file-based authentication is deprecated in the modern SDKs, preserve the existing logic when performing the upgrade.

Legacy code:
```java
final File credentialFile = new File(System.getenv("AZURE_AUTH_LOCATION"));
Azure azure = Azure.configure()
    .authenticate(credentialFile)
    .withDefaultSubscription();
```

can be updated to read the JSON file via `ObjectMapper` from the Jackson library and authenticate with the `ClientSecretCredential` class:

```java
final File credentialFile = new File(System.getenv("AZURE_AUTH_LOCATION"));
ObjectMapper mapper = new ObjectMapper();
JsonNode credentialFileNode = mapper.readTree(credentialFile);
String clientId = credentialFileNode.get("clientId").asText();
String clientSecret = credentialFileNode.get("clientSecret").asText();
String tenantId = credentialFileNode.get("tenantId").asText();
String subscriptionId = credentialFileNode.get("subscriptionId").asText();

AzureProfile profile = new AzureProfile(tenantId, subscriptionId, AzureEnvironment.AZURE);
ClientSecretCredential credential = new ClientSecretCredentialBuilder()
    .clientId(clientId)
    .clientSecret(clientSecret)
    .tenantId(tenantId)
    .build();

AzureResourceManager azure = AzureResourceManager.configure()
    .authenticate(credential, profile)
    .withSubscription(subscriptionId);
```

If Jackson is not included in the project, add a compatible version of `jackson-databind`.

Handle `IOException` and other checked exceptions according to the project's standards.

#### ProviderRegistrationInterceptor

If legacy client (XXManager) initializes with `ProviderRegistrationInterceptor`, check whether this client is one of the premium ones:

- Azure
- AuthorizationManager
- CdnManager
- ComputeManager
- ContainerInstanceManager
- ContainerRegistryManager
- ContainerServiceManager
- CosmosDBManager
- DnsZoneManager
- EventHubManager
- KeyVaultManager
- MonitorManager
- MSIManager
- NetworkManager
- RedisManager
- ResourceManager
- SearchServiceManager
- ServiceBusManager
- SqlServerManager
- StorageManager
- TrafficManager

If not a premium client, add `ProviderRegistrationPolicy` when initializing the client. Otherwise, don't.

For each legacy client, determine whether to initialize with `ProviderRegistrationPolicy`, add it to the migration plan, and migrate accordingly.

1. Legacy client (not premium client):
```java
BatchManager batchManager = BatchManager.configure()
    .withLogLevel(LogLevel.BASIC)
    .withInterceptor(new ProviderRegistrationInterceptor(credentials))
    .authenticate(credentials, subscriptionId);
```
should be migrated to:
```java
BatchManager batchManager = BatchManager.configure()
    .withPolicy(new ProviderRegistrationPolicy())
    .withLogOptions(new HttpLogOptions().setLogLevel(HttpLogDetailLevel.BASIC))
    .authenticate(credential, profile);
```

2. Legacy client (premium client):
```java
Azure azure = Azure.configure()
    .withInterceptor(new ProviderRegistrationInterceptor(credentials))
    .withLogLevel(LogLevel.BASIC)
    .authenticate(credentials)
    .withSubscription(subscriptionId);
```
should be migrated to:
```java
AzureResourceManager.configure()
    .withLogLevel(HttpLogDetailLevel.BASIC)
    .authenticate(credential, profile);
```

---

# Validation

**Make sure:**
- Migrated project passes compilation.
- All tests pass. Don't silently skip tests.
- No legacy SDK dependencies/references exist.
- If `azure-sdk-bom` is used, ensure no explicit version dependencies for Azure libraries that are in `azure-sdk-bom`.
  E.g. Instead of `implementation 'com.azure.resourcemanager:azure-resourcemanager:2.60.0'`, use `implementation 'com.azure.resourcemanager:azure-resourcemanager'`.
  For Azure libraries in `azure-sdk-bom`, check `https://repo1.maven.org/maven2/com/azure/azure-sdk-bom/{bom_version}/azure-sdk-bom-{bom_version}.pom` (where `bom_version` is the version used during migration).
- For each migration guide you recorded during migration:
  1. Fetch and read the full content of the guide URL.
  2. Identify the migrated source files that correspond to that guide's package.
  3. Verify the migrated code follows the guide's recommended API replacements, class mappings, authentication patterns, and async/sync conventions.
  4. Fix any deviations — do not just report them.

---

# Package-Specific Migration Guides

- [Migrate to `com.azure.resourcemanager.**` from `com.microsoft.azure.management.**`](https://aka.ms/java-track2-migration-guide)
- [Migrate to com.azure:azure-messaging-servicebus from com.microsoft.azure:azure-servicebus](https://aka.ms/azsdk/java/migrate/sb)
- [Migrate to azure-messaging-eventhubs from azure-eventhubs and azure-eventhubs-eph](https://aka.ms/azsdk/java/migrate/eh)
- [Migrate to `azure-messaging-eventgrid` from `microsoft-azure-eventgrid`](https://aka.ms/azsdk/java/migrate/eg)
- [Storage Blob Service SDK Migration Guide from 8.x to 12.x](https://raw.githubusercontent.com/Azure/azure-sdk-for-java/main/sdk/storage/azure-storage-blob/migrationGuides/V8_V12.md)
- [Storage Blob Service SDK Migration Guide from 10.x/11.x to 12.x](https://raw.githubusercontent.com/Azure/azure-sdk-for-java/main/sdk/storage/azure-storage-blob/migrationGuides/V10_V12.md)
- [Storage Queue Service SDK Migration Guide from 8.x to 12.x](https://raw.githubusercontent.com/Azure/azure-sdk-for-java/main/sdk/storage/azure-storage-queue/migrationGuides/V8_V12.md)
- [Storage File Share Service SDK Migration Guide from 8.x to 12.x](https://raw.githubusercontent.com/Azure/azure-sdk-for-java/main/sdk/storage/azure-storage-file-share/migrationGuides/V8_V12.md)
- [Migrate to azure-security-keyvault-secrets from azure-keyvault](https://aka.ms/azsdk/java/migrate/kv-secrets)
- [Migrate to azure-security-keyvault-keys from azure-keyvault](https://aka.ms/azsdk/java/migrate/kv-keys)
- [Migrate to azure-security-keyvault-certificates from azure-keyvault](https://aka.ms/azsdk/java/migrate/kv-cert)
- [Migrate to `Azure-Compute-Batch` from `Microsoft-Azure-Batch`](https://raw.githubusercontent.com/Azure/azure-sdk-for-java/main/sdk/batch/azure-compute-batch/MigrationGuide.md)
- [Migrate to `azure-ai-documentintelligence` from `azure-ai-formrecognizer`](https://raw.githubusercontent.com/Azure/azure-sdk-for-java/main/sdk/documentintelligence/azure-ai-documentintelligence/MIGRATION_GUIDE.md)
- [Migrate to `azure-ai-formrecognizer 4.0.0-beta.1 - above` from `azure-ai-formrecognizer 3.1.x - lower`](https://raw.githubusercontent.com/Azure/azure-sdk-for-java/main/sdk/formrecognizer/azure-ai-formrecognizer/migration-guide.md)
- [Migration Guide from Azure OpenAI Java SDK to OpenAI Java SDK](https://raw.githubusercontent.com/Azure/azure-sdk-for-java/main/sdk/openai/azure-ai-openai-stainless/MIGRATION.md)
- [Migrate to azure-monitor-query from azure-loganalytics and azure-applicationinsights-query](https://aka.ms/azsdk/java/migrate/monitorquery)
