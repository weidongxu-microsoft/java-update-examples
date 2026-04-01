package com.contoso.messaging;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.Map;

/**
 * Application-level replacement for the removed com.microsoft.azure.servicebus.primitives.ConnectionStringBuilder.
 * Parses Service Bus connection strings and provides access to individual components.
 */
public class ConnectionStringProperties {

    private final String endpoint;
    private final String entityPath;
    private final String sasKeyName;
    private final String sasKey;

    public ConnectionStringProperties(String connectionString) {
        Map<String, String> parts = parseConnectionString(connectionString);
        this.endpoint = parts.getOrDefault("Endpoint", "");
        this.entityPath = parts.getOrDefault("EntityPath", "");
        this.sasKeyName = parts.getOrDefault("SharedAccessKeyName", "");
        this.sasKey = parts.getOrDefault("SharedAccessKey", "");
    }

    public ConnectionStringProperties(String endpoint, String entityPath,
                                       String sasKeyName, String sasKey) {
        this.endpoint = endpoint;
        this.entityPath = entityPath;
        this.sasKeyName = sasKeyName;
        this.sasKey = sasKey;
    }

    public String getEntityPath() {
        return entityPath;
    }

    public URI getEndpoint() {
        try {
            return new URI(endpoint);
        } catch (URISyntaxException e) {
            throw new IllegalArgumentException("Invalid endpoint URI: " + endpoint, e);
        }
    }

    public String getSasKeyName() {
        return sasKeyName;
    }

    public String getSasKey() {
        return sasKey;
    }

    /**
     * Builds a connection string (without EntityPath) suitable for ServiceBusClientBuilder.
     */
    public String toConnectionString() {
        StringBuilder sb = new StringBuilder();
        sb.append("Endpoint=").append(endpoint).append(";");
        sb.append("SharedAccessKeyName=").append(sasKeyName).append(";");
        sb.append("SharedAccessKey=").append(sasKey);
        if (entityPath != null && !entityPath.isEmpty()) {
            sb.append(";EntityPath=").append(entityPath);
        }
        return sb.toString();
    }

    @Override
    public String toString() {
        return toConnectionString();
    }

    private static Map<String, String> parseConnectionString(String connectionString) {
        Map<String, String> parts = new HashMap<>();
        if (connectionString == null || connectionString.isEmpty()) {
            return parts;
        }
        String[] segments = connectionString.split(";");
        for (String segment : segments) {
            int idx = segment.indexOf('=');
            if (idx > 0) {
                String key = segment.substring(0, idx).trim();
                String value = segment.substring(idx + 1).trim();
                parts.put(key, value);
            }
        }
        return parts;
    }
}
