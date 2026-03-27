package com.example.blobmanagerv2.service;

import com.azure.core.credential.AccessToken;
import com.azure.core.credential.TokenCredential;
import com.azure.core.credential.TokenRequestContext;
import com.azure.identity.DefaultAzureCredentialBuilder;
import com.azure.v2.storage.blob.AzureBlobStorageBuilder;
import com.azure.v2.storage.blob.BlobClient;
import com.azure.v2.storage.blob.BlockBlobClient;
import com.azure.v2.storage.blob.ContainerClient;
import com.azure.v2.storage.blob.StorageServiceClient;
import com.azure.v2.storage.blob.models.BlobContainerItem;
import com.azure.v2.storage.blob.models.BlobHttpHeaders;
import com.azure.v2.storage.blob.models.BlobItemInternal;
import com.azure.v2.storage.blob.models.BlobItemPropertiesInternal;
import com.azure.v2.storage.blob.models.ListBlobsFlatSegmentResponse;
import io.clientcore.core.credentials.oauth.OAuthTokenCredential;
import io.clientcore.core.credentials.oauth.OAuthTokenRequestContext;
import io.clientcore.core.http.models.HttpHeader;
import io.clientcore.core.http.models.HttpHeaderName;
import io.clientcore.core.http.models.HttpRequest;
import io.clientcore.core.http.models.HttpResponseException;
import io.clientcore.core.http.models.Response;
import io.clientcore.core.http.pipeline.HttpPipelineNextPolicy;
import io.clientcore.core.http.pipeline.HttpPipelinePolicy;
import io.clientcore.core.http.pipeline.OAuthBearerTokenAuthenticationPolicy;
import io.clientcore.core.models.binarydata.BinaryData;
import com.example.blobmanagerv2.model.BlobInfo;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StreamUtils;

import jakarta.annotation.PostConstruct;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

@Service
public class BlobStorageService {

    private static final String STORAGE_SCOPE = "https://storage.azure.com/.default";
    private static final HttpHeaderName X_MS_DATE = HttpHeaderName.fromString("x-ms-date");
    private static final HttpHeaderName X_MS_VERSION = HttpHeaderName.fromString("x-ms-version");
    private static final String STORAGE_API_VERSION = "2023-11-03";

    private StorageServiceClient storageServiceClient;
    private ContainerClient containerClient;
    private BlobClient blobClient;
    private BlockBlobClient blockBlobClient;
    private String serviceEndpoint;

    @Value("${azure.storage.account-name:}")
    private String accountName;

    @Value("${azure.storage.blob-endpoint:}")
    private String blobEndpoint;

    @Value("${azure.storage.connection-string:}")
    private String connectionString;

    @PostConstruct
    public void init() {
        AzureBlobStorageBuilder builder;
        if (accountName != null && !accountName.isBlank()) {
            builder = createTokenAuthBuilder();
            this.serviceEndpoint = buildServiceEndpoint(accountName, blobEndpoint);
        } else if (connectionString != null && !connectionString.isBlank()) {
            ConnectionSettings settings = ConnectionSettings.from(connectionString, blobEndpoint);
            this.serviceEndpoint = settings.blobEndpoint();
            this.accountName = settings.accountName();
            builder = new AzureBlobStorageBuilder()
                    .url(serviceEndpoint)
                    .addHttpPipelinePolicy(new StorageSharedKeyPolicy(settings.accountName(), settings.accountKey()));
        } else {
            throw new IllegalStateException(
                    "Configure either AZURE_STORAGE_CONNECTION_STRING (local) or AZURE_STORAGE_ACCOUNT_NAME (Azure)");
        }

        this.storageServiceClient = builder.buildServiceClient();
        this.containerClient = builder.buildContainerClient();
        this.blobClient = builder.buildBlobClient();
        this.blockBlobClient = builder.buildBlockBlobClient();
    }

    private AzureBlobStorageBuilder createTokenAuthBuilder() {
        serviceEndpoint = buildServiceEndpoint(accountName, blobEndpoint);
        return new AzureBlobStorageBuilder()
                .url(serviceEndpoint)
                .addHttpPipelinePolicy(new OAuthBearerTokenAuthenticationPolicy(
                        new DefaultAzureCredentialTokenAdapter(new DefaultAzureCredentialBuilder().build()),
                        new OAuthTokenRequestContext().addScopes(STORAGE_SCOPE)));
    }

    void setClients(StorageServiceClient storageServiceClient, ContainerClient containerClient,
            BlobClient blobClient, BlockBlobClient blockBlobClient, String serviceEndpoint) {
        this.storageServiceClient = storageServiceClient;
        this.containerClient = containerClient;
        this.blobClient = blobClient;
        this.blockBlobClient = blockBlobClient;
        this.serviceEndpoint = serviceEndpoint;
    }

    public List<String> listContainers() {
        List<String> containers = new ArrayList<>();
        for (BlobContainerItem container : storageServiceClient.listBlobContainersSegment(null, null, null, null, null, null)) {
            containers.add(container.getName());
        }
        return containers;
    }

    public void createContainer(String containerName) {
        try {
            containerClient.create(containerName, null, null, null, null, null);
        } catch (HttpResponseException exception) {
            if (statusCode(exception) != 409) {
                throw exception;
            }
        }
    }

    public void deleteContainer(String containerName) {
        try {
            containerClient.delete(containerName, null, null, null, null, null);
        } catch (HttpResponseException exception) {
            if (statusCode(exception) != 404) {
                throw exception;
            }
        }
    }

    public List<BlobInfo> listBlobs(String containerName) {
        if (!containerExists(containerName)) {
            throw new IllegalArgumentException("Container not found: " + containerName);
        }

        ListBlobsFlatSegmentResponse response = containerClient.listBlobFlatSegment(
                containerName, null, null, null, null, null, null);
        List<BlobInfo> blobs = new ArrayList<>();
        if (response.getSegment() == null || response.getSegment().getBlobItems() == null) {
            return blobs;
        }

        for (BlobItemInternal item : response.getSegment().getBlobItems()) {
            BlobItemPropertiesInternal props = item.getProperties();
            blobs.add(new BlobInfo(
                    item.getName() != null ? item.getName().getContent() : null,
                    buildBlobUrl(containerName, item.getName() != null ? item.getName().getContent() : null),
                    props != null && props.getContentLength() != null ? props.getContentLength() : 0L,
                    props != null ? props.getContentType() : null,
                    props != null ? props.getLastModified() : null));
            }
        return blobs;
    }

    public void uploadBlob(String containerName, String blobName, InputStream data, long length, String contentType)
            throws IOException {
        createContainer(containerName);
        blockBlobClient.upload(containerName, blobName, length, BinaryData.fromStream(data, length), null, null, null,
                null, null, null, null, null, null, null, null, null, null, null, null, null, null, null,
                new BlobHttpHeaders().setContentType(contentType), null, null);
    }

    public void downloadBlob(String containerName, String blobName, OutputStream outputStream)
            throws IOException {
        if (!blobExists(containerName, blobName)) {
            throw new IllegalArgumentException("Blob not found: " + blobName);
        }

        try (InputStream inputStream = blobClient.download(containerName, blobName, null, null, null, null, null,
                null, null, null, null, null, null, null, null, null, null)) {
            StreamUtils.copy(inputStream, outputStream);
        }
    }

    public BlobInfo getBlobInfo(String containerName, String blobName) {
        Response<Void> response;
        try {
            response = blobClient.getPropertiesWithResponse(containerName, blobName, null, null, null, null, null,
                    null, null, null, null, null, null, null);
        } catch (HttpResponseException exception) {
            if (statusCode(exception) == 404) {
                throw new IllegalArgumentException("Blob not found: " + blobName);
            }
            throw exception;
        }

        if (response == null) {
            throw new IllegalArgumentException("Blob not found: " + blobName);
        }

        return new BlobInfo(
                blobName,
                buildBlobUrl(containerName, blobName),
                parseContentLength(response),
                response.getHeaders().getValue(HttpHeaderName.CONTENT_TYPE),
                parseLastModified(response));
    }

    public boolean deleteBlob(String containerName, String blobName) {
        try {
            blobClient.delete(containerName, blobName, null, null, null, null, null, null, null, null, null, null,
                    null, null);
            return true;
        } catch (HttpResponseException exception) {
            if (statusCode(exception) == 404) {
                return false;
            }
            throw exception;
        }
    }

    private boolean containerExists(String containerName) {
        try {
            containerClient.getPropertiesWithResponse(containerName, null, null, null,
                    io.clientcore.core.http.models.RequestContext.none());
            return true;
        } catch (HttpResponseException exception) {
            if (statusCode(exception) == 404) {
                return false;
            }
            throw exception;
        }
    }

    private boolean blobExists(String containerName, String blobName) {
        try {
            blobClient.getPropertiesWithResponse(containerName, blobName, null, null, null, null, null, null, null,
                    null, null, null, null, io.clientcore.core.http.models.RequestContext.none());
            return true;
        } catch (HttpResponseException exception) {
            if (statusCode(exception) == 404) {
                return false;
            }
            throw exception;
        }
    }

    private String buildServiceEndpoint(String accountName, String configuredBlobEndpoint) {
        if (configuredBlobEndpoint != null && !configuredBlobEndpoint.isBlank()) {
            return trimTrailingSlash(configuredBlobEndpoint);
        }
        return "https://" + accountName + ".blob.core.windows.net";
    }

    private String buildBlobUrl(String containerName, String blobName) {
        return trimTrailingSlash(serviceEndpoint) + "/" + containerName + "/" + blobName;
    }

    private static String trimTrailingSlash(String value) {
        return value != null && value.endsWith("/") ? value.substring(0, value.length() - 1) : value;
    }

    private static int statusCode(HttpResponseException exception) {
        return exception.getResponse() != null ? exception.getResponse().getStatusCode() : -1;
    }

    private static long parseContentLength(Response<Void> response) {
        String contentLength = response.getHeaders().getValue(HttpHeaderName.CONTENT_LENGTH);
        return contentLength != null ? Long.parseLong(contentLength) : 0L;
    }

    private static OffsetDateTime parseLastModified(Response<Void> response) {
        String lastModified = response.getHeaders().getValue(HttpHeaderName.LAST_MODIFIED);
        if (lastModified == null || lastModified.isBlank()) {
            return null;
        }
        return OffsetDateTime.from(DateTimeFormatter.RFC_1123_DATE_TIME.parse(lastModified));
    }

    private record ConnectionSettings(String accountName, String accountKey, String blobEndpoint) {
        static ConnectionSettings from(String connectionString, String configuredBlobEndpoint) {
            Map<String, String> values = new LinkedHashMap<>();
            for (String part : connectionString.split(";")) {
                int separatorIndex = part.indexOf('=');
                if (separatorIndex > 0) {
                    values.put(part.substring(0, separatorIndex), part.substring(separatorIndex + 1));
                }
            }

            String parsedAccountName = values.get("AccountName");
            String parsedAccountKey = values.get("AccountKey");
            String parsedBlobEndpoint = configuredBlobEndpoint;
            if (parsedBlobEndpoint == null || parsedBlobEndpoint.isBlank()) {
                parsedBlobEndpoint = values.get("BlobEndpoint");
            }
            if ((parsedBlobEndpoint == null || parsedBlobEndpoint.isBlank()) && parsedAccountName != null) {
                parsedBlobEndpoint = "https://" + parsedAccountName + ".blob.core.windows.net";
            }

            if (parsedAccountName == null || parsedAccountKey == null || parsedBlobEndpoint == null) {
                throw new IllegalStateException("Connection string must include AccountName, AccountKey, and BlobEndpoint");
            }

            return new ConnectionSettings(parsedAccountName, parsedAccountKey, trimTrailingSlash(parsedBlobEndpoint));
        }
    }

    private static final class DefaultAzureCredentialTokenAdapter implements OAuthTokenCredential {
        private final TokenCredential credential;

        private DefaultAzureCredentialTokenAdapter(TokenCredential credential) {
            this.credential = credential;
        }

        @Override
        public io.clientcore.core.credentials.oauth.AccessToken getToken(OAuthTokenRequestContext request) {
            AccessToken token = credential.getTokenSync(
                    new TokenRequestContext().addScopes(request.getScopes().toArray(String[]::new)));
            return new io.clientcore.core.credentials.oauth.AccessToken(token.getToken(), token.getExpiresAt());
        }
    }

    private static final class StorageSharedKeyPolicy implements HttpPipelinePolicy {
        private final String accountName;
        private final byte[] accountKey;

        private StorageSharedKeyPolicy(String accountName, String accountKey) {
            this.accountName = accountName;
            this.accountKey = Base64.getDecoder().decode(accountKey);
        }

        @Override
        public Response<BinaryData> process(HttpRequest httpRequest, HttpPipelineNextPolicy next) {
            httpRequest.getHeaders().set(X_MS_DATE,
                    DateTimeFormatter.RFC_1123_DATE_TIME.format(OffsetDateTime.now(ZoneOffset.UTC)));
            if (httpRequest.getHeaders().getValue(X_MS_VERSION) == null) {
                httpRequest.getHeaders().set(X_MS_VERSION, STORAGE_API_VERSION);
            }
            httpRequest.getHeaders().set(HttpHeaderName.AUTHORIZATION,
                    "SharedKey " + accountName + ":" + buildSignature(httpRequest));
            return next.process();
        }

        private String buildSignature(HttpRequest request) {
            String stringToSign = String.join("\n",
                    request.getHttpMethod().toString(),
                    headerValue(request, HttpHeaderName.CONTENT_ENCODING),
                    headerValue(request, HttpHeaderName.CONTENT_LANGUAGE),
                    contentLengthValue(request),
                    headerValue(request, HttpHeaderName.CONTENT_MD5),
                    headerValue(request, HttpHeaderName.CONTENT_TYPE),
                    "",
                    headerValue(request, HttpHeaderName.IF_MODIFIED_SINCE),
                    headerValue(request, HttpHeaderName.IF_MATCH),
                    headerValue(request, HttpHeaderName.IF_NONE_MATCH),
                    headerValue(request, HttpHeaderName.IF_UNMODIFIED_SINCE),
                    headerValue(request, HttpHeaderName.RANGE),
                    canonicalizedHeaders(request),
                    canonicalizedResource(request));

            try {
                Mac mac = Mac.getInstance("HmacSHA256");
                mac.init(new SecretKeySpec(accountKey, "HmacSHA256"));
                return Base64.getEncoder().encodeToString(mac.doFinal(stringToSign.getBytes(StandardCharsets.UTF_8)));
            } catch (Exception exception) {
                throw new IllegalStateException("Failed to sign blob storage request", exception);
            }
        }

        private String canonicalizedHeaders(HttpRequest request) {
            return request.getHeaders().stream()
                    .map(HttpHeader::getName)
                    .map(HttpHeaderName::toString)
                    .filter(name -> name.toLowerCase(Locale.ROOT).startsWith("x-ms-"))
                    .sorted()
                    .map(name -> name.toLowerCase(Locale.ROOT) + ":"
                            + normalizeWhitespace(request.getHeaders().getValue(HttpHeaderName.fromString(name))))
                    .reduce((left, right) -> left + "\n" + right)
                    .orElse("");
        }

        private String canonicalizedResource(HttpRequest request) {
            StringBuilder builder = new StringBuilder("/").append(accountName).append(request.getUri().getPath());
            Map<String, List<String>> queryParameters = parseQueryParameters(request.getUri().getRawQuery());
            queryParameters.entrySet().stream()
                    .sorted(Map.Entry.comparingByKey())
                    .forEach(entry -> builder.append("\n")
                            .append(entry.getKey())
                            .append(":")
                            .append(String.join(",", entry.getValue())));
            return builder.toString();
        }

        private Map<String, List<String>> parseQueryParameters(String rawQuery) {
            if (rawQuery == null || rawQuery.isBlank()) {
                return Collections.emptyMap();
            }

            Map<String, List<String>> values = new TreeMap<>();
            for (String pair : rawQuery.split("&")) {
                String[] tokens = pair.split("=", 2);
                String key = URLDecoder.decode(tokens[0], StandardCharsets.UTF_8).toLowerCase(Locale.ROOT);
                String value = tokens.length > 1 ? URLDecoder.decode(tokens[1], StandardCharsets.UTF_8) : "";
                values.computeIfAbsent(key, ignored -> new ArrayList<>()).add(value);
            }
            values.values().forEach(list -> list.sort(Comparator.naturalOrder()));
            return values;
        }

        private String contentLengthValue(HttpRequest request) {
            String contentLength = headerValue(request, HttpHeaderName.CONTENT_LENGTH);
            return "0".equals(contentLength) ? "" : contentLength;
        }

        private String headerValue(HttpRequest request, HttpHeaderName name) {
            String value = request.getHeaders().getValue(name);
            return value == null ? "" : value;
        }

        private String normalizeWhitespace(String value) {
            return value == null ? "" : value.replaceAll("\\s+", " ").trim();
        }
    }
}
