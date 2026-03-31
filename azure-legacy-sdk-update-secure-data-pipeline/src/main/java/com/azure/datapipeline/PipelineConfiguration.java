package com.azure.datapipeline;

/**
 * Configuration for the secure data pipeline, loaded from environment variables.
 */
public class PipelineConfiguration {

    private final String keyVaultUrl;
    private final String storageConnectionString;
    private final String storageContainerName;
    private final String encryptionKeyName;
    private final String encryptionKeyVersion;

    /**
     * Creates configuration from environment variables with defaults.
     */
    public PipelineConfiguration() {
        this(
            getEnvOrDefault("KEY_VAULT_URL", "https://my-vault.vault.azure.net"),
            getEnvOrDefault("STORAGE_CONNECTION_STRING",
                "DefaultEndpointsProtocol=https;AccountName=mystorageaccount;"
                + "AccountKey=c3RvcmFnZWFjY291bnRrZXk=;EndpointSuffix=core.windows.net"),
            getEnvOrDefault("STORAGE_CONTAINER_NAME", "encrypted-documents"),
            getEnvOrDefault("ENCRYPTION_KEY_NAME", "document-encryption-key"),
            getEnvOrDefault("ENCRYPTION_KEY_VERSION", "")
        );
    }

    /**
     * Creates configuration with explicit values.
     *
     * @param keyVaultUrl             the Key Vault base URL
     * @param storageConnectionString the Storage connection string
     * @param storageContainerName    the blob container name
     * @param encryptionKeyName       the Key Vault encryption key name
     * @param encryptionKeyVersion    the Key Vault encryption key version (empty for latest)
     */
    public PipelineConfiguration(String keyVaultUrl, String storageConnectionString,
                                  String storageContainerName, String encryptionKeyName,
                                  String encryptionKeyVersion) {
        this.keyVaultUrl = keyVaultUrl;
        this.storageConnectionString = storageConnectionString;
        this.storageContainerName = storageContainerName;
        this.encryptionKeyName = encryptionKeyName;
        this.encryptionKeyVersion = encryptionKeyVersion;
    }

    /**
     * Validates that all required configuration values are present.
     *
     * @return true if configuration is valid
     */
    public boolean isValid() {
        return isNotEmpty(keyVaultUrl)
            && isNotEmpty(storageConnectionString)
            && isNotEmpty(storageContainerName)
            && isNotEmpty(encryptionKeyName);
    }

    public String getKeyVaultUrl() {
        return keyVaultUrl;
    }

    public String getStorageConnectionString() {
        return storageConnectionString;
    }

    public String getStorageContainerName() {
        return storageContainerName;
    }

    public String getEncryptionKeyName() {
        return encryptionKeyName;
    }

    public String getEncryptionKeyVersion() {
        return encryptionKeyVersion;
    }

    private static String getEnvOrDefault(String name, String defaultValue) {
        String value = System.getenv(name);
        return (value != null && !value.isEmpty()) ? value : defaultValue;
    }

    private static boolean isNotEmpty(String value) {
        return value != null && !value.isEmpty();
    }
}
