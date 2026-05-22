/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */

package com.microsoft.azure.management.batch.samples;

import com.azure.core.credential.TokenCredential;
import com.azure.core.http.policy.HttpLogDetailLevel;
import com.azure.core.http.policy.HttpLogOptions;
import com.azure.core.management.AzureEnvironment;
import com.azure.core.management.Region;
import com.azure.core.management.profile.AzureProfile;
import com.azure.identity.DefaultAzureCredentialBuilder;
import com.azure.resourcemanager.AzureResourceManager;
import com.azure.resourcemanager.batch.BatchManager;
import com.azure.resourcemanager.batch.models.AccountKeyType;
import com.azure.resourcemanager.batch.models.Application;
import com.azure.resourcemanager.batch.models.ApplicationPackage;
import com.azure.resourcemanager.batch.models.AutoStorageBaseProperties;
import com.azure.resourcemanager.batch.models.BatchAccount;
import com.azure.resourcemanager.batch.models.BatchAccountKeys;
import com.azure.resourcemanager.batch.models.BatchAccountRegenerateKeyParameters;
import com.azure.resourcemanager.storage.models.StorageAccount;
import com.azure.resourcemanager.storage.models.StorageAccountKey;
import com.microsoft.azure.management.samples.Utils;

import java.util.List;

/**
 * Azure Batch sample for managing batch accounts -
 *  - Get subscription batch account quota for a particular location.
 *  - List all the batch accounts, look if quota allows you to create a new batch account at specified location by counting batch accounts in that particular location.
 *  - Create a batch account with new application and application package, along with new storage account.
 *  - Get the keys for batch account.
 *  - Regenerate keys for batch account
 *  - Regenerate the keys of storage accounts, sync with batch account.
 *  - Update application's display name.
 *  - Create another batch account using existing storage account.
 *  - List the batch account.
 *  - Delete the batch account.
 *      - Delete the application packages.
 *      - Delete applications.
 */

public final class ManageBatchAccount {

    /**
     * Main function which runs the actual sample.
     * @param batchManager instance of the BatchManager client
     * @param azure instance of the AzureResourceManager client (for storage and resource group operations)
     * @return true if sample runs successfully
     */
    public static boolean runSample(BatchManager batchManager, AzureResourceManager azure) {
        final String batchAccountName = "samplebatchaccount";
        final String storageAccountName = "samplestorageacct";
        final String applicationName = "application";
        final String applicationDisplayName = "My application display name";
        final String applicationPackageName = "app_package";
        final String batchAccountName2 = "samplebatchaccount2";
        final String rgName = "sample-batch-rg";
        final Region region = Region.AUSTRALIA_SOUTHEAST;
        final Region region2 = Region.US_WEST;

        try {

            // ===========================================================
            // Get how many batch accounts can be created in specified region.

            int allowedNumberOfBatchAccounts = batchManager.locations()
                    .getQuotas(region.name()).accountQuota();

            // ===========================================================
            // List all the batch accounts in subscription.

            int batchAccountsAtSpecificRegion = 0;
            for (BatchAccount batchAccount : batchManager.batchAccounts().list()) {
                if (region.name().equalsIgnoreCase(batchAccount.location())) {
                    batchAccountsAtSpecificRegion++;
                }
            }

            if (batchAccountsAtSpecificRegion >= allowedNumberOfBatchAccounts) {
                System.out.println("No more batch accounts can be created at "
                        + region + " region, this region already have "
                        + batchAccountsAtSpecificRegion
                        + " batch accounts, current quota to create batch account in "
                        + region + " region is " + allowedNumberOfBatchAccounts + ".");
                return false;
            }

            // ============================================================
            // Create a storage account (required before creating the batch account in Track 2)

            System.out.println("Creating a storage account");

            StorageAccount storageAccount = azure.storageAccounts()
                    .define(storageAccountName)
                    .withRegion(region)
                    .withNewResourceGroup(rgName)
                    .create();

            System.out.println("Created storage account: " + storageAccount.name());

            // ============================================================
            // Create a batch account

            System.out.println("Creating a batch Account");

            BatchAccount batchAccount = batchManager.batchAccounts()
                    .define(batchAccountName)
                    .withRegion(region)
                    .withExistingResourceGroup(rgName)
                    .withAutoStorage(new AutoStorageBaseProperties()
                            .withStorageAccountId(storageAccount.id()))
                    .create();

            System.out.println("Created a batch Account:");
            Utils.print(batchAccount);

            // ============================================================
            // Create application and application package for the batch account

            System.out.println("Creating application");

            Application application = batchManager.applications()
                    .define(applicationName)
                    .withExistingBatchAccount(rgName, batchAccountName)
                    .withDisplayName(applicationDisplayName)
                    .withAllowUpdates(true)
                    .create();

            System.out.println("Created application: " + application.name());

            System.out.println("Creating application package");

            ApplicationPackage applicationPackage = batchManager.applicationPackages()
                    .define(applicationPackageName)
                    .withExistingApplication(rgName, batchAccountName, applicationName)
                    .create();

            System.out.println("Created application package: " + applicationPackage.name());

            // ============================================================
            // Get | regenerate batch account access keys

            System.out.println("Getting batch account access keys");

            BatchAccountKeys batchAccountKeys = batchManager.batchAccounts()
                    .getKeys(rgName, batchAccountName);

            Utils.print(batchAccountKeys);

            System.out.println("Regenerating primary batch account primary access key");

            batchAccountKeys = batchManager.batchAccounts()
                    .regenerateKey(rgName, batchAccountName,
                            new BatchAccountRegenerateKeyParameters()
                                    .withKeyName(AccountKeyType.PRIMARY));

            Utils.print(batchAccountKeys);

            // ============================================================
            // Regenerate the keys for storage account
            List<StorageAccountKey> storageAccountKeys = storageAccount.getKeys();

            Utils.print(storageAccountKeys);

            System.out.println("Regenerating first storage account access key");

            storageAccountKeys = storageAccount.regenerateKey(storageAccountKeys.get(0).keyName());

            Utils.print(storageAccountKeys);

            // ============================================================
            // Synchronize storage account keys with batch account

            batchManager.batchAccounts().synchronizeAutoStorageKeys(rgName, batchAccountName);

            // ============================================================
            // Update name of application.
            batchManager.applications().get(rgName, batchAccountName, applicationName)
                    .update()
                    .withDisplayName("New application display name")
                    .apply();

            batchAccount = batchManager.batchAccounts().getByResourceGroup(rgName, batchAccountName);
            Utils.print(batchAccount);

            // ============================================================
            // Create another batch account

            System.out.println("Creating another Batch Account");

            allowedNumberOfBatchAccounts = batchManager.locations()
                    .getQuotas(region2.name()).accountQuota();

            // ===========================================================
            // List all the batch accounts in subscription.

            batchAccountsAtSpecificRegion = 0;
            for (BatchAccount batch : batchManager.batchAccounts().list()) {
                if (region2.name().equalsIgnoreCase(batch.location())) {
                    batchAccountsAtSpecificRegion++;
                }
            }

            BatchAccount batchAccount2 = null;
            if (batchAccountsAtSpecificRegion < allowedNumberOfBatchAccounts) {
                batchAccount2 = batchManager.batchAccounts()
                        .define(batchAccountName2)
                        .withRegion(region2)
                        .withExistingResourceGroup(rgName)
                        .withAutoStorage(new AutoStorageBaseProperties()
                                .withStorageAccountId(storageAccount.id()))
                        .create();

                System.out.println("Created second Batch Account:");
                Utils.print(batchAccount2);
            }

            // ============================================================
            // List batch accounts

            System.out.println("Listing Batch accounts");

            int i = 0;
            for (BatchAccount ba : batchManager.batchAccounts().listByResourceGroup(rgName)) {
                System.out.println("Batch Account (" + i + ") " + ba.name());
                i++;
            }

            // ============================================================
            // Refresh a batch account.
            batchAccount = batchManager.batchAccounts().getByResourceGroup(rgName, batchAccountName);
            Utils.print(batchAccount);

            // ============================================================
            // Delete a batch account

            System.out.println("Deleting a batch account - " + batchAccount.name());

            for (Application app : batchManager.applications().list(rgName, batchAccountName)) {
                for (ApplicationPackage pkg : batchManager.applicationPackages()
                        .list(rgName, batchAccountName, app.name())) {
                    System.out.println("Deleting a application package - " + pkg.name());
                    batchManager.applicationPackages()
                            .delete(rgName, batchAccountName, app.name(), pkg.name());
                }
                System.out.println("Deleting a application - " + app.name());
                batchManager.applications().delete(rgName, batchAccountName, app.name());
            }

            batchManager.batchAccounts().deleteById(batchAccount.id());

            System.out.println("Deleted batch account");

            if (batchAccount2 != null) {
                System.out.println("Deleting second batch account - " + batchAccount2.name());
                batchManager.batchAccounts().deleteById(batchAccount2.id());
                System.out.println("Deleted second batch account");
            }

            return true;
        } catch (Exception f) {
            System.out.println(f.getMessage());
            f.printStackTrace();
        } finally {
            try {
                System.out.println("Deleting Resource Group: " + rgName);
                azure.resourceGroups().deleteByName(rgName);
                System.out.println("Deleted Resource Group: " + rgName);
            } catch (Exception e) {
                System.out.println("Did not create any resources in Azure. No clean up is necessary");
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
            // TODO: The original code authenticated using a credential file (AZURE_AUTH_LOCATION),
            // which is discouraged because it relies on long-lived secrets on disk and conflicts
            // with Azure's security-by-default guidance. It has been replaced with
            // DefaultAzureCredential. This change alters the authentication mechanism, so the
            // resulting code path requires extra testing (local dev, CI, and target runtime
            // identities) before it is considered production-ready.
            TokenCredential credential = new DefaultAzureCredentialBuilder().build();
            AzureProfile profile = new AzureProfile(AzureEnvironment.AZURE);

            AzureResourceManager azure = AzureResourceManager.configure()
                    .withLogOptions(new HttpLogOptions().setLogLevel(HttpLogDetailLevel.BASIC))
                    .authenticate(credential, profile)
                    .withDefaultSubscription();

            // Print selected subscription
            System.out.println("Selected subscription: " + azure.subscriptionId());

            // BatchManager is a non-premium client; in production code consider adding
            // ProviderRegistrationPolicy to auto-register the Microsoft.Batch provider.
            BatchManager batchManager = BatchManager.configure()
                    .withLogOptions(new HttpLogOptions().setLogLevel(HttpLogDetailLevel.BASIC))
                    .authenticate(credential, profile);

            runSample(batchManager, azure);
        } catch (Exception e) {
            System.out.println(e.getMessage());
            e.printStackTrace();
        }
    }

    private ManageBatchAccount() {
    }
}