package com.azure.datapipeline;

import com.microsoft.azure.keyvault.models.KeyBundle;
import com.microsoft.azure.keyvault.models.KeyOperationResult;
import com.microsoft.azure.keyvault.webkey.JsonWebKeyEncryptionAlgorithm;
import com.microsoft.azure.storage.StorageException;
import com.microsoft.azure.storage.blob.CloudBlobClient;
import com.microsoft.azure.storage.blob.CloudBlobContainer;
import com.microsoft.azure.storage.blob.CloudBlockBlob;
import com.microsoft.azure.storage.blob.ListBlobItem;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Encrypted blob store that integrates Azure Key Vault for encryption key
 * management with Azure Blob Storage for data persistence.
 *
 * <p>Migration challenges:</p>
 * <ul>
 *   <li>Cross-service data flow: Key Vault crypto operations produce results
 *       that are directly passed to Storage upload operations. Migrating one
 *       service requires migrating the other simultaneously.</li>
 *   <li>Metadata coupling: Key Vault key identifiers and algorithm names are
 *       stored in blob metadata. The format of these identifiers may differ
 *       between SDK versions.</li>
 * </ul>
 */
public class EncryptedBlobStore {

    static final String METADATA_KEY_ID = "x-encryption-key-id";
    static final String METADATA_KEY_VERSION = "x-encryption-key-version";
    static final String METADATA_ALGORITHM = "x-encryption-algorithm";
    static final String METADATA_KEY_VAULT_URL = "x-key-vault-url";
    static final String METADATA_ORIGINAL_SIZE = "x-original-size";

    private final EncryptionKeyProvider keyProvider;
    private final CloudBlobClient blobClient;
    private final String containerName;
    private final String vaultUrl;
    private final PipelineExceptionTranslator exceptionTranslator;

    public EncryptedBlobStore(EncryptionKeyProvider keyProvider, CloudBlobClient blobClient,
                               String containerName, String vaultUrl) {
        this.keyProvider = keyProvider;
        this.blobClient = blobClient;
        this.containerName = containerName;
        this.vaultUrl = vaultUrl;
        this.exceptionTranslator = new PipelineExceptionTranslator();
    }

    /**
     * Uploads data after encrypting it with a Key Vault managed key.
     * Stores the encryption key identifier and algorithm in blob metadata.
     *
     * @param blobName   the blob name
     * @param data       the plaintext data
     * @param keyName    the Key Vault key name
     * @param keyVersion the Key Vault key version
     * @return metadata describing the stored encrypted blob
     */
    public Map<String, String> uploadEncrypted(String blobName, byte[] data,
                                                String keyName, String keyVersion)
            throws PipelineException {
        try {
            // Step 1: Get the encryption key from Key Vault
            KeyBundle keyBundle = keyProvider.getKey(keyName, keyVersion);
            String keyId = keyBundle.key().kid();

            // Step 2: Encrypt the data using Key Vault server-side crypto
            JsonWebKeyEncryptionAlgorithm algorithm = JsonWebKeyEncryptionAlgorithm.RSA_OAEP;
            KeyOperationResult encryptResult = keyProvider.encrypt(
                    keyName, keyVersion, algorithm, data);

            // Step 3: Upload the encrypted data to Blob Storage
            CloudBlobContainer container = blobClient.getContainerReference(containerName);
            container.createIfNotExists();
            CloudBlockBlob blob = container.getBlockBlobReference(blobName);
            byte[] encryptedData = encryptResult.result();
            blob.upload(new ByteArrayInputStream(encryptedData), encryptedData.length);

            // Step 4: Store encryption metadata in blob metadata
            HashMap<String, String> metadata = new HashMap<String, String>();
            metadata.put(METADATA_KEY_ID, keyId);
            metadata.put(METADATA_KEY_VERSION, keyVersion);
            metadata.put(METADATA_ALGORITHM, algorithm.toString());
            metadata.put(METADATA_KEY_VAULT_URL, vaultUrl);
            metadata.put(METADATA_ORIGINAL_SIZE, String.valueOf(data.length));
            blob.setMetadata(metadata);
            blob.uploadMetadata();

            return Collections.unmodifiableMap(metadata);
        } catch (StorageException e) {
            throw exceptionTranslator.translate(e);
        } catch (URISyntaxException e) {
            throw new PipelineException(
                    PipelineException.ErrorSource.STORAGE, "STORAGE_URI_ERROR",
                    -1, e.getMessage(), e);
        } catch (IOException e) {
            throw new PipelineException(
                    PipelineException.ErrorSource.STORAGE, "STORAGE_IO_ERROR",
                    -1, e.getMessage(), e);
        }
    }

    /**
     * Downloads and decrypts a blob, reading the encryption key details
     * from blob metadata.
     *
     * @param blobName the blob name
     * @return the decrypted data
     */
    public byte[] downloadDecrypted(String blobName) throws PipelineException {
        try {
            // Step 1: Get the blob reference and download its metadata
            CloudBlobContainer container = blobClient.getContainerReference(containerName);
            CloudBlockBlob blob = container.getBlockBlobReference(blobName);
            blob.downloadAttributes();

            HashMap<String, String> metadata = blob.getMetadata();
            String keyName = extractKeyNameFromId(metadata.get(METADATA_KEY_ID));
            String keyVersion = metadata.get(METADATA_KEY_VERSION);
            String algorithmName = metadata.get(METADATA_ALGORITHM);

            // Step 2: Download the encrypted data
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            blob.download(outputStream);
            byte[] encryptedData = outputStream.toByteArray();

            // Step 3: Decrypt using Key Vault with the algorithm from metadata
            JsonWebKeyEncryptionAlgorithm algorithm =
                    new JsonWebKeyEncryptionAlgorithm(algorithmName);
            KeyOperationResult decryptResult = keyProvider.decrypt(
                    keyName, keyVersion, algorithm, encryptedData);

            return decryptResult.result();
        } catch (StorageException e) {
            throw exceptionTranslator.translate(e);
        } catch (URISyntaxException e) {
            throw new PipelineException(
                    PipelineException.ErrorSource.STORAGE, "STORAGE_URI_ERROR",
                    -1, e.getMessage(), e);
        }
    }

    /**
     * Lists all encrypted blobs in the container with their encryption metadata.
     *
     * @return list of blob metadata maps
     */
    public List<Map<String, String>> listEncryptedBlobs() throws PipelineException {
        try {
            CloudBlobContainer container = blobClient.getContainerReference(containerName);
            List<Map<String, String>> results = new ArrayList<Map<String, String>>();

            for (ListBlobItem item : container.listBlobs()) {
                if (item instanceof CloudBlockBlob) {
                    CloudBlockBlob blob = (CloudBlockBlob) item;
                    blob.downloadAttributes();
                    Map<String, String> blobInfo = new HashMap<String, String>(blob.getMetadata());
                    blobInfo.put("name", blob.getName());
                    blobInfo.put("uri", blob.getUri().toString());
                    results.add(blobInfo);
                }
            }

            return results;
        } catch (StorageException e) {
            throw exceptionTranslator.translate(e);
        } catch (URISyntaxException e) {
            throw new PipelineException(
                    PipelineException.ErrorSource.STORAGE, "STORAGE_URI_ERROR",
                    -1, e.getMessage(), e);
        }
    }

    /**
     * Deletes an encrypted blob.
     *
     * @param blobName the blob name
     * @return true if the blob was deleted
     */
    public boolean deleteEncryptedBlob(String blobName) throws PipelineException {
        try {
            CloudBlobContainer container = blobClient.getContainerReference(containerName);
            CloudBlockBlob blob = container.getBlockBlobReference(blobName);
            return blob.deleteIfExists();
        } catch (StorageException e) {
            throw exceptionTranslator.translate(e);
        } catch (URISyntaxException e) {
            throw new PipelineException(
                    PipelineException.ErrorSource.STORAGE, "STORAGE_URI_ERROR",
                    -1, e.getMessage(), e);
        }
    }

    /**
     * Extracts the key name from a Key Vault key identifier URL.
     * Format: https://{vault-name}.vault.azure.net/keys/{key-name}/{version}
     */
    private String extractKeyNameFromId(String keyId) {
        if (keyId == null) {
            return null;
        }
        String[] parts = keyId.split("/");
        for (int i = 0; i < parts.length - 1; i++) {
            if ("keys".equals(parts[i])) {
                return parts[i + 1];
            }
        }
        return null;
    }
}
