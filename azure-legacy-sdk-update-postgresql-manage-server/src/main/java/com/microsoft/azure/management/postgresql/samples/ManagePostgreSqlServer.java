/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */

package com.microsoft.azure.management.postgresql.samples;

import com.microsoft.azure.credentials.MSICredentials;
import com.microsoft.azure.management.Azure;
import com.microsoft.azure.management.postgresql.v2017_12_01.GeoRedundantBackup;
import com.microsoft.azure.management.postgresql.v2017_12_01.Server;
import com.microsoft.azure.management.postgresql.v2017_12_01.ServerPropertiesForDefaultCreate;
import com.microsoft.azure.management.postgresql.v2017_12_01.ServerPropertiesForCreate;
import com.microsoft.azure.management.postgresql.v2017_12_01.ServerVersion;
import com.microsoft.azure.management.postgresql.v2017_12_01.Sku;
import com.microsoft.azure.management.postgresql.v2017_12_01.SkuTier;
import com.microsoft.azure.management.postgresql.v2017_12_01.SslEnforcementEnum;
import com.microsoft.azure.management.postgresql.v2017_12_01.StorageAutogrow;
import com.microsoft.azure.management.postgresql.v2017_12_01.StorageProfile;
import com.microsoft.azure.management.postgresql.v2017_12_01.Servers;
import com.microsoft.azure.management.postgresql.v2017_12_01.implementation.PostgreSQLManager;
import com.microsoft.azure.management.resources.fluentcore.arm.Region;
import com.microsoft.azure.management.resources.fluentcore.utils.SdkContext;
import com.microsoft.rest.LogLevel;

/**
 * Azure PostgreSQL sample for creating and updating a flexible server.
 *  - Create a PostgreSQL server
 *  - Update SKU and storage settings on the server
 */
public final class ManagePostgreSqlServer {

    private ManagePostgreSqlServer() {
    }

    /**
     * Main run method for the sample.
     * @param azure the authenticated Azure client
     * @param postgresManager manager for the legacy PostgreSQL API
     * @return true if sample runs successfully
     */
    public static boolean runSample(Azure azure, PostgreSQLManager postgresManager) {
        final Region region = Region.US_EAST;
        final String resourceGroupName = SdkContext.randomResourceName("rgPostgres", 20);
        final String serverName = SdkContext.randomResourceName("pgsrv", 20);
        final String administratorLogin = "pgadminuser";
        final String administratorPassword = "StrongP@ssw0rd123";

        try {
            // Create the resource group up front so both clients can reference it.
            System.out.println("Creating resource group: " + resourceGroupName);
            azure.resourceGroups().define(resourceGroupName)
                .withRegion(region)
                .create();

            // Define the initial SKU and create properties for the server.
            Sku initialSku = new Sku()
                .withName("GP_Gen5_2")
                .withTier(SkuTier.GENERAL_PURPOSE)
                .withCapacity(2)
                .withFamily("Gen5");

            StorageProfile initialStorage = new StorageProfile()
                .withStorageMB(5120)
                .withBackupRetentionDays(7)
                .withGeoRedundantBackup(GeoRedundantBackup.DISABLED)
                .withStorageAutogrow(StorageAutogrow.ENABLED);

            ServerPropertiesForCreate createProperties = new ServerPropertiesForDefaultCreate()
                .withAdministratorLogin(administratorLogin)
                .withAdministratorLoginPassword(administratorPassword)
                .withVersion(ServerVersion.ONE_ONE)
                .withSslEnforcement(SslEnforcementEnum.ENABLED)
                .withStorageProfile(initialStorage);

            System.out.println("Creating PostgreSQL server: " + serverName);
            Server server = createServer(
                postgresManager.servers(),
                region,
                resourceGroupName,
                serverName,
                createProperties,
                initialSku);

            printServer(server);

            // Prepare updated SKU and storage settings for the server.
            Sku scaledSku = new Sku()
                .withName("GP_Gen5_4")
                .withTier(SkuTier.GENERAL_PURPOSE)
                .withCapacity(4)
                .withFamily("Gen5");

            StorageProfile updatedStorage = new StorageProfile()
                .withStorageMB(10240)
                .withBackupRetentionDays(14)
                .withGeoRedundantBackup(GeoRedundantBackup.DISABLED)
                .withStorageAutogrow(StorageAutogrow.ENABLED);

            System.out.println("Updating PostgreSQL server: " + serverName);
            server = server.update()
                .withTag("language", "java-updated")
                .withSku(scaledSku)
                .withStorageProfile(updatedStorage)
                .apply();

            printServer(server);
            return true;
        } catch (Exception e) {
            System.out.println(e.getMessage());
            e.printStackTrace();
        } finally {
            try {
                System.out.println("Deleting resource group: " + resourceGroupName);
                azure.resourceGroups().deleteByName(resourceGroupName);
                System.out.println("Deleted resource group: " + resourceGroupName);
            } catch (NullPointerException ignore) {
                System.out.println("No resources were created, so no cleanup is required.");
            } catch (Exception cleanupException) {
                cleanupException.printStackTrace();
            }
        }
        return false;
    }

    /**
     * Main entry point.
     * @param args the parameters
     */
    public static void main(String[] args) {
        try {
            final String subscriptionId = System.getenv("AZURE_SUBSCRIPTION_ID");
            if (subscriptionId == null || subscriptionId.isEmpty()) {
                throw new IllegalStateException("AZURE_SUBSCRIPTION_ID is not set.");
            }

            MSICredentials credentials = new MSICredentials();
            final String userAssignedClientId = System.getenv("AZURE_CLIENT_ID");
            if (userAssignedClientId != null && !userAssignedClientId.isEmpty()) {
                credentials = credentials.withClientId(userAssignedClientId);
            }

            Azure azure = Azure.configure()
                .withLogLevel(LogLevel.BASIC)
                .authenticate(credentials)
                .withSubscription(subscriptionId);

            PostgreSQLManager postgresManager = PostgreSQLManager.configure()
                .withLogLevel(LogLevel.BASIC)
                .authenticate(credentials, subscriptionId);

            System.out.println("Using subscription: " + subscriptionId);
            runSample(azure, postgresManager);
        } catch (Exception e) {
            System.out.println(e.getMessage());
            e.printStackTrace();
        }
    }

    private static void printServer(Server server) {
        System.out.println(new StringBuilder()
            .append("PostgreSQL server id: ").append(server.id())
            .append("\n\tName: ").append(server.name())
            .append("\n\tRegion: ").append(server.regionName())
            .append("\n\tFully Qualified Domain Name: ").append(server.fullyQualifiedDomainName())
            .append("\n\tVersion: ").append(server.version())
            .append("\n\tAdministrator Login: ").append(server.administratorLogin())
            .append("\n\tSku: ").append(server.sku() == null ? "n/a" : server.sku().name())
            .append("\n\tStorage (MB): ").append(server.storageProfile() == null ? "n/a" : server.storageProfile().storageMB())
            .append("\n\tBackup Retention (days): ").append(server.storageProfile() == null ? "n/a" : server.storageProfile().backupRetentionDays())
            .append("\n\tSSL Enforcement: ").append(server.sslEnforcement())
            .append("\n\tTags: ").append(server.tags())
            .toString());
    }

    static Server createServer(
            Servers servers,
            Region region,
            String resourceGroupName,
            String serverName,
            ServerPropertiesForCreate createProperties,
            Sku sku) {
        return servers
            .define(serverName)
            .withRegion(region.name())
            .withExistingResourceGroup(resourceGroupName)
            .withProperties(createProperties)
            .withSku(sku)
            .withTag("sample", "postgresql")
            .withTag("language", "java")
            .create();
    }
}
