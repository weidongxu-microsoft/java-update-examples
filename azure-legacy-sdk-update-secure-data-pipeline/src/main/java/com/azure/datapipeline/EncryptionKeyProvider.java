package com.azure.datapipeline;

import com.microsoft.azure.keyvault.models.KeyBundle;
import com.microsoft.azure.keyvault.models.KeyOperationResult;
import com.microsoft.azure.keyvault.webkey.JsonWebKeyEncryptionAlgorithm;

/**
 * Provides encryption key management and cryptographic operations.
 * Implementations delegate to Azure Key Vault for key storage and
 * server-side crypto operations.
 *
 * <p>Migration challenge: Return types {@link KeyBundle} and {@link KeyOperationResult}
 * are Track 1 types. Track 2 uses {@code KeyVaultKey} and separate {@code EncryptResult}/
 * {@code DecryptResult} types. Changing this interface cascades to all implementers
 * and callers.</p>
 */
public interface EncryptionKeyProvider {

    /**
     * Creates a new RSA encryption key.
     *
     * @param keyName the key name
     * @return the created {@link KeyBundle}
     */
    KeyBundle createKey(String keyName);

    /**
     * Retrieves the latest version of a key.
     *
     * @param keyName the key name
     * @return the {@link KeyBundle}
     */
    KeyBundle getKey(String keyName);

    /**
     * Retrieves a specific version of a key.
     *
     * @param keyName    the key name
     * @param keyVersion the key version
     * @return the {@link KeyBundle}
     */
    KeyBundle getKey(String keyName, String keyVersion);

    /**
     * Encrypts data using the specified key and algorithm.
     *
     * @param keyName    the key name
     * @param keyVersion the key version
     * @param algorithm  the encryption algorithm
     * @param plaintext  the data to encrypt
     * @return the {@link KeyOperationResult} containing the ciphertext
     */
    KeyOperationResult encrypt(String keyName, String keyVersion,
                                JsonWebKeyEncryptionAlgorithm algorithm, byte[] plaintext);

    /**
     * Decrypts data using the specified key and algorithm.
     *
     * @param keyName    the key name
     * @param keyVersion the key version
     * @param algorithm  the encryption algorithm
     * @param ciphertext the data to decrypt
     * @return the {@link KeyOperationResult} containing the plaintext
     */
    KeyOperationResult decrypt(String keyName, String keyVersion,
                                JsonWebKeyEncryptionAlgorithm algorithm, byte[] ciphertext);
}
