# Azure Storage V10 Concurrent Upload

Demonstrates concurrent blob upload operations with asynchronous support using Azure Storage SDK v10.

## Features

- **Concurrent Upload**: Upload files using multiple parallel operations for better performance
- **Async Operations**: Non-blocking upload operations using RxJava reactive streams
- **Configurable Transfer Options**: Customize block size and concurrency level
- **Stream Upload**: Upload data from Flowable streams with buffering support
- **File Upload**: Upload files directly from disk with automatic chunking

## Key Classes

- **ConcurrentUploadManager**: Manages concurrent blob upload operations with configurable block size and concurrency
- **ConcurrentUploadRunner**: Demonstrates various upload scenarios

## Transfer Configuration

The manager uses `TransferManager` from Azure Storage SDK v10 with:
- Configurable block size (default: 4 MB)
- Configurable max concurrency (default: 5 concurrent operations)
- Support for both known and unknown content lengths

## Usage

### Basic File Upload

```java
ConcurrentUploadManager manager = ConcurrentUploadManager.fromAccount(
    accountName, accountKey);

// Synchronous upload
manager.uploadFile("container", "blob.dat", Paths.get("file.dat"));
```

### Async File Upload

```java
manager.uploadFileAsync("container", "blob.dat", filePath)
    .subscribe(
        response -> System.out.println("Upload complete: " + response.headers().eTag()),
        error -> System.err.println("Upload failed: " + error.getMessage())
    );
```

### Stream Upload with Concurrency

```java
Flowable<ByteBuffer> dataStream = // ... your data source
long totalSize = // ... total bytes

manager.uploadFromFlowableAsync("container", "blob.dat", dataStream, totalSize)
    .subscribe(
        response -> System.out.println("Upload complete"),
        error -> System.err.println("Upload failed")
    );
```

### Custom Transfer Options

```java
// 2MB blocks, 8 concurrent operations
ConcurrentUploadManager manager = ConcurrentUploadManager.fromAccount(
    accountName, accountKey, 2 * 1024 * 1024, 8);
```

## Running the Demo

Set environment variables:
```bash
export AZURE_STORAGE_ACCOUNT_NAME=your_account_name
export AZURE_STORAGE_ACCOUNT_KEY=your_account_key
```

Run with Maven:
```bash
mvn clean compile exec:java
```

## Building

```bash
mvn clean package
```

## Testing

```bash
mvn test
```

## Dependencies

- **azure-storage-blob**: 10.5.0 (Azure Storage SDK v10)
- **rxjava**: 2.2.21 (Reactive Extensions for JVM)
- **junit**: 4.13.2
- **mockito-core**: 4.11.0

## Comparison with Other SDK Versions

- **V8**: Uses CloudBlockBlob with synchronous operations
- **V10**: Uses URL-based clients (BlockBlobURL) with RxJava reactive streams
- **V12**: Uses BlobClient with Project Reactor and ParallelTransferOptions

This project demonstrates V10's concurrent upload capabilities using `TransferManager` with configurable block size and concurrency, similar to V12's `ParallelTransferOptions` but using RxJava instead of Project Reactor.
