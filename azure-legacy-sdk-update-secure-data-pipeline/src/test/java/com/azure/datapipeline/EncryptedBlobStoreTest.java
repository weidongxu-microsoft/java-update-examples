package com.azure.datapipeline;

import com.microsoft.azure.keyvault.models.KeyBundle;
import com.microsoft.azure.keyvault.models.KeyOperationResult;
import com.microsoft.azure.keyvault.webkey.JsonWebKey;
import com.microsoft.azure.keyvault.webkey.JsonWebKeyEncryptionAlgorithm;
import com.microsoft.azure.storage.StorageException;
import com.microsoft.azure.storage.blob.CloudBlobClient;
import com.microsoft.azure.storage.blob.CloudBlobContainer;
import com.microsoft.azure.storage.blob.CloudBlockBlob;
import com.microsoft.azure.storage.blob.ListBlobItem;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.io.ByteArrayInputStream;
import java.io.OutputStream;
import java.net.URI;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class EncryptedBlobStoreTest {

    private static final String VAULT_URL = "https://my-vault.vault.azure.net";
    private static final String CONTAINER_NAME = "encrypted-documents";
    private static final String KEY_ID = "https://my-vault.vault.azure.net/keys/data-encryption-key/v1";

    @Mock
    private EncryptionKeyProvider keyProvider;

    @Mock
    private CloudBlobClient blobClient;

    @Mock
    private CloudBlobContainer container;

    @Mock
    private CloudBlockBlob blob;

    @Mock
    private KeyBundle keyBundle;

    @Mock
    private JsonWebKey jsonWebKey;

    @Mock
    private KeyOperationResult encryptResult;

    @Mock
    private KeyOperationResult decryptResult;

    private EncryptedBlobStore store;

    @Before
    public void setUp() throws Exception {
        store = new EncryptedBlobStore(keyProvider, blobClient, CONTAINER_NAME, VAULT_URL);
        when(blobClient.getContainerReference(CONTAINER_NAME)).thenReturn(container);
        when(container.getBlockBlobReference(anyString())).thenReturn(blob);
    }

    @Test
    public void uploadEncryptedEncryptsAndStoresWithMetadata() throws Exception {
        byte[] plaintext = "confidential document".getBytes();
        byte[] encrypted = "encrypted-payload".getBytes();

        when(keyProvider.getKey("data-encryption-key", "v1")).thenReturn(keyBundle);
        when(keyBundle.key()).thenReturn(jsonWebKey);
        when(jsonWebKey.kid()).thenReturn(KEY_ID);
        when(keyProvider.encrypt(eq("data-encryption-key"), eq("v1"),
                any(JsonWebKeyEncryptionAlgorithm.class), eq(plaintext)))
                .thenReturn(encryptResult);
        when(encryptResult.result()).thenReturn(encrypted);

        Map<String, String> metadata = store.uploadEncrypted(
                "reports/quarterly.enc", plaintext, "data-encryption-key", "v1");

        // Verify encrypted data was uploaded
        verify(blob).upload(any(ByteArrayInputStream.class), eq((long) encrypted.length));

        // Verify metadata was stored
        verify(blob).setMetadata(any(HashMap.class));
        verify(blob).uploadMetadata();

        // Verify metadata contents
        assertThat(metadata.get(EncryptedBlobStore.METADATA_KEY_ID), is(KEY_ID));
        assertThat(metadata.get(EncryptedBlobStore.METADATA_KEY_VERSION), is("v1"));
        assertThat(metadata.get(EncryptedBlobStore.METADATA_ALGORITHM), is("RSA-OAEP"));
        assertThat(metadata.get(EncryptedBlobStore.METADATA_KEY_VAULT_URL), is(VAULT_URL));
        assertThat(metadata.get(EncryptedBlobStore.METADATA_ORIGINAL_SIZE),
                is(String.valueOf(plaintext.length)));
    }

    @Test
    public void uploadEncryptedCreatesContainerIfNotExists() throws Exception {
        byte[] plaintext = "data".getBytes();
        byte[] encrypted = "enc".getBytes();

        when(keyProvider.getKey("data-encryption-key", "v1")).thenReturn(keyBundle);
        when(keyBundle.key()).thenReturn(jsonWebKey);
        when(jsonWebKey.kid()).thenReturn(KEY_ID);
        when(keyProvider.encrypt(anyString(), anyString(),
                any(JsonWebKeyEncryptionAlgorithm.class), any(byte[].class)))
                .thenReturn(encryptResult);
        when(encryptResult.result()).thenReturn(encrypted);

        store.uploadEncrypted("doc.enc", plaintext, "data-encryption-key", "v1");

        verify(container).createIfNotExists();
    }

    @Test
    public void downloadDecryptedReadsMetadataAndDecrypts() throws Exception {
        byte[] encrypted = "encrypted-payload".getBytes();
        byte[] decrypted = "decrypted content".getBytes();

        HashMap<String, String> metadata = new HashMap<String, String>();
        metadata.put(EncryptedBlobStore.METADATA_KEY_ID, KEY_ID);
        metadata.put(EncryptedBlobStore.METADATA_KEY_VERSION, "v1");
        metadata.put(EncryptedBlobStore.METADATA_ALGORITHM, "RSA-OAEP");

        when(blob.getMetadata()).thenReturn(metadata);
        doAnswer(invocation -> {
            OutputStream os = invocation.getArgument(0);
            os.write(encrypted);
            return null;
        }).when(blob).download(any(OutputStream.class));

        when(keyProvider.decrypt(eq("data-encryption-key"), eq("v1"),
                any(JsonWebKeyEncryptionAlgorithm.class), eq(encrypted)))
                .thenReturn(decryptResult);
        when(decryptResult.result()).thenReturn(decrypted);

        byte[] result = store.downloadDecrypted("reports/quarterly.enc");

        assertThat(result, is(decrypted));
        verify(blob).downloadAttributes();
    }

    @Test
    public void downloadDecryptedParsesKeyNameFromKeyId() throws Exception {
        byte[] encrypted = "data".getBytes();
        byte[] decrypted = "plain".getBytes();

        HashMap<String, String> metadata = new HashMap<String, String>();
        metadata.put(EncryptedBlobStore.METADATA_KEY_ID,
                "https://vault.vault.azure.net/keys/custom-key-name/version2");
        metadata.put(EncryptedBlobStore.METADATA_KEY_VERSION, "version2");
        metadata.put(EncryptedBlobStore.METADATA_ALGORITHM, "RSA-OAEP");

        when(blob.getMetadata()).thenReturn(metadata);
        doAnswer(invocation -> {
            OutputStream os = invocation.getArgument(0);
            os.write(encrypted);
            return null;
        }).when(blob).download(any(OutputStream.class));

        when(keyProvider.decrypt(eq("custom-key-name"), eq("version2"),
                any(JsonWebKeyEncryptionAlgorithm.class), eq(encrypted)))
                .thenReturn(decryptResult);
        when(decryptResult.result()).thenReturn(decrypted);

        byte[] result = store.downloadDecrypted("doc.enc");

        assertThat(result, is(decrypted));
        verify(keyProvider).decrypt(eq("custom-key-name"), eq("version2"),
                any(JsonWebKeyEncryptionAlgorithm.class), eq(encrypted));
    }

    @Test
    public void deleteEncryptedBlobDelegatesDelete() throws Exception {
        when(blob.deleteIfExists()).thenReturn(true);

        boolean deleted = store.deleteEncryptedBlob("reports/quarterly.enc");

        assertThat(deleted, is(true));
        verify(blob).deleteIfExists();
    }

    @Test(expected = PipelineException.class)
    public void uploadEncryptedTranslatesStorageException() throws Exception {
        when(keyProvider.getKey("data-encryption-key", "v1")).thenReturn(keyBundle);
        when(keyBundle.key()).thenReturn(jsonWebKey);
        when(jsonWebKey.kid()).thenReturn(KEY_ID);
        when(keyProvider.encrypt(anyString(), anyString(),
                any(JsonWebKeyEncryptionAlgorithm.class), any(byte[].class)))
                .thenReturn(encryptResult);
        when(container.createIfNotExists())
                .thenThrow(new StorageException("ContainerAlreadyExists",
                        "Container already exists", 409, null, null));

        store.uploadEncrypted("doc.enc", "data".getBytes(), "data-encryption-key", "v1");
    }

    @Test
    public void listEncryptedBlobsReturnsBlobMetadata() throws Exception {
        CloudBlockBlob listBlob = blob;
        HashMap<String, String> metadata = new HashMap<String, String>();
        metadata.put(EncryptedBlobStore.METADATA_KEY_ID, KEY_ID);

        when(container.listBlobs()).thenReturn(Arrays.<ListBlobItem>asList(listBlob));
        when(listBlob.getMetadata()).thenReturn(metadata);
        when(listBlob.getName()).thenReturn("reports/quarterly.enc");
        when(listBlob.getUri()).thenReturn(
                URI.create("https://storage.blob.core.windows.net/encrypted-documents/reports/quarterly.enc"));

        List<Map<String, String>> results = store.listEncryptedBlobs();

        assertThat(results.size(), is(1));
        assertThat(results.get(0).get("name"), is("reports/quarterly.enc"));
        assertThat(results.get(0).get(EncryptedBlobStore.METADATA_KEY_ID), is(KEY_ID));
    }
}
