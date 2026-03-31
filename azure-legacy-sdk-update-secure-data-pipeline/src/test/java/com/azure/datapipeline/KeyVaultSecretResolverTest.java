package com.azure.datapipeline;

import com.microsoft.azure.PagedList;
import com.microsoft.azure.keyvault.KeyVaultClient;
import com.microsoft.azure.keyvault.models.SecretBundle;
import com.microsoft.azure.keyvault.models.SecretItem;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.List;
import java.util.Map;

import static org.hamcrest.CoreMatchers.hasItems;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class KeyVaultSecretResolverTest {

    private static final String VAULT_URL = "https://my-vault.vault.azure.net";

    @Mock
    private KeyVaultClient keyVaultClient;

    @Mock
    private SecretBundle secretBundle;

    @Mock
    private SecretBundle secondSecretBundle;

    @Mock
    private PagedList<SecretItem> secretItems;

    @Mock
    private SecretItem secretItem;

    @Mock
    private SecretItem secondSecretItem;

    private KeyVaultSecretResolver resolver;

    @Before
    public void setUp() {
        resolver = new KeyVaultSecretResolver(keyVaultClient);
    }

    @Test
    public void resolveRetrievesSecretByName() {
        when(keyVaultClient.getSecret(VAULT_URL, "database-connection")).thenReturn(secretBundle);

        SecretBundle result = resolver.resolve(VAULT_URL, "database-connection");

        assertSame(secretBundle, result);
        verify(keyVaultClient).getSecret(VAULT_URL, "database-connection");
    }

    @Test
    public void getIdentifierReturnsSecretId() {
        when(secretBundle.id()).thenReturn(
                "https://my-vault.vault.azure.net/secrets/database-connection/abc123");

        String identifier = resolver.getIdentifier(secretBundle);

        assertThat(identifier,
                is("https://my-vault.vault.azure.net/secrets/database-connection/abc123"));
    }

    @Test
    public void toMetadataIncludesSecretId() {
        when(secretBundle.id()).thenReturn(
                "https://my-vault.vault.azure.net/secrets/database-connection/abc123");
        when(secretBundle.contentType()).thenReturn("text/plain");
        when(secretBundle.attributes()).thenReturn(null);

        Map<String, String> metadata = resolver.toMetadata(secretBundle);

        assertThat(metadata.get("secret-id"),
                is("https://my-vault.vault.azure.net/secrets/database-connection/abc123"));
        assertThat(metadata.get("content-type"), is("text/plain"));
    }

    @Test
    public void toMetadataOmitsNullContentType() {
        when(secretBundle.id()).thenReturn("https://my-vault.vault.azure.net/secrets/key/v1");
        when(secretBundle.contentType()).thenReturn(null);
        when(secretBundle.attributes()).thenReturn(null);

        Map<String, String> metadata = resolver.toMetadata(secretBundle);

        assertThat(metadata.containsKey("content-type"), is(false));
    }

    @Test
    public void getIdentifierReturnsNullForNullId() {
        when(secretBundle.id()).thenReturn(null);

        String identifier = resolver.getIdentifier(secretBundle);

        assertThat(identifier, is(nullValue()));
    }
}
