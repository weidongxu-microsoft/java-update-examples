package com.azure.datapipeline;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.microsoft.azure.keyvault.KeyVaultClient;
import com.microsoft.azure.keyvault.models.KeyBundle;
import com.microsoft.azure.keyvault.models.SecretBundle;
import com.microsoft.azure.storage.blob.CloudBlobClient;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

/**
 * Demonstrates the secure data pipeline workflow: encrypt documents with
 * Key Vault managed keys and store them in Azure Blob Storage.
 */
public class SecureDataPipelineRunner {

    public static void main(String[] args) {
        System.out.println("=== Secure Data Pipeline ===");
        System.out.println();

        PipelineConfiguration config = new PipelineConfiguration();
        if (!config.isValid()) {
            System.err.println("Invalid configuration. Set the following environment variables:");
            System.err.println("  KEY_VAULT_URL - Azure Key Vault URL");
            System.err.println("  STORAGE_CONNECTION_STRING - Azure Storage connection string");
            System.err.println("  STORAGE_CONTAINER_NAME - Blob container name");
            System.err.println("  ENCRYPTION_KEY_NAME - Key Vault encryption key name");
            return;
        }

        try {
            AzureClientFactory factory = new AzureClientFactory();
            factory.validateAllClientsAvailable();
            System.out.println("All Azure SDK classes available on classpath.");

            System.out.println();
            System.out.println("Pipeline configuration:");
            System.out.println("  Key Vault URL: " + config.getKeyVaultUrl());
            System.out.println("  Container: " + config.getStorageContainerName());
            System.out.println("  Encryption Key: " + config.getEncryptionKeyName());
            System.out.println();
            System.out.println("To run the full pipeline, provide Azure credentials.");
        } catch (Exception e) {
            System.err.println("Pipeline initialization failed: " + e.getMessage());
        }
    }

    /**
     * Runs the full pipeline workflow.
     * Demonstrates: resolve secrets → create/get keys → encrypt → store → retrieve → decrypt.
     *
     * @param keyVaultClient the Key Vault client
     * @param blobClient     the Blob Storage client
     * @param config         the pipeline configuration
     */
    public void runPipeline(KeyVaultClient keyVaultClient, CloudBlobClient blobClient,
                             PipelineConfiguration config) throws PipelineException {

        String vaultUrl = config.getKeyVaultUrl();

        // Wire up components — each uses Track 1 SDK types throughout
        EncryptionKeyProvider keyProvider = new KeyVaultEncryptionKeyProvider(keyVaultClient, vaultUrl);
        AzureResourceResolver<SecretBundle> secretResolver = new KeyVaultSecretResolver(keyVaultClient);
        EncryptedBlobStore blobStore = new EncryptedBlobStore(
                keyProvider, blobClient, config.getStorageContainerName(), vaultUrl);
        ResourceCache<SecretBundle> secretCache =
                new ResourceCache<SecretBundle>(new ObjectMapper(), SecretBundle.class, 300_000L);

        // Step 1: Resolve and cache configuration secrets
        System.out.println("Resolving configuration secrets...");
        SecretBundle dbConnectionSecret = secretResolver.resolve(vaultUrl, "database-connection-string");
        try {
            secretCache.put("database-connection-string", dbConnectionSecret);
            System.out.println("  Cached secret: " + secretResolver.getIdentifier(dbConnectionSecret));
        } catch (com.fasterxml.jackson.core.JsonProcessingException e) {
            System.err.println("  Failed to cache secret: " + e.getMessage());
        }

        // Step 2: Get the encryption key
        System.out.println("Setting up encryption key...");
        KeyBundle encryptionKey = keyProvider.getKey(config.getEncryptionKeyName());
        System.out.println("  Key ID: " + encryptionKey.key().kid());

        // Step 3: Encrypt and upload a document
        String documentContent = "Confidential patient record #12345";
        byte[] documentBytes = documentContent.getBytes(StandardCharsets.UTF_8);
        System.out.println("Encrypting and uploading document...");
        Map<String, String> uploadMetadata = blobStore.uploadEncrypted(
                "medical-records/patient-12345.enc",
                documentBytes,
                config.getEncryptionKeyName(),
                config.getEncryptionKeyVersion());
        System.out.println("  Stored with metadata: " + uploadMetadata);

        // Step 4: List encrypted blobs
        System.out.println("Listing encrypted documents...");
        List<Map<String, String>> blobs = blobStore.listEncryptedBlobs();
        for (Map<String, String> blob : blobs) {
            System.out.println("  " + blob.get("name") + " -> " + blob.get(EncryptedBlobStore.METADATA_KEY_ID));
        }

        // Step 5: Download and decrypt
        System.out.println("Downloading and decrypting document...");
        byte[] decryptedBytes = blobStore.downloadDecrypted("medical-records/patient-12345.enc");
        String decryptedContent = new String(decryptedBytes, StandardCharsets.UTF_8);
        System.out.println("  Decrypted: " + decryptedContent);

        // Step 6: Show secret metadata from the resolver
        Map<String, String> secretMetadata = secretResolver.toMetadata(dbConnectionSecret);
        System.out.println("Secret metadata: " + secretMetadata);

        System.out.println();
        System.out.println("Pipeline completed successfully.");
    }
}
