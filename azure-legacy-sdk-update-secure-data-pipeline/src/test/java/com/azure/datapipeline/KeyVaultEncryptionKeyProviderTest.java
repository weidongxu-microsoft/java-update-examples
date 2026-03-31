package com.azure.datapipeline;

import com.microsoft.azure.keyvault.KeyVaultClient;
import com.microsoft.azure.keyvault.models.KeyBundle;
import com.microsoft.azure.keyvault.models.KeyOperationResult;
import com.microsoft.azure.keyvault.requests.CreateKeyRequest;
import com.microsoft.azure.keyvault.webkey.JsonWebKeyEncryptionAlgorithm;
import com.microsoft.azure.keyvault.webkey.JsonWebKeyType;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertSame;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class KeyVaultEncryptionKeyProviderTest {

    private static final String VAULT_URL = "https://my-vault.vault.azure.net";

    @Mock
    private KeyVaultClient keyVaultClient;

    @Mock
    private KeyBundle keyBundle;

    @Mock
    private KeyOperationResult keyOperationResult;

    private KeyVaultEncryptionKeyProvider provider;

    @Before
    public void setUp() {
        provider = new KeyVaultEncryptionKeyProvider(keyVaultClient, VAULT_URL);
    }

    @Test
    public void createKeyCreatesRsaKeyInVault() {
        when(keyVaultClient.createKey(any(CreateKeyRequest.class))).thenReturn(keyBundle);

        KeyBundle result = provider.createKey("data-encryption-key");

        assertSame(keyBundle, result);
        ArgumentCaptor<CreateKeyRequest> captor = ArgumentCaptor.forClass(CreateKeyRequest.class);
        verify(keyVaultClient).createKey(captor.capture());
        assertThat(captor.getValue().vaultBaseUrl(), is(VAULT_URL));
        assertThat(captor.getValue().keyName(), is("data-encryption-key"));
        assertThat(captor.getValue().keyType(), is(JsonWebKeyType.RSA));
    }

    @Test
    public void getKeyRetrievesLatestVersion() {
        when(keyVaultClient.getKey(VAULT_URL, "data-encryption-key")).thenReturn(keyBundle);

        KeyBundle result = provider.getKey("data-encryption-key");

        assertSame(keyBundle, result);
        verify(keyVaultClient).getKey(VAULT_URL, "data-encryption-key");
    }

    @Test
    public void getKeyWithVersionRetrievesSpecificVersion() {
        when(keyVaultClient.getKey(VAULT_URL, "data-encryption-key", "v1")).thenReturn(keyBundle);

        KeyBundle result = provider.getKey("data-encryption-key", "v1");

        assertSame(keyBundle, result);
        verify(keyVaultClient).getKey(VAULT_URL, "data-encryption-key", "v1");
    }

    @Test
    public void encryptDelegatesToKeyVaultClient() {
        byte[] plaintext = "confidential data".getBytes();
        when(keyVaultClient.encrypt(VAULT_URL, "data-encryption-key", "v1",
                JsonWebKeyEncryptionAlgorithm.RSA_OAEP, plaintext)).thenReturn(keyOperationResult);

        KeyOperationResult result = provider.encrypt("data-encryption-key", "v1",
                JsonWebKeyEncryptionAlgorithm.RSA_OAEP, plaintext);

        assertSame(keyOperationResult, result);
        verify(keyVaultClient).encrypt(VAULT_URL, "data-encryption-key", "v1",
                JsonWebKeyEncryptionAlgorithm.RSA_OAEP, plaintext);
    }

    @Test
    public void decryptDelegatesToKeyVaultClient() {
        byte[] ciphertext = "encrypted-data".getBytes();
        when(keyVaultClient.decrypt(VAULT_URL, "data-encryption-key", "v1",
                JsonWebKeyEncryptionAlgorithm.RSA_OAEP, ciphertext)).thenReturn(keyOperationResult);

        KeyOperationResult result = provider.decrypt("data-encryption-key", "v1",
                JsonWebKeyEncryptionAlgorithm.RSA_OAEP, ciphertext);

        assertSame(keyOperationResult, result);
        verify(keyVaultClient).decrypt(VAULT_URL, "data-encryption-key", "v1",
                JsonWebKeyEncryptionAlgorithm.RSA_OAEP, ciphertext);
    }
}
