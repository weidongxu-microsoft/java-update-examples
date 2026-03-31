# Secure Data Pipeline

Encrypted document storage using Azure Key Vault for key management and Azure Blob Storage for persistence.

## Overview

This project demonstrates a realistic enterprise pattern: encrypting data with keys managed in Azure Key Vault before storing it in Azure Blob Storage. It uses the Track 1 (legacy) Azure SDKs.

### Legacy Azure SDKs Used

| Artifact | Version | Purpose |
|----------|---------|---------|
| `com.microsoft.azure:azure-storage` | 8.6.6 | Blob upload/download/list/delete |
| `com.microsoft.azure:azure-keyvault` | 1.2.6 | Key management and server-side crypto |

### Migration Targets

| Legacy | Modern Replacement |
|--------|-------------------|
| `com.microsoft.azure:azure-storage` | `com.azure:azure-storage-blob` (v12) |
| `com.microsoft.azure:azure-keyvault` | `com.azure:azure-security-keyvault-keys` + `azure-security-keyvault-secrets` |

## Architecture

```
┌──────────────────────────────────────────────────────────────┐
│                   SecureDataPipelineRunner                    │
│                        (main class)                          │
├──────────────────────────────────────────────────────────────┤
│                                                              │
│  ┌─────────────────────┐    ┌──────────────────────────┐    │
│  │  EncryptedBlobStore  │───▶│  EncryptionKeyProvider   │    │
│  │  (Storage + KV)      │    │  (interface)             │    │
│  └─────────┬───────────┘    └────────────▲─────────────┘    │
│            │                             │                   │
│            │                ┌────────────┴─────────────┐    │
│            │                │ KeyVaultEncryptionKey-    │    │
│            │                │ Provider (KV Track 1)    │    │
│            │                └──────────────────────────┘    │
│            │                                                 │
│  ┌─────────▼───────────┐    ┌──────────────────────────┐    │
│  │   CloudBlobClient   │    │  AzureResourceResolver<T>│    │
│  │   (Track 1 v8)      │    │  (generic interface)     │    │
│  └─────────────────────┘    └────────────▲─────────────┘    │
│                                          │                   │
│  ┌─────────────────────┐    ┌────────────┴─────────────┐    │
│  │  ResourceCache<T>   │    │ KeyVaultSecretResolver   │    │
│  │  (JSON serialized)  │    │ (SecretBundle bound)     │    │
│  └─────────────────────┘    └──────────────────────────┘    │
│                                                              │
│  ┌─────────────────────┐    ┌──────────────────────────┐    │
│  │  AzureClientFactory │    │ PipelineException-       │    │
│  │  (reflection-based) │    │ Translator               │    │
│  └─────────────────────┘    └──────────────────────────┘    │
└──────────────────────────────────────────────────────────────┘
```

## Migration Difficulty

This project is intentionally designed with patterns that make automated SDK migration challenging:

### 1. Generic Interfaces with SDK Type Bounds
`AzureResourceResolver<T>` is parameterized on `SecretBundle` (Track 1 type). Changing it to `KeyVaultSecret` (Track 2) requires updating all implementations, callers, and method bodies.

### 2. Cross-Service Data Flow
`EncryptedBlobStore` combines Key Vault crypto operations with Storage uploads in a single workflow. You cannot migrate one service without the other.

### 3. JSON Serialization of SDK Types
`ResourceCache<SecretBundle>` serializes Track 1 model objects to JSON. The JSON field names are SDK-version-specific.

### 4. SDK Exception Translation
`PipelineExceptionTranslator` maps `StorageException` and `KeyVaultErrorException` using Track 1-specific methods (`getHttpStatusCode()`, `body().error().code()`). Track 2 exceptions have completely different APIs.

### 5. Reflection-Based Client Factory
`AzureClientFactory` references SDK class names as strings (`"com.microsoft.azure.keyvault.KeyVaultClient"`). Import-based scanning tools won't detect these.

### 6. Cross-Service Metadata Coupling
Blob metadata stores Key Vault key identifiers and algorithm names. Format changes between SDK versions break the retrieval path.

### 7. Interface Contracts with SDK Return Types
`EncryptionKeyProvider` returns `KeyBundle` and `KeyOperationResult` (Track 1 types). Track 2 uses `KeyVaultKey` and separate `EncryptResult`/`DecryptResult` types.

## Build and Test

```bash
mvn clean test
```

## Project Structure

```
src/main/java/com/azure/datapipeline/
├── AzureClientFactory.java           # Reflection-based client factory
├── AzureResourceResolver.java        # Generic interface (parameterized on SDK types)
├── EncryptedBlobStore.java           # Cross-service encrypted storage
├── EncryptionKeyProvider.java        # Interface with SDK return types
├── KeyVaultEncryptionKeyProvider.java # Key Vault implementation
├── KeyVaultSecretResolver.java       # AzureResourceResolver<SecretBundle>
├── PipelineConfiguration.java        # Environment-based configuration
├── PipelineException.java            # Unified exception model
├── PipelineExceptionTranslator.java  # SDK exception mapping
├── ResourceCache.java                # JSON serialization cache
└── SecureDataPipelineRunner.java     # Main entry point

src/test/java/com/azure/datapipeline/
├── AzureClientFactoryTest.java
├── EncryptedBlobStoreTest.java
├── KeyVaultEncryptionKeyProviderTest.java
├── KeyVaultSecretResolverTest.java
├── PipelineExceptionTranslatorTest.java
└── ResourceCacheTest.java
```
