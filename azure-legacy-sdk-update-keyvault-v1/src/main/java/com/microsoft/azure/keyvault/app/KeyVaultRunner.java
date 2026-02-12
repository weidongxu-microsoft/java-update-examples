package com.microsoft.azure.keyvault.app;

import com.microsoft.azure.keyvault.KeyVaultClient;
import com.microsoft.azure.keyvault.authentication.KeyVaultCredentials;
import com.microsoft.azure.keyvault.models.CertificateBundle;
import com.microsoft.azure.keyvault.models.CertificateOperation;
import com.microsoft.azure.keyvault.models.DeletedKeyBundle;
import com.microsoft.azure.keyvault.models.KeyBundle;
import com.microsoft.azure.keyvault.models.SecretBundle;

/**
 * Demonstrates Key Vault v1 SDK usage for keys, secrets and certificates.
 *
 * <p>Uses {@link KeyVaultClient} with {@link KeyVaultCredentials} for authentication
 * and interacts with secrets, keys, and certificates through the unified client.</p>
 */
public class KeyVaultRunner {

    /**
     * Entry point.  Requires environment variables {@code AZURE_CLIENT_ID},
     * {@code AZURE_CLIENT_KEY} and {@code KEY_VAULT_URL}.
     */
    public static void main(String[] args) {
        String clientId = System.getenv("AZURE_CLIENT_ID");
        String clientKey = System.getenv("AZURE_CLIENT_KEY");
        String vaultUrl = System.getenv("KEY_VAULT_URL");

        if (clientId == null || clientKey == null || vaultUrl == null) {
            System.err.println("Please set AZURE_CLIENT_ID, AZURE_CLIENT_KEY and KEY_VAULT_URL environment variables.");
            return;
        }

        KeyVaultCredentials credentials = new ClientSecretCredentials(clientId, clientKey);
        KeyVaultClient keyVaultClient = new KeyVaultClient(credentials);

        runSecretOperations(keyVaultClient, vaultUrl);
        runKeyOperations(keyVaultClient, vaultUrl);
        runCertificateOperations(keyVaultClient, vaultUrl);
    }

    private static void runSecretOperations(KeyVaultClient client, String vaultUrl) {
        System.out.println("=== Secret Operations ===");
        KeyVaultSecretManager secretManager = new KeyVaultSecretManager(client, vaultUrl);

        // Create a secret
        SecretBundle secret = secretManager.setSecret("app-connection-string", "Server=db.contoso.com;Database=appdb");
        System.out.println("Created secret: " + secret.secretIdentifier().name());

        // Retrieve the secret
        SecretBundle retrieved = secretManager.getSecret("app-connection-string");
        System.out.println("Retrieved secret value: " + retrieved.value());

        // Update content type
        SecretBundle updated = secretManager.updateSecretContentType(
                "app-connection-string", secret.secretIdentifier().version(), "text/plain");
        System.out.println("Updated secret content type: " + updated.contentType());

        // List secrets
        System.out.println("Listing secrets:");
        secretManager.listSecrets().forEach(item -> System.out.println("  - " + item.id()));

        // Delete secret
        secretManager.deleteSecret("app-connection-string");
        System.out.println("Deleted secret.");
    }

    private static void runKeyOperations(KeyVaultClient client, String vaultUrl) {
        System.out.println("\n=== Key Operations ===");
        KeyVaultKeyManager keyManager = new KeyVaultKeyManager(client, vaultUrl);

        // Create an RSA key
        KeyBundle rsaKey = keyManager.createRsaKey("app-encryption-key");
        System.out.println("Created RSA key: " + rsaKey.keyIdentifier().name());

        // Create an EC key
        KeyBundle ecKey = keyManager.createEcKey("app-signing-key");
        System.out.println("Created EC key: " + ecKey.keyIdentifier().name());

        // Retrieve a key
        KeyBundle retrieved = keyManager.getKey("app-encryption-key");
        System.out.println("Retrieved key type: " + retrieved.key().kty());

        // List keys
        System.out.println("Listing keys:");
        keyManager.listKeys().forEach(item -> System.out.println("  - " + item.kid()));

        // Delete keys
        keyManager.deleteKey("app-encryption-key");
        keyManager.deleteKey("app-signing-key");
        System.out.println("Deleted keys.");
    }

    private static void runCertificateOperations(KeyVaultClient client, String vaultUrl) {
        System.out.println("\n=== Certificate Operations ===");
        KeyVaultCertificateManager certManager = new KeyVaultCertificateManager(client, vaultUrl);

        // Create a self-signed certificate
        CertificateOperation operation = certManager.createSelfSignedCertificate(
                "app-auth-certificate", "CN=app.contoso.com");
        System.out.println("Certificate creation started, status: " + operation.status());

        // Poll until completed
        String status = operation.status();
        while ("inProgress".equalsIgnoreCase(status)) {
            try {
                Thread.sleep(2000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return;
            }
            CertificateOperation polled = certManager.getCertificateOperation("app-auth-certificate");
            status = polled.status();
            System.out.println("Certificate operation status: " + status);
        }

        // Retrieve the certificate
        CertificateBundle cert = certManager.getCertificate("app-auth-certificate");
        System.out.println("Retrieved certificate: " + cert.certificateIdentifier().name());

        // List certificates
        System.out.println("Listing certificates:");
        certManager.listCertificates().forEach(item -> System.out.println("  - " + item.id()));

        // Delete the certificate
        certManager.deleteCertificate("app-auth-certificate");
        System.out.println("Deleted certificate.");
    }
}
