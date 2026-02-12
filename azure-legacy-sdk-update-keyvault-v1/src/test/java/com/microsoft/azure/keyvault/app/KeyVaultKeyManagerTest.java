package com.microsoft.azure.keyvault.app;

import com.microsoft.azure.PagedList;
import com.microsoft.azure.keyvault.KeyVaultClient;
import com.microsoft.azure.keyvault.models.DeletedKeyBundle;
import com.microsoft.azure.keyvault.models.KeyBundle;
import com.microsoft.azure.keyvault.models.KeyItem;
import com.microsoft.azure.keyvault.models.KeyOperationResult;
import com.microsoft.azure.keyvault.models.KeyVerifyResult;
import com.microsoft.azure.keyvault.requests.CreateKeyRequest;
import com.microsoft.azure.keyvault.webkey.JsonWebKeyEncryptionAlgorithm;
import com.microsoft.azure.keyvault.webkey.JsonWebKeySignatureAlgorithm;
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
public class KeyVaultKeyManagerTest {

    private static final String VAULT_URL = "https://test-vault.vault.azure.net";

    @Mock
    private KeyVaultClient keyVaultClient;

    @Mock
    private KeyBundle keyBundle;

    @Mock
    private DeletedKeyBundle deletedKeyBundle;

    @Mock
    private KeyItem keyItem;

    @Mock
    private KeyOperationResult keyOperationResult;

    @Mock
    private KeyVerifyResult keyVerifyResult;

    @Mock
    private PagedList<KeyItem> keyItems;

    private KeyVaultKeyManager manager;

    @Before
    public void setUp() {
        manager = new KeyVaultKeyManager(keyVaultClient, VAULT_URL);
    }

    @Test
    public void createRsaKeyCreatesKeyWithRsaType() {
        when(keyVaultClient.createKey(any(CreateKeyRequest.class))).thenReturn(keyBundle);

        KeyBundle result = manager.createRsaKey("encryption-key");

        assertSame(keyBundle, result);
        ArgumentCaptor<CreateKeyRequest> captor = ArgumentCaptor.forClass(CreateKeyRequest.class);
        verify(keyVaultClient).createKey(captor.capture());
        assertThat(captor.getValue().vaultBaseUrl(), is(VAULT_URL));
        assertThat(captor.getValue().keyName(), is("encryption-key"));
        assertThat(captor.getValue().keyType(), is(JsonWebKeyType.RSA));
    }

    @Test
    public void createEcKeyCreatesKeyWithEcType() {
        when(keyVaultClient.createKey(any(CreateKeyRequest.class))).thenReturn(keyBundle);

        KeyBundle result = manager.createEcKey("signing-key");

        assertSame(keyBundle, result);
        ArgumentCaptor<CreateKeyRequest> captor = ArgumentCaptor.forClass(CreateKeyRequest.class);
        verify(keyVaultClient).createKey(captor.capture());
        assertThat(captor.getValue().keyType(), is(JsonWebKeyType.EC));
    }

    @Test
    public void getKeyRetrievesLatestVersion() {
        when(keyVaultClient.getKey(VAULT_URL, "encryption-key")).thenReturn(keyBundle);

        KeyBundle result = manager.getKey("encryption-key");

        assertSame(keyBundle, result);
        verify(keyVaultClient).getKey(VAULT_URL, "encryption-key");
    }

    @Test
    public void getKeyWithVersionRetrievesSpecificVersion() {
        when(keyVaultClient.getKey(VAULT_URL, "encryption-key", "v1")).thenReturn(keyBundle);

        KeyBundle result = manager.getKey("encryption-key", "v1");

        assertSame(keyBundle, result);
        verify(keyVaultClient).getKey(VAULT_URL, "encryption-key", "v1");
    }

    @Test
    public void listKeysReturnsAllKeys() {
        when(keyVaultClient.listKeys(VAULT_URL)).thenReturn(keyItems);

        PagedList<KeyItem> result = manager.listKeys();

        assertSame(keyItems, result);
        verify(keyVaultClient).listKeys(VAULT_URL);
    }

    @Test
    public void deleteKeyRemovesKey() {
        when(keyVaultClient.deleteKey(VAULT_URL, "encryption-key")).thenReturn(deletedKeyBundle);

        DeletedKeyBundle result = manager.deleteKey("encryption-key");

        assertSame(deletedKeyBundle, result);
        verify(keyVaultClient).deleteKey(VAULT_URL, "encryption-key");
    }

    @Test
    public void encryptDelegatesToClient() {
        byte[] plaintext = "data".getBytes();
        when(keyVaultClient.encrypt(VAULT_URL, "encryption-key", "v1",
                JsonWebKeyEncryptionAlgorithm.RSA_OAEP, plaintext)).thenReturn(keyOperationResult);

        KeyOperationResult result = manager.encrypt("encryption-key", "v1",
                JsonWebKeyEncryptionAlgorithm.RSA_OAEP, plaintext);

        assertSame(keyOperationResult, result);
        verify(keyVaultClient).encrypt(VAULT_URL, "encryption-key", "v1",
                JsonWebKeyEncryptionAlgorithm.RSA_OAEP, plaintext);
    }

    @Test
    public void decryptDelegatesToClient() {
        byte[] ciphertext = "encrypted".getBytes();
        when(keyVaultClient.decrypt(VAULT_URL, "encryption-key", "v1",
                JsonWebKeyEncryptionAlgorithm.RSA_OAEP, ciphertext)).thenReturn(keyOperationResult);

        KeyOperationResult result = manager.decrypt("encryption-key", "v1",
                JsonWebKeyEncryptionAlgorithm.RSA_OAEP, ciphertext);

        assertSame(keyOperationResult, result);
    }

    @Test
    public void wrapKeyDelegatesToClient() {
        byte[] keyToWrap = "symmetric-key".getBytes();
        when(keyVaultClient.wrapKey(VAULT_URL, "encryption-key", "v1",
                JsonWebKeyEncryptionAlgorithm.RSA_OAEP, keyToWrap)).thenReturn(keyOperationResult);

        KeyOperationResult result = manager.wrapKey("encryption-key", "v1",
                JsonWebKeyEncryptionAlgorithm.RSA_OAEP, keyToWrap);

        assertSame(keyOperationResult, result);
    }

    @Test
    public void unwrapKeyDelegatesToClient() {
        byte[] wrappedKey = "wrapped".getBytes();
        when(keyVaultClient.unwrapKey(VAULT_URL, "encryption-key", "v1",
                JsonWebKeyEncryptionAlgorithm.RSA_OAEP, wrappedKey)).thenReturn(keyOperationResult);

        KeyOperationResult result = manager.unwrapKey("encryption-key", "v1",
                JsonWebKeyEncryptionAlgorithm.RSA_OAEP, wrappedKey);

        assertSame(keyOperationResult, result);
    }

    @Test
    public void signDelegatesToClient() {
        byte[] digest = "hash".getBytes();
        when(keyVaultClient.sign(VAULT_URL, "signing-key", "v1",
                JsonWebKeySignatureAlgorithm.RS256, digest)).thenReturn(keyOperationResult);

        KeyOperationResult result = manager.sign("signing-key", "v1",
                JsonWebKeySignatureAlgorithm.RS256, digest);

        assertSame(keyOperationResult, result);
    }

    @Test
    public void verifyDelegatesToClient() {
        byte[] digest = "hash".getBytes();
        byte[] signature = "sig".getBytes();
        when(keyVaultClient.verify(VAULT_URL, "signing-key", "v1",
                JsonWebKeySignatureAlgorithm.RS256, digest, signature)).thenReturn(keyVerifyResult);

        KeyVerifyResult result = manager.verify("signing-key", "v1",
                JsonWebKeySignatureAlgorithm.RS256, digest, signature);

        assertSame(keyVerifyResult, result);
    }
}
