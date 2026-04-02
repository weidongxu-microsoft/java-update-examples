/**
 * https://github.com/Azure/azure-libraries-for-java/blob/b324fabb9ba2c9687614c800c6ae69e189ce990e/azure-mgmt-resources/src/test/java/com/microsoft/azure/management/resources/core/TestBase.java
 */
package com.microsoft.azure.management.clientinitialization;

import com.microsoft.azure.AzureEnvironment;
import com.microsoft.azure.AzureResponseBuilder;
import com.microsoft.azure.credentials.ApplicationTokenCredentials;
import com.microsoft.azure.management.Azure;
import com.microsoft.azure.management.resources.core.ResourceGroupTaggingInterceptor;
import com.microsoft.azure.management.resources.fluentcore.utils.ProviderRegistrationInterceptor;
import com.microsoft.azure.serializer.AzureJacksonAdapter;
import com.microsoft.rest.LogLevel;
import com.microsoft.rest.RestClient;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.TimeUnit;

public class AzureInitialization {
    public static void main(String[] args) throws IOException {
        // initialize using env
        String clientId = System.getenv("AZURE_CLIENT_ID");
        String tenantId = System.getenv("AZURE_TENANT_ID");
        String clientSecret = System.getenv("AZURE_CLIENT_SECRET");
        String subscriptionId = System.getenv("AZURE_SUBSCRIPTION_ID");
        if (clientId == null || tenantId == null || clientSecret == null || subscriptionId == null) {
            throw new IllegalArgumentException("When running tests in record mode either 'AZURE_AUTH_LOCATION' or 'AZURE_CLIENT_ID, AZURE_TENANT_ID, AZURE_CLIENT_SECRET and AZURE_SUBSCRIPTION_ID' needs to be set");
        }

        ApplicationTokenCredentials credentials = new ApplicationTokenCredentials(clientId, tenantId, clientSecret, AzureEnvironment.AZURE);
        credentials.withDefaultSubscriptionId(subscriptionId);

        String baseUrl = credentials.environment().url(AzureEnvironment.Endpoint.RESOURCE_MANAGER);
        RestClient.Builder builder = new RestClient.Builder()
            .withBaseUrl(baseUrl)
            .withSerializerAdapter(new AzureJacksonAdapter())
            .withResponseBuilderFactory(new AzureResponseBuilder.Factory())
            .withInterceptor(new ProviderRegistrationInterceptor(credentials))
            .withNetworkInterceptor(new ResourceGroupTaggingInterceptor())
            .withCredentials(credentials)
            .withLogLevel(LogLevel.BODY_AND_HEADERS)
            .withReadTimeout(3, TimeUnit.MINUTES);

        Azure.Authenticated azureAuthed = Azure.authenticate(builder.build(), subscriptionId, credentials.domain());
        Azure azure = azureAuthed.withSubscription(subscriptionId);

        // initialize using credential file
        final File credentialFile = new File(System.getenv("AZURE_AUTH_LOCATION"));
        azure = Azure.configure()
            .authenticate(credentialFile)
            .withDefaultSubscription();
    }
}
