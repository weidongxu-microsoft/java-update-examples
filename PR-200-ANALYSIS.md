# Analysis of Azure Deprecated Library Filter (PR #200)

## Executive Summary

This document analyzes the deprecated Azure Java library filter proposed in [PR #200](https://github.com/Azure/appcat-konveyor-rulesets/pull/200) of the appcat-konveyor-rulesets repository. The filter is designed to identify legacy Azure Java SDK libraries (`com.microsoft.azure.*`) that have been deprecated since March 31, 2022.

**Key Finding:** The filter has **false negatives** (misses some deprecated libraries) but **no false positives**. The primary issue is the use of an allowlist approach with explicit artifactId names, which causes it to miss deprecated libraries not explicitly listed.

---

## Current Filter Implementation

### Scope
The filter checks for deprecated Azure libraries in:
- **pom.xml** (Maven)
- **.gradle** files (Gradle)
- **.toml** files (Gradle version catalogs)
- **.java** and **.groovy** source files (import statements)

### GroupIds Checked
- `com.microsoft.azure` (primary deprecated namespace)
- `com.microsoft.azure.cognitiveservices` (via artifactId pattern)
- `com.azure` (only for the special case of `azure-search`)

### Explicitly Listed ArtifactIds
The filter explicitly lists these artifactIds:
- azure-servicebus
- azure-eventhubs
- azure-eventhubs-eph
- azure-eventgrid
- azure-storage
- azure-keyvault
- azure-batch
- azure-cosmos
- azure-cosmosdb
- azure-documentdb
- azure-documentdb-rx
- azure-client-runtime
- azure-client-authentication
- adal4j
- azure-media
- azure-search
- azure (the main management SDK)

### ArtifactId Patterns
- `starts-with(m:artifactId, 'azure-mgmt-')` - Captures management libraries
- `starts-with(m:artifactId, 'azure-cognitiveservices-')` - Captures cognitive services libraries

### Special Cases
- `com.azure:azure-search` - Legacy azure-search mistakenly published under com.azure groupId

---

## Verification Against Real-World Examples

### Deprecated Libraries Found in This Repository

#### ✅ Correctly Captured
- azure-documentdb
- azure-eventhubs  
- azure-eventhubs-eph
- azure-keyvault
- azure-servicebus
- azure-storage
- azure-client-runtime
- adal4j
- azure (main SDK)
- azure-mgmt-keyvault (via pattern)
- azure-mgmt-postgresql (via pattern)

#### ❌ **MISSED - False Negatives**

1. **azure-storage-blob** (`com.microsoft.azure:azure-storage-blob`)
   - **Status:** Deprecated ✓
   - **Found in:** azure-legacy-sdk-update-storage-v10-advanced/pom.xml
   - **Impact:** This is a commonly used legacy blob storage library
   - **Should be captured:** YES

2. **azure-management** (`com.microsoft.azure:azure-management`)
   - **Status:** Deprecated ✓
   - **Found in:** azure-legacy-sdk-update-cloud-connectors/AzureServiceBus/AzureSBRAR/pom.xml
   - **Impact:** Core management library used in service bus examples
   - **Should be captured:** YES

3. **azure-keyvault-core** (`com.microsoft.azure:azure-keyvault-core`)
   - **Status:** Deprecated ✓
   - **Found in:** azure-legacy-sdk-update-rundeck-plugins/gradle/libs.versions.toml
   - **Impact:** Core KeyVault functionality library
   - **Should be captured:** YES

4. **Libraries with service-specific groupIds** (`com.microsoft.azure.postgresql.*`)
   - **Example:** `com.microsoft.azure.postgresql.v2017_12_01:azure-mgmt-postgresql`
   - **Status:** Deprecated ✓
   - **Found in:** azure-legacy-sdk-update-postgresql-manage-server/pom.xml
   - **Impact:** Service-specific management libraries
   - **Should be captured:** YES

#### ✓ Correctly Excluded

- **azure-functions-java-library** (`com.microsoft.azure.functions:azure-functions-java-library`)
  - **Status:** NOT deprecated - Still actively maintained
  - **Found in:** azure-legacy-sdk-update-app-service-java-manage-functions/pom.xml
  - **Note:** This library is under a different groupId (`com.microsoft.azure.functions`) and is the current/modern library for Azure Functions
  - **Should be captured:** NO

---

## Issues Identified

### 1. False Negatives (Missing Deprecated Libraries)

The filter misses several deprecated libraries because it uses an **allowlist approach** rather than checking if a library starts with the deprecated groupId namespace.

**Missed Libraries:**
- `azure-storage-blob`
- `azure-management`
- `azure-keyvault-core`
- Service-specific groupIds like `com.microsoft.azure.postgresql.*`

### 2. Incomplete GroupId Coverage

The filter only checks `com.microsoft.azure` and `com.microsoft.azure.cognitiveservices`, but Azure has other deprecated groupIds:
- `com.microsoft.azure.postgresql.*`
- `com.microsoft.azure.*.v20??_??_??` (versioned service packages)

### 3. No False Positives Detected

The filter correctly excludes:
- `com.microsoft.azure.functions:azure-functions-java-library` (not checked because of different groupId)
- Modern `com.azure:*` libraries (except the correctly flagged `azure-search`)

---

## Recommendations

### Recommended Changes

#### 1. **Add Missing ArtifactIds to Explicit List**
Add to the explicit artifactId list:
```yaml
m:artifactId = 'azure-storage-blob' or
m:artifactId = 'azure-management' or
m:artifactId = 'azure-keyvault-core' or
```

#### 2. **Add Pattern for Service-Specific GroupIds**
Add a condition to check for service-specific groupIds:
```yaml
or
(starts-with(m:groupId, 'com.microsoft.azure.') and m:groupId != 'com.microsoft.azure.functions')
```

This would capture all `com.microsoft.azure.*` sub-groupIds except for the non-deprecated Functions library.

#### 3. **Consider Broader Approach (Alternative)**
Instead of maintaining an allowlist, consider using a denylist approach:
```yaml
(starts-with(m:groupId, 'com.microsoft.azure') and m:groupId != 'com.microsoft.azure.functions')
```

**Rationale:** All libraries under `com.microsoft.azure` namespace (except `com.microsoft.azure.functions`) are deprecated. This approach:
- ✅ Catches all current and future deprecated libraries
- ✅ Easier to maintain
- ✅ Reduces risk of missing libraries
- ⚠️ Requires explicit exclusion of `com.microsoft.azure.functions`

#### 4. **Update Source Code Patterns**
Update the Java import pattern to include:
```
com\.microsoft\.azure\.management\..*
com\.microsoft\.azure\.storage\.blob\..*
```

Update the Gradle pattern similarly.

---

## Verification Testing

### Test Cases to Add

1. **Test for azure-storage-blob:**
   ```xml
   <dependency>
       <groupId>com.microsoft.azure</groupId>
       <artifactId>azure-storage-blob</artifactId>
       <version>11.0.1</version>
   </dependency>
   ```

2. **Test for azure-management:**
   ```xml
   <dependency>
       <groupId>com.microsoft.azure</groupId>
       <artifactId>azure-management</artifactId>
       <version>1.41.4</version>
   </dependency>
   ```

3. **Test for azure-keyvault-core:**
   ```xml
   <dependency>
       <groupId>com.microsoft.azure</groupId>
       <artifactId>azure-keyvault-core</artifactId>
       <version>1.0.0</version>
   </dependency>
   ```

4. **Test for service-specific groupId:**
   ```xml
   <dependency>
       <groupId>com.microsoft.azure.postgresql.v2017_12_01</groupId>
       <artifactId>azure-mgmt-postgresql</artifactId>
       <version>1.0.0-beta</version>
   </dependency>
   ```

5. **Test that azure-functions-java-library is NOT flagged:**
   ```xml
   <dependency>
       <groupId>com.microsoft.azure.functions</groupId>
       <artifactId>azure-functions-java-library</artifactId>
       <version>3.0.0</version>
   </dependency>
   ```

---

## References

- [Azure SDK Deprecated Releases](https://azure.github.io/azure-sdk/releases/deprecated/index.html)
- [Azure SDK for Java Migration Guide](https://aka.ms/java-track2-migration-guide)
- [Azure for Java Developer Documentation](https://learn.microsoft.com/azure/developer/java/)
- [Azure SDK Lifecycle and Support Policy](https://azure.github.io/azure-sdk/policies_support.html)

---

## Summary

The filter in PR #200 is a good start but has **false negatives** that need to be addressed:

1. **Add missing artifactIds:** azure-storage-blob, azure-management, azure-keyvault-core
2. **Add pattern for service-specific groupIds:** `com.microsoft.azure.*` (except functions)
3. **Consider switching to broader approach:** Check if groupId starts with `com.microsoft.azure` (with explicit exclusion for functions)

These changes will ensure the filter catches all deprecated Azure Java SDK libraries while not flagging the modern Azure Functions library.
