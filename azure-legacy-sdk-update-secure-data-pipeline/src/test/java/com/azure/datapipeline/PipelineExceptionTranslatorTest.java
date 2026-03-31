package com.azure.datapipeline;

import com.microsoft.azure.keyvault.models.KeyVaultErrorException;
import com.microsoft.azure.storage.StorageException;
import com.microsoft.azure.storage.StorageExtendedErrorInformation;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class PipelineExceptionTranslatorTest {

    private PipelineExceptionTranslator translator;

    @Before
    public void setUp() {
        translator = new PipelineExceptionTranslator();
    }

    @Test
    public void translateStorageExceptionMapsErrorCode() {
        StorageException storageEx = new StorageException(
                "BlobNotFound", "The specified blob does not exist.", 404, null, null);

        PipelineException result = translator.translate(storageEx);

        assertThat(result.getErrorSource(), is(PipelineException.ErrorSource.STORAGE));
        assertThat(result.getErrorCode(), is("STORAGE_BlobNotFound"));
        assertThat(result.getHttpStatusCode(), is(404));
    }

    @Test
    public void translateStorageExceptionUsesExtendedErrorInfoWhenAvailable() {
        StorageExtendedErrorInformation extendedInfo = mock(StorageExtendedErrorInformation.class);
        when(extendedInfo.getErrorMessage()).thenReturn("Detailed error message from storage service");

        StorageException storageEx = new StorageException(
                "ContainerNotFound", "Container not found", 404, extendedInfo, null);

        PipelineException result = translator.translate(storageEx);

        assertThat(result.getMessage(), is("Detailed error message from storage service"));
    }

    @Test
    public void translateStorageExceptionHandlesNullErrorCode() {
        StorageException storageEx = new StorageException(
                null, "Unknown storage error", 500, null, null);

        PipelineException result = translator.translate(storageEx);

        assertThat(result.getErrorCode(), is("STORAGE_UNKNOWN"));
    }

    @Test
    @SuppressWarnings("unchecked")
    public void translateKeyVaultExceptionMapsErrorCode() {
        // Mock the Track 1 KeyVaultErrorException chain:
        // e.body() -> KeyVaultError -> error() -> Error -> code(), message()
        KeyVaultErrorException kvEx = mock(KeyVaultErrorException.class);
        com.microsoft.azure.keyvault.models.KeyVaultError kvError =
                mock(com.microsoft.azure.keyvault.models.KeyVaultError.class);
        com.microsoft.azure.keyvault.models.Error error =
                mock(com.microsoft.azure.keyvault.models.Error.class);
        retrofit2.Response response = mock(retrofit2.Response.class);

        when(kvEx.body()).thenReturn(kvError);
        when(kvError.error()).thenReturn(error);
        when(error.code()).thenReturn("SecretNotFound");
        when(error.message()).thenReturn("Secret not found in vault");
        when(kvEx.response()).thenReturn(response);
        when(response.code()).thenReturn(404);

        PipelineException result = translator.translate(kvEx);

        assertThat(result.getErrorSource(), is(PipelineException.ErrorSource.KEY_VAULT));
        assertThat(result.getErrorCode(), is("KEYVAULT_SecretNotFound"));
        assertThat(result.getHttpStatusCode(), is(404));
        assertThat(result.getMessage(), is("Secret not found in vault"));
    }

    @Test
    public void translateKeyVaultExceptionHandlesNullBody() {
        KeyVaultErrorException kvEx = mock(KeyVaultErrorException.class);
        when(kvEx.body()).thenReturn(null);
        when(kvEx.response()).thenReturn(null);
        when(kvEx.getMessage()).thenReturn("Connection refused");

        PipelineException result = translator.translate(kvEx);

        assertThat(result.getErrorSource(), is(PipelineException.ErrorSource.KEY_VAULT));
        assertThat(result.getErrorCode(), is("KEYVAULT_UNKNOWN"));
        assertThat(result.getHttpStatusCode(), is(-1));
    }

    @Test
    public void translateUnknownExceptionReturnsUnknownSource() {
        RuntimeException unknownEx = new RuntimeException("Something went wrong");

        PipelineException result = translator.translate(unknownEx);

        assertThat(result.getErrorSource(), is(PipelineException.ErrorSource.UNKNOWN));
        assertThat(result.getErrorCode(), is("UNKNOWN_ERROR"));
        assertThat(result.getHttpStatusCode(), is(-1));
    }
}
