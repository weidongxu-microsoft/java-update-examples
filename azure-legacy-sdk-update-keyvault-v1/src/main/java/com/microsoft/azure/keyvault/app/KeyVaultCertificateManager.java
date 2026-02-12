package com.microsoft.azure.keyvault.app;

import com.microsoft.azure.keyvault.KeyVaultClient;
import com.microsoft.azure.keyvault.models.CertificateBundle;
import com.microsoft.azure.keyvault.models.CertificateOperation;
import com.microsoft.azure.keyvault.models.CertificatePolicy;
import com.microsoft.azure.keyvault.models.IssuerParameters;
import com.microsoft.azure.keyvault.models.SecretProperties;
import com.microsoft.azure.keyvault.models.X509CertificateProperties;
import com.microsoft.azure.keyvault.requests.CreateCertificateRequest;
import com.microsoft.azure.keyvault.models.DeletedCertificateBundle;
import com.microsoft.azure.keyvault.requests.ImportCertificateRequest;
import com.microsoft.azure.PagedList;

/**
 * Manages Azure Key Vault certificates using the Key Vault SDK v1.
 *
 * <p>This class wraps {@link KeyVaultClient} and provides convenience methods
 * for creating, importing, retrieving, listing, and deleting certificates.</p>
 */
public class KeyVaultCertificateManager {

    private final KeyVaultClient keyVaultClient;
    private final String vaultUrl;

    /**
     * Creates a new certificate manager.
     *
     * @param keyVaultClient the Key Vault client
     * @param vaultUrl       the vault base URL, e.g. {@code https://myvault.vault.azure.net}
     */
    public KeyVaultCertificateManager(KeyVaultClient keyVaultClient, String vaultUrl) {
        this.keyVaultClient = keyVaultClient;
        this.vaultUrl = vaultUrl;
    }

    /**
     * Creates a self-signed certificate with the given name and subject.
     *
     * @param certificateName the certificate name
     * @param subject         the X.509 subject, e.g. {@code "CN=myapp.contoso.com"}
     * @return the {@link CertificateOperation} representing the creation status
     */
    public CertificateOperation createSelfSignedCertificate(String certificateName, String subject) {
        CertificatePolicy policy = new CertificatePolicy()
                .withIssuerParameters(new IssuerParameters().withName("Self"))
                .withX509CertificateProperties(new X509CertificateProperties().withSubject(subject))
                .withSecretProperties(new SecretProperties().withContentType("application/x-pkcs12"));

        CreateCertificateRequest request = new CreateCertificateRequest.Builder(vaultUrl, certificateName)
                .withPolicy(policy)
                .build();
        return keyVaultClient.createCertificate(request);
    }

    /**
     * Imports a PKCS#12 certificate into the vault.
     *
     * @param certificateName      the certificate name
     * @param base64EncodedContent the base64-encoded PKCS#12 content
     * @return the imported {@link CertificateBundle}
     */
    public CertificateBundle importCertificate(String certificateName, String base64EncodedContent) {
        ImportCertificateRequest request = new ImportCertificateRequest.Builder(vaultUrl, certificateName, base64EncodedContent)
                .build();
        return keyVaultClient.importCertificate(request);
    }

    /**
     * Retrieves the latest version of a certificate.
     *
     * @param certificateName the certificate name
     * @return the {@link CertificateBundle}
     */
    public CertificateBundle getCertificate(String certificateName) {
        return keyVaultClient.getCertificate(vaultUrl, certificateName);
    }

    /**
     * Retrieves a specific version of a certificate.
     *
     * @param certificateName    the certificate name
     * @param certificateVersion the certificate version
     * @return the {@link CertificateBundle} for the specified version
     */
    public CertificateBundle getCertificate(String certificateName, String certificateVersion) {
        return keyVaultClient.getCertificate(vaultUrl, certificateName, certificateVersion);
    }

    /**
     * Lists all certificates in the vault.
     *
     * @return a list of certificate items
     */
    public PagedList<com.microsoft.azure.keyvault.models.CertificateItem> listCertificates() {
        return keyVaultClient.listCertificates(vaultUrl);
    }

    /**
     * Deletes a certificate from the vault.
     *
     * @param certificateName the certificate name
     * @return the deleted {@link DeletedCertificateBundle}
     */
    public DeletedCertificateBundle deleteCertificate(String certificateName) {
        return keyVaultClient.deleteCertificate(vaultUrl, certificateName);
    }

    /**
     * Retrieves the status of a certificate creation operation.
     *
     * @param certificateName the certificate name
     * @return the {@link CertificateOperation}
     */
    public CertificateOperation getCertificateOperation(String certificateName) {
        return keyVaultClient.getCertificateOperation(vaultUrl, certificateName);
    }
}
