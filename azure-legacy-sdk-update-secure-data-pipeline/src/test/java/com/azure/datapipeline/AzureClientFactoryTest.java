package com.azure.datapipeline;

import org.junit.Before;
import org.junit.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;

public class AzureClientFactoryTest {

    private AzureClientFactory factory;

    @Before
    public void setUp() {
        factory = new AzureClientFactory();
    }

    @Test
    public void isClientAvailableReturnsTrueForKeyVault() {
        assertThat(factory.isClientAvailable("keyvault"), is(true));
    }

    @Test
    public void isClientAvailableReturnsTrueForStorageAccount() {
        assertThat(factory.isClientAvailable("storage-account"), is(true));
    }

    @Test
    public void isClientAvailableReturnsTrueForStorageBlob() {
        assertThat(factory.isClientAvailable("storage-blob"), is(true));
    }

    @Test
    public void isClientAvailableReturnsTrueForStorageContainer() {
        assertThat(factory.isClientAvailable("storage-container"), is(true));
    }

    @Test
    public void isClientAvailableReturnsTrueForStorageBlockBlob() {
        assertThat(factory.isClientAvailable("storage-block-blob"), is(true));
    }

    @Test
    public void isClientAvailableReturnsFalseForUnknownService() {
        assertThat(factory.isClientAvailable("unknown-service"), is(false));
    }

    @Test
    public void isClientAvailableReturnsFalseForNull() {
        assertThat(factory.isClientAvailable(null), is(false));
    }

    @Test
    public void getClientClassNameReturnsKeyVaultClassName() {
        assertThat(factory.getClientClassName("keyvault"),
                is("com.microsoft.azure.keyvault.KeyVaultClient"));
    }

    @Test
    public void getClientClassNameReturnsStorageAccountClassName() {
        assertThat(factory.getClientClassName("storage-account"),
                is("com.microsoft.azure.storage.CloudStorageAccount"));
    }

    @Test
    public void getClientClassNameReturnsStorageBlobClassName() {
        assertThat(factory.getClientClassName("storage-blob"),
                is("com.microsoft.azure.storage.blob.CloudBlobClient"));
    }

    @Test
    public void getClientClassNameReturnsNullForUnknown() {
        assertThat(factory.getClientClassName("nonexistent"), is(nullValue()));
    }

    @Test
    public void validateAllClientsAvailableSucceedsWhenAllPresent() {
        // Should not throw since all SDK classes are on the classpath
        factory.validateAllClientsAvailable();
    }

    @Test
    public void createBlobClientFromConnectionStringReturnsClient() throws Exception {
        // Uses a syntactically valid connection string for unit testing
        String connectionString = "DefaultEndpointsProtocol=https;AccountName=testaccount;"
                + "AccountKey=dGVzdGtleXRlc3RrZXl0ZXN0a2V5dGVzdGtleQ==;"
                + "EndpointSuffix=core.windows.net";
        assertThat(factory.createBlobClient(connectionString), is(notNullValue()));
    }
}
