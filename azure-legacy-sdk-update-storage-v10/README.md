# Azure Storage V10 Blob Operations

This project demonstrates how the Azure Storage SDK v10 (a.k.a. Track 1) composes `ServiceURL`, `ContainerURL`, and `BlockBlobURL` primitives to perform blob lifecycle operations. The code mirrors the APIs referenced in the [V10 to V12 migration guide](https://github.com/Azure/azure-sdk-for-java/blob/main/sdk/storage/azure-storage-blob/migrationGuides/V10_V12.md) so that you can compare the older fluent style with modern Track 2 clients.

## Prerequisites

- Java 8+
- Maven 3.8+
- Storage account credentials (account name + account key).

## Configuration

Set the following environment variables before running commands:

- `STORAGE_V10_ACCOUNT_NAME` – storage account name.
- `STORAGE_V10_ACCOUNT_KEY` – primary or secondary account key.
- `STORAGE_V10_CONTAINER` (optional) – defaults to `documents-v10` if omitted.

## Build and Run

```bash
mvn clean package
java -jar target/azure-storage-v10-app-1.0.0.jar
```

The executable jar kicks off the default lifecycle scenario: create the container if needed, upload a text blob, download it back, and delete it.

### Commands via Maven Exec

The `StorageV10Runner` entry point also exposes utility commands through the exec plugin:

- Lifecycle demo: `mvn exec:java`
- List blobs: `mvn exec:java -Dexec.args="list"`
- Download blob to a path: `mvn exec:java -Dexec.args="download <blobName> <destinationPath>"`

## Tests

```bash
mvn test
```

Mockito’s inline mock maker is enabled so that the final Azure SDK v10 classes can be mocked safely.
