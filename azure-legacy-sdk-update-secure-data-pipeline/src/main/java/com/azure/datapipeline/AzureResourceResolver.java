package com.azure.datapipeline;

import java.util.List;
import java.util.Map;

/**
 * Generic resolver for Azure resource types. Implementations are parameterized
 * on a specific Azure SDK response type (e.g., {@code SecretBundle}, {@code KeyBundle}).
 *
 * <p>Migration challenge: The generic type parameter flows through the entire interface
 * contract. Changing it (e.g., {@code SecretBundle} to {@code KeyVaultSecret}) requires
 * updating all implementations, callers, and method bodies that operate on {@code T}.</p>
 *
 * @param <T> the Azure SDK resource type to resolve
 */
public interface AzureResourceResolver<T> {

    /**
     * Resolves a single resource by name from the specified vault.
     *
     * @param vaultUrl the vault base URL
     * @param name     the resource name
     * @return the resolved resource
     */
    T resolve(String vaultUrl, String name);

    /**
     * Resolves all resources from the specified vault.
     *
     * @param vaultUrl the vault base URL
     * @return list of all resources
     */
    List<T> resolveAll(String vaultUrl);

    /**
     * Extracts the unique identifier from a resource.
     *
     * @param resource the resolved resource
     * @return the resource identifier URL
     */
    String getIdentifier(T resource);

    /**
     * Converts a resource to a metadata map suitable for storage in blob metadata.
     *
     * @param resource the resolved resource
     * @return key-value metadata derived from the resource
     */
    Map<String, String> toMetadata(T resource);
}
