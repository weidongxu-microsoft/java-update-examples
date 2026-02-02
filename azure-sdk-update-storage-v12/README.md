# Azure Storage V12 Blob Operations

This project demonstrates how the Azure Storage SDK v12 (Track 2) uses `BlobServiceClient`, `BlobContainerClient`, and `BlobClient` to perform blob lifecycle operations. The code showcases the modern Azure SDK design principles with simplified APIs and improved developer experience.

## Prerequisites

- Java 8+
- Maven 3.8+
- Storage account credentials (account name + account key).

## Configuration

Set the following environment variables before running commands:

- `STORAGE_V12_ACCOUNT_NAME` – storage account name.
- `STORAGE_V12_ACCOUNT_KEY` – primary or secondary account key.
- `STORAGE_V12_CONTAINER` (optional) – defaults to `documents-v12` if omitted.

## Build and Run

```bash
mvn clean package
java -jar target/azure-storage-v12-app-1.0.0.jar
```

The executable jar kicks off the default lifecycle scenario: create the container if needed, upload a text blob, download it back, and delete it.

### Commands via Maven Exec

The `StorageV12Runner` entry point also exposes utility commands through the exec plugin:

- Lifecycle demo: `mvn exec:java`
- List blobs: `mvn exec:java -Dexec.args="list"`
- Download blob to a path: `mvn exec:java -Dexec.args="download <blobName> <destinationPath>"`

## Tests

```bash
mvn test
```

Mockito's inline mock maker is enabled so that the Azure SDK v12 classes can be mocked safely.
