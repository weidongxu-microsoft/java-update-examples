package com.azure.datapipeline;

import com.microsoft.azure.keyvault.KeyVaultClient;
import com.microsoft.azure.storage.CloudStorageAccount;
import com.microsoft.azure.storage.blob.CloudBlobClient;
import com.microsoft.rest.credentials.ServiceClientCredentials;

import java.net.URISyntaxException;
import java.security.InvalidKeyException;
import java.util.HashMap;
import java.util.Map;

/**
 * Factory for creating Azure SDK clients. Uses reflection-based validation
 * to verify client class availability at runtime before instantiation.
 *
 * <p>Migration challenge: SDK class names are referenced as string constants.
 * Import-based scanning tools will not detect these references when searching
 * for legacy SDK usage. The fully-qualified class names change between Track 1
 * and Track 2 (e.g., {@code com.microsoft.azure.keyvault.KeyVaultClient} →
 * {@code com.azure.security.keyvault.keys.KeyClient}).</p>
 */
public class AzureClientFactory {

    private static final Map<String, String> AZURE_CLIENT_CLASSES = new HashMap<String, String>();

    static {
        AZURE_CLIENT_CLASSES.put("keyvault",
                "com.microsoft.azure.keyvault.KeyVaultClient");
        AZURE_CLIENT_CLASSES.put("storage-account",
                "com.microsoft.azure.storage.CloudStorageAccount");
        AZURE_CLIENT_CLASSES.put("storage-blob",
                "com.microsoft.azure.storage.blob.CloudBlobClient");
        AZURE_CLIENT_CLASSES.put("storage-container",
                "com.microsoft.azure.storage.blob.CloudBlobContainer");
        AZURE_CLIENT_CLASSES.put("storage-block-blob",
                "com.microsoft.azure.storage.blob.CloudBlockBlob");
        AZURE_CLIENT_CLASSES.put("keyvault-credentials",
                "com.microsoft.azure.keyvault.authentication.KeyVaultCredentials");
    }

    /**
     * Checks whether the Azure SDK class for the given service type is available
     * on the classpath using reflection.
     *
     * @param serviceType the service type key
     * @return true if the SDK class is found
     */
    public boolean isClientAvailable(String serviceType) {
        String className = AZURE_CLIENT_CLASSES.get(serviceType);
        if (className == null) {
            return false;
        }
        try {
            Class.forName(className);
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }

    /**
     * Returns the fully-qualified class name for the given service type.
     *
     * @param serviceType the service type key
     * @return the SDK class name, or null if unknown
     */
    public String getClientClassName(String serviceType) {
        return AZURE_CLIENT_CLASSES.get(serviceType);
    }

    /**
     * Creates a Key Vault client with the given credentials.
     *
     * @param credentials the service client credentials
     * @return a configured {@link KeyVaultClient}
     */
    public KeyVaultClient createKeyVaultClient(ServiceClientCredentials credentials) {
        validateClientAvailable("keyvault");
        return new KeyVaultClient(credentials);
    }

    /**
     * Parses a connection string into a storage account.
     *
     * @param connectionString the Azure Storage connection string
     * @return the parsed {@link CloudStorageAccount}
     */
    public CloudStorageAccount createStorageAccount(String connectionString)
            throws URISyntaxException, InvalidKeyException {
        validateClientAvailable("storage-account");
        return CloudStorageAccount.parse(connectionString);
    }

    /**
     * Creates a blob client from a storage account.
     *
     * @param account the storage account
     * @return a configured {@link CloudBlobClient}
     */
    public CloudBlobClient createBlobClient(CloudStorageAccount account) {
        validateClientAvailable("storage-blob");
        return account.createCloudBlobClient();
    }

    /**
     * Creates a blob client from a connection string (convenience method).
     *
     * @param connectionString the Azure Storage connection string
     * @return a configured {@link CloudBlobClient}
     */
    public CloudBlobClient createBlobClient(String connectionString)
            throws URISyntaxException, InvalidKeyException {
        CloudStorageAccount account = createStorageAccount(connectionString);
        return createBlobClient(account);
    }

    /**
     * Validates all required Azure SDK classes are on the classpath.
     *
     * @throws IllegalStateException if any required classes are missing
     */
    public void validateAllClientsAvailable() {
        StringBuilder missing = new StringBuilder();
        for (Map.Entry<String, String> entry : AZURE_CLIENT_CLASSES.entrySet()) {
            if (!isClientAvailable(entry.getKey())) {
                if (missing.length() > 0) {
                    missing.append(", ");
                }
                missing.append(entry.getValue());
            }
        }
        if (missing.length() > 0) {
            throw new IllegalStateException("Missing Azure SDK classes: " + missing);
        }
    }

    private void validateClientAvailable(String serviceType) {
        if (!isClientAvailable(serviceType)) {
            throw new IllegalStateException(
                    "Azure client class not found: " + AZURE_CLIENT_CLASSES.get(serviceType));
        }
    }
}
