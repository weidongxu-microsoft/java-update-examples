package com.microsoft.azure.keyvault.app;

import com.microsoft.azure.keyvault.KeyVaultClient;
import com.microsoft.azure.keyvault.models.SecretBundle;
import com.microsoft.azure.keyvault.requests.SetSecretRequest;
import com.microsoft.azure.keyvault.models.DeletedSecretBundle;
import com.microsoft.azure.keyvault.requests.UpdateSecretRequest;
import com.microsoft.azure.PagedList;

/**
 * Manages Azure Key Vault secrets using the Key Vault SDK v1.
 *
 * <p>This class wraps {@link KeyVaultClient} and provides convenience methods
 * for creating, retrieving, updating, listing, and deleting secrets.</p>
 */
public class KeyVaultSecretManager {

    private final KeyVaultClient keyVaultClient;
    private final String vaultUrl;

    /**
     * Creates a new secret manager.
     *
     * @param keyVaultClient the Key Vault client
     * @param vaultUrl       the vault base URL, e.g. {@code https://myvault.vault.azure.net}
     */
    public KeyVaultSecretManager(KeyVaultClient keyVaultClient, String vaultUrl) {
        this.keyVaultClient = keyVaultClient;
        this.vaultUrl = vaultUrl;
    }

    /**
     * Creates or updates a secret in the vault.
     *
     * @param secretName  the secret name
     * @param secretValue the secret value
     * @return the created {@link SecretBundle}
     */
    public SecretBundle setSecret(String secretName, String secretValue) {
        SetSecretRequest request = new SetSecretRequest.Builder(vaultUrl, secretName, secretValue).build();
        return keyVaultClient.setSecret(request);
    }

    /**
     * Retrieves the latest version of a secret.
     *
     * @param secretName the secret name
     * @return the {@link SecretBundle} containing the secret value
     */
    public SecretBundle getSecret(String secretName) {
        return keyVaultClient.getSecret(vaultUrl, secretName);
    }

    /**
     * Retrieves a specific version of a secret.
     *
     * @param secretName    the secret name
     * @param secretVersion the secret version
     * @return the {@link SecretBundle} for the specified version
     */
    public SecretBundle getSecret(String secretName, String secretVersion) {
        return keyVaultClient.getSecret(vaultUrl, secretName, secretVersion);
    }

    /**
     * Updates the content type of a secret.
     *
     * @param secretName  the secret name
     * @param secretVersion  the secret version
     * @param contentType the new content type
     * @return the updated {@link SecretBundle}
     */
    public SecretBundle updateSecretContentType(String secretName, String secretVersion, String contentType) {
        UpdateSecretRequest request = new UpdateSecretRequest.Builder(vaultUrl, secretName)
                .withVersion(secretVersion)
                .withContentType(contentType)
                .build();
        return keyVaultClient.updateSecret(request);
    }

    /**
     * Lists all secrets in the vault. Returns a list of {@link SecretBundle}
     * containing secret identifiers (no values).
     *
     * @return a list of secret items
     */
    public PagedList<com.microsoft.azure.keyvault.models.SecretItem> listSecrets() {
        return keyVaultClient.listSecrets(vaultUrl);
    }

    /**
     * Deletes a secret from the vault.
     *
     * @param secretName the secret name
     * @return the deleted {@link DeletedSecretBundle}
     */
    public DeletedSecretBundle deleteSecret(String secretName) {
        return keyVaultClient.deleteSecret(vaultUrl, secretName);
    }
}
