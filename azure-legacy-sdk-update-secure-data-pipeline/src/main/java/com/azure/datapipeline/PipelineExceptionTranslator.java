package com.azure.datapipeline;

import com.microsoft.azure.keyvault.models.KeyVaultErrorException;
import com.microsoft.azure.storage.StorageException;

/**
 * Translates Azure SDK exceptions into unified {@link PipelineException} instances.
 *
 * <p>Migration challenge: Track 1 and Track 2 SDK exceptions have fundamentally
 * different class hierarchies, method names, and error models:</p>
 * <ul>
 *   <li>{@code StorageException.getErrorCode()} vs Track 2 {@code BlobStorageException.getErrorCode()}</li>
 *   <li>{@code StorageException.getHttpStatusCode()} vs Track 2 {@code BlobStorageException.getStatusCode()}</li>
 *   <li>{@code KeyVaultErrorException.body().error().code()} vs Track 2: completely different error model</li>
 *   <li>{@code KeyVaultErrorException.response().code()} vs Track 2 {@code HttpResponseException.getResponse().getStatusCode()}</li>
 * </ul>
 */
public class PipelineExceptionTranslator {

    /**
     * Translates an Azure SDK exception into a {@link PipelineException}.
     *
     * @param e the original exception
     * @return a unified pipeline exception
     */
    public PipelineException translate(Exception e) {
        if (e instanceof StorageException) {
            return translateStorageException((StorageException) e);
        }
        if (e instanceof KeyVaultErrorException) {
            return translateKeyVaultException((KeyVaultErrorException) e);
        }
        return new PipelineException(
                PipelineException.ErrorSource.UNKNOWN,
                "UNKNOWN_ERROR",
                -1,
                e.getMessage(),
                e
        );
    }

    private PipelineException translateStorageException(StorageException e) {
        String errorCode = "STORAGE_" + (e.getErrorCode() != null ? e.getErrorCode() : "UNKNOWN");
        String message = e.getMessage();
        if (e.getExtendedErrorInformation() != null
                && e.getExtendedErrorInformation().getErrorMessage() != null) {
            message = e.getExtendedErrorInformation().getErrorMessage();
        }
        return new PipelineException(
                PipelineException.ErrorSource.STORAGE,
                errorCode,
                e.getHttpStatusCode(),
                message,
                e
        );
    }

    private PipelineException translateKeyVaultException(KeyVaultErrorException e) {
        String code = "KEYVAULT_UNKNOWN";
        String message = e.getMessage();
        int httpStatus = -1;

        if (e.body() != null && e.body().error() != null) {
            if (e.body().error().code() != null) {
                code = "KEYVAULT_" + e.body().error().code();
            }
            if (e.body().error().message() != null) {
                message = e.body().error().message();
            }
        }

        if (e.response() != null) {
            httpStatus = e.response().code();
        }

        return new PipelineException(
                PipelineException.ErrorSource.KEY_VAULT,
                code,
                httpStatus,
                message,
                e
        );
    }
}
