# Azure Storage V8 Blob Operations

This project manages Azure Blob Storage resources with the `azure-storage` v8 SDK. It mirrors the object model documented in the [V8 to V12 migration guide](https://github.com/Azure/azure-sdk-for-java/blob/main/sdk/storage/azure-storage-blob/migrationGuides/V8_V12.md) so you can compare the older API surface with the modern client libraries.

## Prerequisites

- Java 8+
- Maven 3.8+
- An Azure Storage account with a valid connection string.

## Configuration

Set the following environment variables before running the application:

- `AZURE_STORAGE_CONNECTION_STRING` – full connection string for the storage account.
- `STORAGE_V8_CONTAINER` (optional) – defaults to `documents-container` if not set.
- `STORAGE_V8_QUEUE` (optional) – queue name for the legacy queue client, defaults to `notifications-queue`.
- `STORAGE_V8_SHARE` (optional) – file share name for the CloudFile client, defaults to `documents-share`.

## Build and Run

```bash
mvn clean package
java -jar target/azure-storage-v8-app-1.0.0.jar
```

The `StorageV8Runner` class ensures the container exists, uploads a text blob, downloads it back, and deletes it to keep the account tidy. After the blob workflow completes it also demonstrates the classic queue client (enqueue, peek, clear) and the file share client (upload, download, delete) so you can compare the full storage surface area with the migration guides for [queues](https://github.com/Azure/azure-sdk-for-java/blob/main/sdk/storage/azure-storage-queue/migrationGuides/V8_V12.md) and [file shares](https://github.com/Azure/azure-sdk-for-java/blob/main/sdk/storage/azure-storage-file-share/migrationGuides/V8_V12.md).

### Commands

Use the Maven exec plugin to call additional commands without wiring your own classpath:

- Lifecycle demo: `mvn exec:java`
- List blobs: `mvn exec:java -Dexec.args="list"`
- Download blob to a file: `mvn exec:java -Dexec.args="download <blobName> <destinationPath>"`

## Tests

```bash
mvn test
```

Unit tests rely on Mockito (inline mock maker) to mock the final classes in the v8 SDK.
