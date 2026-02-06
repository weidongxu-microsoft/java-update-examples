Upgrade deprecated legacy Azure Java SDKs(`com.microsoft.azure`) used for this project to the modern ones(`com.azure`) with latest stable version using Java Upgrade tools by invoking #generate_upgrade_plan.
Help with looking up the latest stable version on https://repo1.maven.org/maven2/ and proceed with migration. `azure-resourcemanager-xx` should have groupId `com.azure.resourcemanager` instead of `com.azure`.
If upgrading Java version is necessary, upgrade Java version within the same #generate_upgrade_plan call.
For all available choices, just proceed with the one you see fittest. Try other options if current one doesn't work. Don't stop until full migration is done.
Upgrade both dependencies and source code.

# Context

The application is using deprecated Azure legacy Java SDKs (`com.microsoft.azure.*`). These libraries reached end of support on 31-Mar-2022, so you should migrate to the supported Azure SDKs (`com.azure.*`) for security fixes and new capabilities. Follow these steps:

* **Inventory legacy dependencies**: Use tools such as `mvn dependency:tree` or `gradlew dependencies` to find every `com.microsoft.azure.*` artifact and map each one to its modern counterpart under `com.azure.*`.

* **Adopt supported packages**: Replace the legacy dependencies with their modern equivalents in your `pom.xml` or `build.gradle`, following the migration guide to align feature parity and new package names.

* **Update application code**: Refactor your code to the builder-based APIs, updated authentication flows (Azure Identity), and modern async or reactive patterns required by the latest clients. Add concise comments explaining non-obvious changes.

* **Test thoroughly**: Run unit, integration, and end-to-end tests to validate that the modern clients behave as expected, focusing on authentication, retry, and serialization differences.

# Migration Guide

## Assumption

- Project is Maven or Gradle.
- Java code is on JDK 8 or above.

## Migrate pom.xml

It is recommended to use azure-sdk-bom (version higher than 1.3.0).

Example of pom.xml
```
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
Example of build.gradle
```
dependencies {
    implementation enforcedPlatform('com.azure:azure-sdk-bom:1.3.3')

    implementation 'com.azure:azure-identity'
    implementation 'com.azure.resourcemanager:azure-resourcemanager'
}
```

## Migrate Java Code

- Make a list of Java files that contains `com.microsoft.azure` package. Migrate each of them.
- Follow [Migration Guides](#appendix-migration-guides-for-specific-packages) whenever possible.
- Do not modify package name in file.
- Do not upgrade JDK version, if it is already JDK 8 or above.
- If there is test in the project, Java code there also need to be updated.

## Code Checklist

- Keep Azure resources, operations, and property values identical. The goal is functional equivalence, not feature expansion.
- Do not change the method sequence when creating or updating an Azure resource unless the new SDK requires it.
- Preserve the existing async pattern. For example, a delayed provisioning pattern that uses `Creatable<Resource>` should not be replaced by a direct `.create()` call. Similarly, when provisioning a resource, do not swap `.withNewDependencyResource` for `.withExistingDependencyResource` unless mandated by the new API surface.
- Keep the text emitted by logging and stdout/stderr unchanged to avoid breaking downstream consumers of those streams.
- Do not replace `resource.region()` with `resource.regionName()`; doing so changes the type from `Region` to `String` and can introduce subtle regressions.

## Code Samples

### Authentication with File
Even though file-based authentication is deprecated in the modern SDKs, preserve the existing logic when performing the upgrade.

Legacy code
```java
final File credentialFile = new File(System.getenv("AZURE_AUTH_LOCATION"));
Azure azure = Azure.configure()
    .authenticate(credentialFile)
    .withDefaultSubscription();
```
can be updated to read the JSON file via `ObjectMapper` from the Jackson library and authenticate with the `ClientSecretCredential` class.
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

## Validation

**Make sure**
- Migrated project pass compilation.
- All tests pass. Don't silently skip tests.
- No legacy SDK dependencies/references exist.

## Appendix: Migration Guides for specific packages

- [Migrate to `com.azure.resourcemanager.**` from `com.microsoft.azure.management.**`](https://raw.githubusercontent.com/Azure/azure-sdk-for-java/main/sdk/resourcemanager/docs/MIGRATION_GUIDE.md)
- [Migrate to com.azure:azure-messaging-servicebus from com.microsoft.azure:azure-servicebus](https://aka.ms/azsdk/java/migrate/sb)
- [Migrate to azure-messaging-eventhubs from azure-eventhubs and azure-eventhubs-eph](https://aka.ms/azsdk/java/migrate/eh)
- [Migrate to `azure-messaging-eventgrid` from `microsoft-azure-eventgrid`](https://raw.githubusercontent.com/Azure/azure-sdk-for-java/main/sdk/eventgrid/azure-messaging-eventgrid/migration-guide.md)
- [Storage Blob Service SDK Migration Guide from 8.x to 12.x](https://raw.githubusercontent.com/Azure/azure-sdk-for-java/main/sdk/storage/azure-storage-blob/migrationGuides/V8_V12.md)
- [Storage Blob Service SDK Migration Guide from 10.x/11.x to 12.x](https://raw.githubusercontent.com/Azure/azure-sdk-for-java/main/sdk/storage/azure-storage-blob/migrationGuides/V10_V12.md)
- [Storage Queue Service SDK Migration Guide from 8.x to 12.x](https://raw.githubusercontent.com/Azure/azure-sdk-for-java/main/sdk/storage/azure-storage-queue/migrationGuides/V8_V12.md)
- [Storage File Share Service SDK Migration Guide from 8.x to 12.x](https://raw.githubusercontent.com/Azure/azure-sdk-for-java/main/sdk/storage/azure-storage-file-share/migrationGuides/V8_V12.md)
- [Migrate to azure-security-keyvault-secrets from azure-keyvault](https://raw.githubusercontent.com/Azure/azure-sdk-for-java/main/sdk/keyvault/azure-security-keyvault-secrets/migration_guide.md)
- [Migrate to azure-security-keyvault-keys from azure-keyvault](https://raw.githubusercontent.com/Azure/azure-sdk-for-java/main/sdk/keyvault/azure-security-keyvault-keys/migration_guide.md)
- [Migrate to azure-security-keyvault-certificates from azure-keyvault](https://raw.githubusercontent.com/Azure/azure-sdk-for-java/main/sdk/keyvault/azure-security-keyvault-certificates/migration_guide.md)
- [Migrate to `Azure-Compute-Batch` from `Microsoft-Azure-Batch`](https://raw.githubusercontent.com/Azure/azure-sdk-for-java/main/sdk/batch/azure-compute-batch/MigrationGuide.md)
- [Migrate to `azure-ai-documentintelligence` from `azure-ai-formrecognizer`](https://raw.githubusercontent.com/Azure/azure-sdk-for-java/main/sdk/documentintelligence/azure-ai-documentintelligence/MIGRATION_GUIDE.md)
- [Migrate to `azure-ai-formrecognizer (4.0.0-beta.1 - above)` from `azure-ai-formrecognizer (3.1.x - lower)](https://raw.githubusercontent.com/Azure/azure-sdk-for-java/main/sdk/formrecognizer/azure-ai-formrecognizer/migration-guide.md)
- [Migration Guide from Azure OpenAI Java SDK to OpenAI Java SDK](https://raw.githubusercontent.com/Azure/azure-sdk-for-java/main/sdk/openai/azure-ai-openai-stainless/MIGRATION.md)



