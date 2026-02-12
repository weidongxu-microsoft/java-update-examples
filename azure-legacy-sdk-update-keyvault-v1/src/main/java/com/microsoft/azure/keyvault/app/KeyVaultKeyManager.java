package com.microsoft.azure.keyvault.app;

import com.microsoft.azure.keyvault.KeyVaultClient;
import com.microsoft.azure.keyvault.models.KeyBundle;
import com.microsoft.azure.keyvault.models.KeyOperationResult;
import com.microsoft.azure.keyvault.requests.CreateKeyRequest;
import com.microsoft.azure.keyvault.webkey.JsonWebKeyEncryptionAlgorithm;
import com.microsoft.azure.keyvault.webkey.JsonWebKeySignatureAlgorithm;
import com.microsoft.azure.keyvault.models.DeletedKeyBundle;
import com.microsoft.azure.keyvault.webkey.JsonWebKeyType;
import com.microsoft.azure.PagedList;

/**
 * Manages Azure Key Vault keys using the Key Vault SDK v1.
 *
 * <p>This class wraps {@link KeyVaultClient} and provides convenience methods
 * for creating, retrieving, listing, deleting keys, and performing
 * cryptographic operations (encrypt/decrypt, wrap/unwrap, sign/verify).</p>
 */
public class KeyVaultKeyManager {

    private final KeyVaultClient keyVaultClient;
    private final String vaultUrl;

    /**
     * Creates a new key manager.
     *
     * @param keyVaultClient the Key Vault client
     * @param vaultUrl       the vault base URL, e.g. {@code https://myvault.vault.azure.net}
     */
    public KeyVaultKeyManager(KeyVaultClient keyVaultClient, String vaultUrl) {
        this.keyVaultClient = keyVaultClient;
        this.vaultUrl = vaultUrl;
    }

    /**
     * Creates an RSA key in the vault.
     *
     * @param keyName the key name
     * @return the created {@link KeyBundle}
     */
    public KeyBundle createRsaKey(String keyName) {
        CreateKeyRequest request = new CreateKeyRequest.Builder(vaultUrl, keyName, JsonWebKeyType.RSA).build();
        return keyVaultClient.createKey(request);
    }

    /**
     * Creates an EC (Elliptic Curve) key in the vault.
     *
     * @param keyName the key name
     * @return the created {@link KeyBundle}
     */
    public KeyBundle createEcKey(String keyName) {
        CreateKeyRequest request = new CreateKeyRequest.Builder(vaultUrl, keyName, JsonWebKeyType.EC).build();
        return keyVaultClient.createKey(request);
    }

    /**
     * Retrieves the latest version of a key.
     *
     * @param keyName the key name
     * @return the {@link KeyBundle}
     */
    public KeyBundle getKey(String keyName) {
        return keyVaultClient.getKey(vaultUrl, keyName);
    }

    /**
     * Retrieves a specific version of a key.
     *
     * @param keyName    the key name
     * @param keyVersion the key version
     * @return the {@link KeyBundle} for the specified version
     */
    public KeyBundle getKey(String keyName, String keyVersion) {
        return keyVaultClient.getKey(vaultUrl, keyName, keyVersion);
    }

    /**
     * Lists all keys in the vault.
     *
     * @return a list of key items
     */
    public PagedList<com.microsoft.azure.keyvault.models.KeyItem> listKeys() {
        return keyVaultClient.listKeys(vaultUrl);
    }

    /**
     * Deletes a key from the vault.
     *
     * @param keyName the key name
     * @return the deleted {@link DeletedKeyBundle}
     */
    public DeletedKeyBundle deleteKey(String keyName) {
        return keyVaultClient.deleteKey(vaultUrl, keyName);
    }

    /**
     * Encrypts data using the specified key and algorithm.
     *
     * @param keyName    the key name
     * @param keyVersion the key version
     * @param algorithm  the encryption algorithm (e.g. {@link JsonWebKeyEncryptionAlgorithm#RSA_OAEP})
     * @param plaintext  the data to encrypt
     * @return the {@link KeyOperationResult} containing the ciphertext
     */
    public KeyOperationResult encrypt(String keyName, String keyVersion,
                                      JsonWebKeyEncryptionAlgorithm algorithm, byte[] plaintext) {
        return keyVaultClient.encrypt(vaultUrl, keyName, keyVersion, algorithm, plaintext);
    }

    /**
     * Decrypts data using the specified key and algorithm.
     *
     * @param keyName    the key name
     * @param keyVersion the key version
     * @param algorithm  the encryption algorithm
     * @param ciphertext the data to decrypt
     * @return the {@link KeyOperationResult} containing the plaintext
     */
    public KeyOperationResult decrypt(String keyName, String keyVersion,
                                      JsonWebKeyEncryptionAlgorithm algorithm, byte[] ciphertext) {
        return keyVaultClient.decrypt(vaultUrl, keyName, keyVersion, algorithm, ciphertext);
    }

    /**
     * Wraps a symmetric key using the specified key.
     *
     * @param keyName    the key name
     * @param keyVersion the key version
     * @param algorithm  the wrap algorithm
     * @param keyToWrap  the symmetric key bytes to wrap
     * @return the {@link KeyOperationResult} containing the wrapped key
     */
    public KeyOperationResult wrapKey(String keyName, String keyVersion,
                                      JsonWebKeyEncryptionAlgorithm algorithm, byte[] keyToWrap) {
        return keyVaultClient.wrapKey(vaultUrl, keyName, keyVersion, algorithm, keyToWrap);
    }

    /**
     * Unwraps a previously wrapped symmetric key.
     *
     * @param keyName    the key name
     * @param keyVersion the key version
     * @param algorithm  the wrap algorithm
     * @param wrappedKey the wrapped key bytes
     * @return the {@link KeyOperationResult} containing the unwrapped key
     */
    public KeyOperationResult unwrapKey(String keyName, String keyVersion,
                                        JsonWebKeyEncryptionAlgorithm algorithm, byte[] wrappedKey) {
        return keyVaultClient.unwrapKey(vaultUrl, keyName, keyVersion, algorithm, wrappedKey);
    }

    /**
     * Signs a digest using the specified key.
     *
     * @param keyName    the key name
     * @param keyVersion the key version
     * @param algorithm  the signing algorithm (e.g. {@link JsonWebKeySignatureAlgorithm#RS256})
     * @param digest     the digest to sign
     * @return the {@link KeyOperationResult} containing the signature
     */
    public KeyOperationResult sign(String keyName, String keyVersion,
                                   JsonWebKeySignatureAlgorithm algorithm, byte[] digest) {
        return keyVaultClient.sign(vaultUrl, keyName, keyVersion, algorithm, digest);
    }

    /**
     * Verifies a signature using the specified key.
     *
     * @param keyName    the key name
     * @param keyVersion the key version
     * @param algorithm  the signing algorithm
     * @param digest     the digest that was signed
     * @param signature  the signature to verify
     * @return the verification result
     */
    public com.microsoft.azure.keyvault.models.KeyVerifyResult verify(String keyName, String keyVersion,
                                                                       JsonWebKeySignatureAlgorithm algorithm,
                                                                       byte[] digest, byte[] signature) {
        return keyVaultClient.verify(vaultUrl, keyName, keyVersion, algorithm, digest, signature);
    }
}
