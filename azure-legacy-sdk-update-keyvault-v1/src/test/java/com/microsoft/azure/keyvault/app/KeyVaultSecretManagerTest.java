package com.microsoft.azure.keyvault.app;

import com.microsoft.azure.PagedList;
import com.microsoft.azure.keyvault.KeyVaultClient;
import com.microsoft.azure.keyvault.models.DeletedSecretBundle;
import com.microsoft.azure.keyvault.models.SecretBundle;
import com.microsoft.azure.keyvault.models.SecretItem;
import com.microsoft.azure.keyvault.requests.SetSecretRequest;
import com.microsoft.azure.keyvault.requests.UpdateSecretRequest;
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
public class KeyVaultSecretManagerTest {

    private static final String VAULT_URL = "https://test-vault.vault.azure.net";

    @Mock
    private KeyVaultClient keyVaultClient;

    @Mock
    private SecretBundle secretBundle;

    @Mock
    private DeletedSecretBundle deletedSecretBundle;

    @Mock
    private PagedList<SecretItem> secretItems;

    private KeyVaultSecretManager manager;

    @Before
    public void setUp() {
        manager = new KeyVaultSecretManager(keyVaultClient, VAULT_URL);
    }

    @Test
    public void setSecretCreatesSecretWithNameAndValue() {
        when(keyVaultClient.setSecret(any(SetSecretRequest.class))).thenReturn(secretBundle);

        SecretBundle result = manager.setSecret("db-password", "s3cret");

        assertSame(secretBundle, result);
        ArgumentCaptor<SetSecretRequest> captor = ArgumentCaptor.forClass(SetSecretRequest.class);
        verify(keyVaultClient).setSecret(captor.capture());
        assertThat(captor.getValue().vaultBaseUrl(), is(VAULT_URL));
        assertThat(captor.getValue().secretName(), is("db-password"));
    }

    @Test
    public void getSecretRetrievesLatestVersion() {
        when(keyVaultClient.getSecret(VAULT_URL, "db-password")).thenReturn(secretBundle);

        SecretBundle result = manager.getSecret("db-password");

        assertSame(secretBundle, result);
        verify(keyVaultClient).getSecret(VAULT_URL, "db-password");
    }

    @Test
    public void getSecretWithVersionRetrievesSpecificVersion() {
        when(keyVaultClient.getSecret(VAULT_URL, "db-password", "v1")).thenReturn(secretBundle);

        SecretBundle result = manager.getSecret("db-password", "v1");

        assertSame(secretBundle, result);
        verify(keyVaultClient).getSecret(VAULT_URL, "db-password", "v1");
    }

    @Test
    public void updateSecretContentTypeSetsNewContentType() {
        when(keyVaultClient.updateSecret(any(UpdateSecretRequest.class))).thenReturn(secretBundle);

        SecretBundle result = manager.updateSecretContentType("db-password", "v1", "text/plain");

        assertSame(secretBundle, result);
        ArgumentCaptor<UpdateSecretRequest> captor = ArgumentCaptor.forClass(UpdateSecretRequest.class);
        verify(keyVaultClient).updateSecret(captor.capture());
        assertThat(captor.getValue().vaultBaseUrl(), is(VAULT_URL));
        assertThat(captor.getValue().secretName(), is("db-password"));
        assertThat(captor.getValue().contentType(), is("text/plain"));
    }

    @Test
    public void listSecretsReturnsAllSecrets() {
        when(keyVaultClient.listSecrets(VAULT_URL)).thenReturn(secretItems);

        PagedList<SecretItem> result = manager.listSecrets();

        assertSame(secretItems, result);
        verify(keyVaultClient).listSecrets(VAULT_URL);
    }

    @Test
    public void deleteSecretRemovesSecret() {
        when(keyVaultClient.deleteSecret(VAULT_URL, "db-password")).thenReturn(deletedSecretBundle);

        DeletedSecretBundle result = manager.deleteSecret("db-password");

        assertSame(deletedSecretBundle, result);
        verify(keyVaultClient).deleteSecret(VAULT_URL, "db-password");
    }
}
