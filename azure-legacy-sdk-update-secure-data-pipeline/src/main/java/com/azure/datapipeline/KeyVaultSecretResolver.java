package com.azure.datapipeline;

import com.microsoft.azure.keyvault.KeyVaultClient;
import com.microsoft.azure.keyvault.models.SecretBundle;
import com.microsoft.azure.keyvault.models.SecretItem;
import com.microsoft.azure.PagedList;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Resolves Azure Key Vault secrets using the Track 1 SDK.
 * Implements {@link AzureResourceResolver} parameterized on {@link SecretBundle}.
 *
 * <p>Migration challenge: The generic type parameter {@code SecretBundle} flows
 * through the entire resolver interface. Changing to the Track 2 equivalent
 * ({@code KeyVaultSecret}) requires updating all callers that use
 * {@code AzureResourceResolver<SecretBundle>} and all methods that access
 * {@code SecretBundle}-specific properties like {@code value()}, {@code id()},
 * {@code contentType()}, and {@code attributes()}.</p>
 */
public class KeyVaultSecretResolver implements AzureResourceResolver<SecretBundle> {

    private final KeyVaultClient keyVaultClient;

    public KeyVaultSecretResolver(KeyVaultClient keyVaultClient) {
        this.keyVaultClient = keyVaultClient;
    }

    @Override
    public SecretBundle resolve(String vaultUrl, String name) {
        return keyVaultClient.getSecret(vaultUrl, name);
    }

    @Override
    public List<SecretBundle> resolveAll(String vaultUrl) {
        PagedList<SecretItem> items = keyVaultClient.listSecrets(vaultUrl);
        List<SecretBundle> secrets = new ArrayList<SecretBundle>();
        for (SecretItem item : items) {
            String secretName = extractNameFromId(item.id());
            if (secretName != null) {
                secrets.add(keyVaultClient.getSecret(vaultUrl, secretName));
            }
        }
        return secrets;
    }

    @Override
    public String getIdentifier(SecretBundle resource) {
        return resource.id();
    }

    @Override
    public Map<String, String> toMetadata(SecretBundle resource) {
        Map<String, String> metadata = new HashMap<String, String>();
        metadata.put("secret-id", resource.id());
        if (resource.contentType() != null) {
            metadata.put("content-type", resource.contentType());
        }
        if (resource.attributes() != null && resource.attributes().enabled() != null) {
            metadata.put("enabled", resource.attributes().enabled().toString());
        }
        return metadata;
    }

    /**
     * Extracts the secret name from a Key Vault secret identifier URL.
     * Format: https://{vault-name}.vault.azure.net/secrets/{secret-name}/{version}
     */
    private String extractNameFromId(String secretId) {
        if (secretId == null) {
            return null;
        }
        String[] parts = secretId.split("/");
        for (int i = 0; i < parts.length - 1; i++) {
            if ("secrets".equals(parts[i])) {
                return parts[i + 1];
            }
        }
        return null;
    }
}
