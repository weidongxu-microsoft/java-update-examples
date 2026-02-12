package com.microsoft.azure.keyvault.app;

import com.microsoft.azure.PagedList;
import com.microsoft.azure.keyvault.KeyVaultClient;
import com.microsoft.azure.keyvault.models.CertificateBundle;
import com.microsoft.azure.keyvault.models.CertificateItem;
import com.microsoft.azure.keyvault.models.CertificateOperation;
import com.microsoft.azure.keyvault.models.DeletedCertificateBundle;
import com.microsoft.azure.keyvault.requests.CreateCertificateRequest;
import com.microsoft.azure.keyvault.requests.ImportCertificateRequest;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class KeyVaultCertificateManagerTest {

    private static final String VAULT_URL = "https://test-vault.vault.azure.net";

    @Mock
    private KeyVaultClient keyVaultClient;

    @Mock
    private CertificateBundle certificateBundle;

    @Mock
    private DeletedCertificateBundle deletedCertificateBundle;

    @Mock
    private CertificateOperation certificateOperation;

    @Mock
    private CertificateItem certificateItem;

    @Mock
    private PagedList<CertificateItem> certificateItems;

    private KeyVaultCertificateManager manager;

    @Before
    public void setUp() {
        manager = new KeyVaultCertificateManager(keyVaultClient, VAULT_URL);
    }

    @Test
    public void createSelfSignedCertificateStartsCreation() {
        when(keyVaultClient.createCertificate(any(CreateCertificateRequest.class))).thenReturn(certificateOperation);

        CertificateOperation result = manager.createSelfSignedCertificate("auth-certificate", "CN=app.contoso.com");

        assertSame(certificateOperation, result);
        ArgumentCaptor<CreateCertificateRequest> captor = ArgumentCaptor.forClass(CreateCertificateRequest.class);
        verify(keyVaultClient).createCertificate(captor.capture());
        assertThat(captor.getValue().vaultBaseUrl(), is(VAULT_URL));
        assertThat(captor.getValue().certificateName(), is("auth-certificate"));
        assertNotNull(captor.getValue().certificatePolicy());
        assertThat(captor.getValue().certificatePolicy().issuerParameters().name(), is("Self"));
        assertThat(captor.getValue().certificatePolicy().x509CertificateProperties().subject(), is("CN=app.contoso.com"));
    }

    @Test
    public void importCertificateImportsContent() {
        when(keyVaultClient.importCertificate(any(ImportCertificateRequest.class))).thenReturn(certificateBundle);

        CertificateBundle result = manager.importCertificate("imported-certificate", "base64content");

        assertSame(certificateBundle, result);
        ArgumentCaptor<ImportCertificateRequest> captor = ArgumentCaptor.forClass(ImportCertificateRequest.class);
        verify(keyVaultClient).importCertificate(captor.capture());
        assertThat(captor.getValue().vaultBaseUrl(), is(VAULT_URL));
        assertThat(captor.getValue().certificateName(), is("imported-certificate"));
        assertThat(captor.getValue().base64EncodedCertificate(), is("base64content"));
    }

    @Test
    public void getCertificateRetrievesLatestVersion() {
        when(keyVaultClient.getCertificate(VAULT_URL, "auth-certificate")).thenReturn(certificateBundle);

        CertificateBundle result = manager.getCertificate("auth-certificate");

        assertSame(certificateBundle, result);
        verify(keyVaultClient).getCertificate(VAULT_URL, "auth-certificate");
    }

    @Test
    public void getCertificateWithVersionRetrievesSpecificVersion() {
        when(keyVaultClient.getCertificate(VAULT_URL, "auth-certificate", "v1")).thenReturn(certificateBundle);

        CertificateBundle result = manager.getCertificate("auth-certificate", "v1");

        assertSame(certificateBundle, result);
        verify(keyVaultClient).getCertificate(VAULT_URL, "auth-certificate", "v1");
    }

    @Test
    public void listCertificatesReturnsAll() {
        when(keyVaultClient.listCertificates(VAULT_URL)).thenReturn(certificateItems);

        PagedList<CertificateItem> result = manager.listCertificates();

        assertSame(certificateItems, result);
        verify(keyVaultClient).listCertificates(VAULT_URL);
    }

    @Test
    public void deleteCertificateRemovesCertificate() {
        when(keyVaultClient.deleteCertificate(VAULT_URL, "auth-certificate")).thenReturn(deletedCertificateBundle);

        DeletedCertificateBundle result = manager.deleteCertificate("auth-certificate");

        assertSame(deletedCertificateBundle, result);
        verify(keyVaultClient).deleteCertificate(VAULT_URL, "auth-certificate");
    }

    @Test
    public void getCertificateOperationReturnsStatus() {
        when(keyVaultClient.getCertificateOperation(VAULT_URL, "auth-certificate")).thenReturn(certificateOperation);

        CertificateOperation result = manager.getCertificateOperation("auth-certificate");

        assertSame(certificateOperation, result);
        verify(keyVaultClient).getCertificateOperation(VAULT_URL, "auth-certificate");
    }
}
