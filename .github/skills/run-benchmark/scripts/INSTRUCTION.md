Upgrade deprecated legacy Azure Java SDKs(`com.microsoft.azure`) used for this project to the modern ones(`com.azure`) with latest stable version using Java Upgrade tools by invoking #generate_upgrade_plan.
Help with looking up the latest stable version on https://repo1.maven.org/maven2/ and proceed with migration. `azure-resourcemanager-xx` should have groupId `com.azure.resourcemanager` instead of `com.azure`.
If upgrading Java version is necessary, upgrade Java version within the same #generate_upgrade_plan call.
For all available choices, just proceed with the one you see fittest. Try other options if current one doesn't work. Don't stop until full migration is done.
Upgrade both dependencies and source code.
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

- [Reference for update Java code](https://github.com/Azure/azure-sdk-for-java/blob/main/sdk/resourcemanager/docs/MIGRATION_GUIDE.md)
- Make a list of Java files that contains `com.microsoft.azure` package. Migrate each of them.
- Do not modify package name in file.
- Do not upgrade JDK version, if it is already JDK 8 or above.
- Keep Azure resource and operation exactly same. The property on the Azure resource should not be changed, for "improvement" or "modernization". The focus here is to use the new libaray to create the exactly same Azure resource, apply the exactly same operation on it.
- If there is test in the project, Java code there also need to be updated.

## Validation

Make sure the migrated project compile pass.


The application is using deprecated Azure legacy Java SDKs (`com.microsoft.azure.*`). These libraries reached end of support on 31-Mar-2022, so you should migrate to the supported Azure SDKs (`com.azure.*`) for security fixes and new capabilities. Follow these steps:

* **Inventory legacy dependencies**: Use tools such as `mvn dependency:tree` or `gradlew dependencies` to find every `com.microsoft.azure.*` artifact and map each one to its modern counterpart under `com.azure.*`.

* **Adopt supported packages**: Replace the legacy dependencies with their modern equivalents in your `pom.xml` or `build.gradle`, following the migration guide to align feature parity and new package names.

* **Update application code**: Refactor your code to the builder-based APIs, updated authentication flows (Azure Identity), and modern async or reactive patterns required by the latest clients. Add concise comments explaining non-obvious changes.

* **Test thoroughly**: Run unit, integration, and end-to-end tests to validate that the modern clients behave as expected, focusing on authentication, retry, and serialization differences.
