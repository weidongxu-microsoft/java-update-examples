package com.microsoft.azure.storage.advanced;

import com.microsoft.azure.storage.CloudStorageAccount;
import com.microsoft.azure.storage.SharedAccessAccountPolicy;
import com.microsoft.azure.storage.StorageException;
import com.microsoft.azure.storage.blob.CloudBlobContainer;
import com.microsoft.azure.storage.blob.SharedAccessBlobPermissions;

import org.junit.Before;
import org.junit.Test;

import java.net.URISyntaxException;
import java.security.InvalidKeyException;
import java.time.Duration;
import java.util.EnumSet;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class SasTokenGeneratorTest {

    private static final String CONNECTION_STRING =
            "DefaultEndpointsProtocol=https;AccountName=devaccount;AccountKey=dGVzdGtleQ==;EndpointSuffix=core.windows.net";

    private SasTokenGenerator generator;
    private CloudStorageAccount account;

    @Before
    public void setUp() throws URISyntaxException, InvalidKeyException {
        account = CloudStorageAccount.parse(CONNECTION_STRING);
        generator = new SasTokenGenerator(account);
    }

    @Test
    public void testGenerateAccountSas() throws InvalidKeyException, StorageException {
        String sas = generator.generateAccountSas(Duration.ofHours(1));
        assertNotNull(sas);
        assertFalse(sas.isEmpty());
        assertTrue(sas.contains("sig="));
        assertTrue(sas.contains("se="));
        assertTrue(sas.contains("sp="));
    }

    @Test
    public void testGenerateFullAccessAccountSas() throws InvalidKeyException, StorageException {
        String sas = generator.generateFullAccessAccountSas(Duration.ofHours(2));
        assertNotNull(sas);
        assertFalse(sas.isEmpty());
        assertTrue(sas.contains("sig="));
    }

    @Test
    public void testGenerateContainerSas()
            throws URISyntaxException, StorageException, InvalidKeyException {
        CloudBlobContainer container = account.createCloudBlobClient()
                .getContainerReference("testcontainer");

        EnumSet<SharedAccessBlobPermissions> permissions = EnumSet.of(
                SharedAccessBlobPermissions.READ,
                SharedAccessBlobPermissions.WRITE);

        String sas = generator.generateContainerSas(container, Duration.ofHours(1), permissions);
        assertNotNull(sas);
        assertFalse(sas.isEmpty());
        assertTrue(sas.contains("sig="));
    }

    @Test
    public void testGenerateReadOnlyContainerSas()
            throws URISyntaxException, StorageException, InvalidKeyException {
        CloudBlobContainer container = account.createCloudBlobClient()
                .getContainerReference("readonly");

        String sas = generator.generateReadOnlyContainerSas(container, Duration.ofMinutes(30));
        assertNotNull(sas);
        assertTrue(sas.contains("sig="));
    }

    @Test
    public void testGetStorageAccountReturnsSameInstance() {
        CloudStorageAccount returned = generator.getStorageAccount();
        assertNotNull(returned);
        assertTrue(returned == account);
    }

    @Test
    public void testSasTokenContainsExpectedComponents() throws InvalidKeyException, StorageException {
        String sas = generator.generateAccountSas(Duration.ofMinutes(15));
        assertTrue("SAS should contain signature", sas.contains("sig="));
        assertTrue("SAS should contain expiry", sas.contains("se="));
        assertTrue("SAS should contain start time", sas.contains("st="));
        assertTrue("SAS should contain permissions", sas.contains("sp="));
        assertTrue("SAS should contain services", sas.contains("ss="));
        assertTrue("SAS should contain resource types", sas.contains("srt="));
        assertTrue("SAS should contain protocol", sas.contains("spr="));
    }
}
