package com.azure.datapipeline;

/**
 * Exception thrown by the secure data pipeline. Wraps underlying Azure SDK
 * exceptions with a unified error model including the originating service,
 * error code, and HTTP status.
 */
public class PipelineException extends Exception {

    /**
     * Identifies which Azure service originated the error.
     */
    public enum ErrorSource {
        STORAGE,
        KEY_VAULT,
        CONFIGURATION,
        UNKNOWN
    }

    private final ErrorSource errorSource;
    private final String errorCode;
    private final int httpStatusCode;

    public PipelineException(ErrorSource errorSource, String errorCode,
                              int httpStatusCode, String message) {
        super(message);
        this.errorSource = errorSource;
        this.errorCode = errorCode;
        this.httpStatusCode = httpStatusCode;
    }

    public PipelineException(ErrorSource errorSource, String errorCode,
                              int httpStatusCode, String message, Throwable cause) {
        super(message, cause);
        this.errorSource = errorSource;
        this.errorCode = errorCode;
        this.httpStatusCode = httpStatusCode;
    }

    public ErrorSource getErrorSource() {
        return errorSource;
    }

    public String getErrorCode() {
        return errorCode;
    }

    public int getHttpStatusCode() {
        return httpStatusCode;
    }

    @Override
    public String toString() {
        return String.format("PipelineException{source=%s, code='%s', httpStatus=%d, message='%s'}",
                errorSource, errorCode, httpStatusCode, getMessage());
    }
}
