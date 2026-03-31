package com.azure.datapipeline;

import com.microsoft.azure.keyvault.KeyVaultClient;
import com.microsoft.azure.keyvault.models.KeyBundle;
import com.microsoft.azure.keyvault.models.KeyOperationResult;
import com.microsoft.azure.keyvault.requests.CreateKeyRequest;
import com.microsoft.azure.keyvault.webkey.JsonWebKeyEncryptionAlgorithm;
import com.microsoft.azure.keyvault.webkey.JsonWebKeyType;

/**
 * {@link EncryptionKeyProvider} backed by Azure Key Vault using the Track 1 SDK.
 *
 * <p>Uses {@link KeyVaultClient} directly for key management and cryptographic
 * operations. The Track 1 client is monolithic — a single client handles keys,
 * secrets, and certificates. Track 2 splits these into three separate clients.</p>
 */
public class KeyVaultEncryptionKeyProvider implements EncryptionKeyProvider {

    private final KeyVaultClient keyVaultClient;
    private final String vaultUrl;

    public KeyVaultEncryptionKeyProvider(KeyVaultClient keyVaultClient, String vaultUrl) {
        this.keyVaultClient = keyVaultClient;
        this.vaultUrl = vaultUrl;
    }

    @Override
    public KeyBundle createKey(String keyName) {
        CreateKeyRequest request = new CreateKeyRequest.Builder(vaultUrl, keyName, JsonWebKeyType.RSA).build();
        return keyVaultClient.createKey(request);
    }

    @Override
    public KeyBundle getKey(String keyName) {
        return keyVaultClient.getKey(vaultUrl, keyName);
    }

    @Override
    public KeyBundle getKey(String keyName, String keyVersion) {
        return keyVaultClient.getKey(vaultUrl, keyName, keyVersion);
    }

    @Override
    public KeyOperationResult encrypt(String keyName, String keyVersion,
                                       JsonWebKeyEncryptionAlgorithm algorithm, byte[] plaintext) {
        return keyVaultClient.encrypt(vaultUrl, keyName, keyVersion, algorithm, plaintext);
    }

    @Override
    public KeyOperationResult decrypt(String keyName, String keyVersion,
                                       JsonWebKeyEncryptionAlgorithm algorithm, byte[] ciphertext) {
        return keyVaultClient.decrypt(vaultUrl, keyName, keyVersion, algorithm, ciphertext);
    }
}
