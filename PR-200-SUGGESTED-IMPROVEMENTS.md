# Suggested Improvements for PR #200 Filter

## Overview
This document provides concrete suggestions for improving the deprecated Azure library filter in PR #200.

## Option 1: Add Missing Artifacts (Minimal Change)

Add these missing artifactIds to the existing explicit list in the XPath condition:

```yaml
when:
  or:
  - and:
    - builtin.file:
        pattern: pom.xml
      as: poms
      ignore: true
    - builtin.xml:
        filepaths: "{{poms.Filepaths}}"
        from: poms
        namespaces:
          m: http://maven.apache.org/POM/4.0.0
        xpath: >-
          //m:dependency[
            (starts-with(m:groupId, 'com.microsoft.azure') and (
              m:artifactId = 'azure-servicebus' or
              m:artifactId = 'azure-eventhubs' or
              m:artifactId = 'azure-eventhubs-eph' or
              m:artifactId = 'azure-eventgrid' or
              m:artifactId = 'azure-storage' or
              m:artifactId = 'azure-storage-blob' or         # ADD THIS
              m:artifactId = 'azure-keyvault' or
              m:artifactId = 'azure-keyvault-core' or        # ADD THIS
              m:artifactId = 'azure-batch' or
              m:artifactId = 'azure-cosmos' or
              m:artifactId = 'azure-cosmosdb' or
              m:artifactId = 'azure-documentdb' or
              m:artifactId = 'azure-documentdb-rx' or
              m:artifactId = 'azure-client-runtime' or
              m:artifactId = 'azure-client-authentication' or
              m:artifactId = 'adal4j' or
              m:artifactId = 'azure-media' or
              m:artifactId = 'azure-search' or
              m:artifactId = 'azure-management' or           # ADD THIS
              m:artifactId = 'azure' or
              starts-with(m:artifactId, 'azure-mgmt-') or
              starts-with(m:artifactId, 'azure-cognitiveservices-')
            ))
            or
            (m:groupId = 'com.azure' and m:artifactId = 'azure-search')
          ]
```

**Also update the Gradle pattern:**
```
(com\.microsoft\.azure(\.cognitiveservices)?:(azure-servicebus|azure-eventhubs|azure-eventhubs-eph|azure-eventgrid|azure-storage|azure-storage-blob|azure-keyvault|azure-keyvault-core|azure-batch|azure-cosmos|azure-cosmosdb|azure-documentdb|azure-documentdb-rx|azure-client-runtime|azure-client-authentication|adal4j|azure-media|azure-search|azure-management|azure|azure-mgmt-[a-z]+|azure-cognitiveservices-[a-z]+)|com\.azure:azure-search):
```

**Also update the TOML pattern:**
```
((group\s*=\s*"com\.microsoft\.azure(\.cognitiveservices)?"\s*,\s*name\s*=\s*"|module\s*=\s*"com\.microsoft\.azure(\.cognitiveservices)?:)(azure-servicebus|azure-eventhubs|azure-eventhubs-eph|azure-eventgrid|azure-storage|azure-storage-blob|azure-keyvault|azure-keyvault-core|azure-batch|azure-cosmos|azure-cosmosdb|azure-documentdb|azure-documentdb-rx|azure-client-runtime|azure-client-authentication|adal4j|azure-media|azure-search|azure-management|azure|azure-mgmt-[a-z]+|azure-cognitiveservices-[a-z]+)"|group\s*=\s*"com\.azure"\s*,\s*name\s*=\s*"azure-search"|module\s*=\s*"com\.azure:azure-search")
```

**Also update the Java import pattern:**
```
(import\s+(?:static\s+)?(com\.azure\.search(?!\.documents)|com\.microsoft\.(?:aad\.adal4j|windowsazure|rest|azure\.(?:batch|cognitiveservices|cosmosdb|documentdb|eventgrid|eventhubs|eventprocessorhost|keyvault|servicebus|credentials|management|serializer|storage\.blob))))
```

---

## Option 2: Broader Approach (Recommended)

Use a more comprehensive approach that catches all `com.microsoft.azure.*` groupIds except for non-deprecated ones:

```yaml
when:
  or:
  - and:
    - builtin.file:
        pattern: pom.xml
      as: poms
      ignore: true
    - builtin.xml:
        filepaths: "{{poms.Filepaths}}"
        from: poms
        namespaces:
          m: http://maven.apache.org/POM/4.0.0
        xpath: >-
          //m:dependency[
            (
              (starts-with(m:groupId, 'com.microsoft.azure') and 
               m:groupId != 'com.microsoft.azure.functions' and (
                m:artifactId = 'azure-servicebus' or
                m:artifactId = 'azure-eventhubs' or
                m:artifactId = 'azure-eventhubs-eph' or
                m:artifactId = 'azure-eventgrid' or
                m:artifactId = 'azure-storage' or
                m:artifactId = 'azure-storage-blob' or
                m:artifactId = 'azure-keyvault' or
                m:artifactId = 'azure-keyvault-core' or
                m:artifactId = 'azure-batch' or
                m:artifactId = 'azure-cosmos' or
                m:artifactId = 'azure-cosmosdb' or
                m:artifactId = 'azure-documentdb' or
                m:artifactId = 'azure-documentdb-rx' or
                m:artifactId = 'azure-client-runtime' or
                m:artifactId = 'azure-client-authentication' or
                m:artifactId = 'adal4j' or
                m:artifactId = 'azure-media' or
                m:artifactId = 'azure-search' or
                m:artifactId = 'azure-management' or
                m:artifactId = 'azure' or
                starts-with(m:artifactId, 'azure-mgmt-') or
                starts-with(m:artifactId, 'azure-cognitiveservices-')
              ))
            )
            or
            (m:groupId = 'com.azure' and m:artifactId = 'azure-search')
          ]
```

**Key changes:**
1. Added explicit check: `m:groupId != 'com.microsoft.azure.functions'`
2. Changed first condition to `starts-with(m:groupId, 'com.microsoft.azure')` to catch service-specific groupIds
3. This catches packages like `com.microsoft.azure.postgresql.v2017_12_01`

---

## Option 3: Most Comprehensive (Future-Proof)

For maximum coverage and future-proofing, check for the deprecated namespace prefix and explicitly exclude known non-deprecated libraries:

```yaml
when:
  or:
  - and:
    - builtin.file:
        pattern: pom.xml
      as: poms
      ignore: true
    - builtin.xml:
        filepaths: "{{poms.Filepaths}}"
        from: poms
        namespaces:
          m: http://maven.apache.org/POM/4.0.0
        xpath: >-
          //m:dependency[
            (
              starts-with(m:groupId, 'com.microsoft.azure') and 
              not(starts-with(m:groupId, 'com.microsoft.azure.functions'))
            )
            or
            (m:groupId = 'com.azure' and m:artifactId = 'azure-search')
          ]
```

**Advantages:**
- ✅ Catches ALL deprecated libraries under `com.microsoft.azure.*`
- ✅ Automatically catches new deprecated libraries (if any are found)
- ✅ Explicitly excludes the Functions namespace
- ✅ Simplest to maintain
- ✅ No need to update list when new deprecated libraries are discovered

**Disadvantages:**
- ⚠️ Might be too broad if Microsoft introduces new non-deprecated libraries under `com.microsoft.azure` (unlikely)

---

## Corresponding Updates for Other File Types

### For Gradle (.gradle files):
```regex
com\.microsoft\.azure(?!\.functions)[^:]*:[^:]+:
```

### For Gradle Version Catalogs (.toml files):
```regex
(group\s*=\s*"com\.microsoft\.azure(?!\.functions)[^"]*"|module\s*=\s*"com\.microsoft\.azure(?!\.functions)[^"]*:[^"]+")
```

### For Java Source Files (.java):
```regex
import\s+(?:static\s+)?com\.microsoft\.azure\.(?!functions\b)[a-z0-9_.]+\b
```

---

## Test Cases

Add these test cases to verify the filter works correctly:

### Should Flag (Positive Cases):
```xml
<!-- Basic deprecated library -->
<dependency>
    <groupId>com.microsoft.azure</groupId>
    <artifactId>azure</artifactId>
</dependency>

<!-- Storage blob (missed by current filter) -->
<dependency>
    <groupId>com.microsoft.azure</groupId>
    <artifactId>azure-storage-blob</artifactId>
</dependency>

<!-- Management (missed by current filter) -->
<dependency>
    <groupId>com.microsoft.azure</groupId>
    <artifactId>azure-management</artifactId>
</dependency>

<!-- KeyVault core (missed by current filter) -->
<dependency>
    <groupId>com.microsoft.azure</groupId>
    <artifactId>azure-keyvault-core</artifactId>
</dependency>

<!-- Service-specific groupId (missed by current filter) -->
<dependency>
    <groupId>com.microsoft.azure.postgresql.v2017_12_01</groupId>
    <artifactId>azure-mgmt-postgresql</artifactId>
</dependency>

<!-- Legacy azure-search under com.azure -->
<dependency>
    <groupId>com.azure</groupId>
    <artifactId>azure-search</artifactId>
</dependency>
```

### Should NOT Flag (Negative Cases):
```xml
<!-- Modern Azure SDK -->
<dependency>
    <groupId>com.azure</groupId>
    <artifactId>azure-storage-blob</artifactId>
</dependency>

<!-- Azure Functions (NOT deprecated) -->
<dependency>
    <groupId>com.microsoft.azure.functions</groupId>
    <artifactId>azure-functions-java-library</artifactId>
</dependency>

<!-- Modern search -->
<dependency>
    <groupId>com.azure</groupId>
    <artifactId>azure-search-documents</artifactId>
</dependency>
```

---

## Recommendation

I recommend **Option 3** (Most Comprehensive) as it:
1. Provides the most complete coverage
2. Is future-proof
3. Is easier to maintain
4. Clearly documents the intent (all of `com.microsoft.azure` is deprecated except Functions)
5. Matches Microsoft's actual deprecation policy (entire namespace is legacy)

However, if you prefer to maintain explicit control, **Option 1** (Minimal Change) addresses the immediate false negatives while keeping the current structure.
