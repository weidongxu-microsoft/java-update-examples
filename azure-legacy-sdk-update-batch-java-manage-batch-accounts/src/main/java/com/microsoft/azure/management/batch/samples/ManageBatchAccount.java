/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */

package com.microsoft.azure.management.batch.samples;

import com.azure.core.credential.TokenCredential;
import com.azure.core.management.AzureEnvironment;
import com.azure.core.management.Region;
import com.azure.core.management.profile.AzureProfile;
import com.azure.core.util.Context;
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
     * @param azure instance of the azure resource manager client
     * @param batchManager instance of the batch manager client
     * @return true if sample runs successfully
     */
    public static boolean runSample(AzureResourceManager azure, BatchManager batchManager) {
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
                    .getQuotasWithResponse(region.toString(), Context.NONE)
                    .getValue()
                    .accountQuota();

            // ===========================================================
            // List all the batch accounts in subscription.

            Iterable<BatchAccount> batchAccounts = batchManager.batchAccounts().list(Context.NONE);
            int batchAccountsAtSpecificRegion = 0;
            for (BatchAccount batchAccount : batchAccounts) {
                if (batchAccount.region().equals(region)) {
                    batchAccountsAtSpecificRegion++;
                }
            }

            if (batchAccountsAtSpecificRegion >= allowedNumberOfBatchAccounts) {
                System.out.println("No more batch accounts can be created at "
                        + region + " region, this region already have "
                        + batchAccountsAtSpecificRegion
                        + " batch accounts, current quota to create batch account in "
                        + region + " region is " +  allowedNumberOfBatchAccounts + ".");
                return false;
            }

            // ============================================================
            // Create a storage account for use as batch auto-storage.
            // Note: In Track 2, auto-storage cannot be provisioned inline during batch account
            // creation. The storage account is created first and linked via AutoStorageBaseProperties.

            System.out.println("Creating a storage account for batch auto-storage");

            StorageAccount storageAccount = azure.storageAccounts().define(storageAccountName)
                    .withRegion(region)
                    .withNewResourceGroup(rgName)
                    .create();

            System.out.println("Created storage account: " + storageAccount.name());

            // ============================================================
            // Create a batch account

            System.out.println("Creating a batch Account");

            BatchAccount batchAccount = batchManager.batchAccounts().define(batchAccountName)
                    .withRegion(region)
                    .withExistingResourceGroup(rgName)
                    .withAutoStorage(new AutoStorageBaseProperties().withStorageAccountId(storageAccount.id()))
                    .create();

            // Create application and application package separately
            // (Track 2 requires separate calls; inline definition via defineNewApplication is not supported)
            batchManager.applications().define(applicationName)
                    .withExistingBatchAccount(rgName, batchAccountName)
                    .withDisplayName(applicationDisplayName)
                    .withAllowUpdates(true)
                    .create();

            batchManager.applicationPackages().define(applicationPackageName)
                    .withExistingApplication(rgName, batchAccountName, applicationName)
                    .create();

            // Refresh batch account to reflect latest state
            batchAccount = batchManager.batchAccounts()
                    .getByResourceGroupWithResponse(rgName, batchAccountName, Context.NONE)
                    .getValue();

            System.out.println("Created a batch Account:");
            Utils.print(batchAccount, batchManager);

            // ============================================================
            // Get | regenerate batch account access keys

            System.out.println("Getting batch account access keys");

            BatchAccountKeys batchAccountKeys = batchManager.batchAccounts()
                    .getKeysWithResponse(rgName, batchAccountName, Context.NONE)
                    .getValue();

            Utils.print(batchAccountKeys);

            System.out.println("Regenerating primary batch account primary access key");

            batchAccountKeys = batchManager.batchAccounts()
                    .regenerateKeyWithResponse(rgName, batchAccountName,
                            new BatchAccountRegenerateKeyParameters().withKeyName(AccountKeyType.PRIMARY),
                            Context.NONE)
                    .getValue();

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

            batchManager.batchAccounts()
                    .synchronizeAutoStorageKeysWithResponse(rgName, batchAccountName, Context.NONE);

            // ============================================================
            // Update name of application.
            batchManager.applications()
                    .getWithResponse(rgName, batchAccountName, applicationName, Context.NONE)
                    .getValue()
                    .update()
                    .withDisplayName("New application display name")
                    .apply();

            batchAccount = batchManager.batchAccounts()
                    .getByResourceGroupWithResponse(rgName, batchAccountName, Context.NONE)
                    .getValue();
            Utils.print(batchAccount, batchManager);

            // ============================================================
            // Create another batch account

            System.out.println("Creating another Batch Account");

            allowedNumberOfBatchAccounts = batchManager.locations()
                    .getQuotasWithResponse(region2.toString(), Context.NONE)
                    .getValue()
                    .accountQuota();

            // ===========================================================
            // List all the batch accounts in subscription.

            batchAccounts = batchManager.batchAccounts().list(Context.NONE);
            batchAccountsAtSpecificRegion = 0;
            for (BatchAccount batch : batchAccounts) {
                if (batch.region().equals(region2)) {
                    batchAccountsAtSpecificRegion++;
                }
            }

            BatchAccount batchAccount2 = null;
            if (batchAccountsAtSpecificRegion < allowedNumberOfBatchAccounts) {
                batchAccount2 = batchManager.batchAccounts().define(batchAccountName2)
                        .withRegion(region2)
                        .withExistingResourceGroup(rgName)
                        .withAutoStorage(new AutoStorageBaseProperties().withStorageAccountId(storageAccount.id()))
                        .create();

                System.out.println("Created second Batch Account:");
                Utils.print(batchAccount2, batchManager);
            }

            // ============================================================
            // List batch accounts

            System.out.println("Listing Batch accounts");

            Iterable<BatchAccount> accounts = batchManager.batchAccounts().listByResourceGroup(rgName, Context.NONE);
            int i = 0;
            for (BatchAccount ba : accounts) {
                System.out.println("Batch Account (" + i + ") " + ba.name());
                i++;
            }

            // ============================================================
            // Refresh a batch account.
            batchAccount = batchManager.batchAccounts()
                    .getByResourceGroupWithResponse(rgName, batchAccountName, Context.NONE)
                    .getValue();
            Utils.print(batchAccount, batchManager);

            // ============================================================
            // Delete a batch account

            System.out.println("Deleting a batch account - " + batchAccount.name());

            for (ApplicationPackage applicationPackage : batchManager.applicationPackages()
                    .list(rgName, batchAccountName, applicationName, null, Context.NONE)) {
                System.out.println("Deleting a application package - " + applicationPackage.name());
                batchManager.applicationPackages()
                        .deleteWithResponse(rgName, batchAccountName, applicationName,
                                applicationPackage.name(), Context.NONE);
            }
            for (Application application : batchManager.applications()
                    .list(rgName, batchAccountName, null, Context.NONE)) {
                System.out.println("Deleting a application - " + application.name());
                batchManager.applications()
                        .deleteWithResponse(rgName, batchAccountName, application.name(), Context.NONE);
            }

            batchManager.batchAccounts().delete(rgName, batchAccountName, Context.NONE);

            System.out.println("Deleted batch account");

            if (batchAccount2 != null) {
                System.out.println("Deleting second batch account - " + batchAccount2.name());
                batchManager.batchAccounts().delete(rgName, batchAccountName2, Context.NONE);
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
            }
            catch (Exception e) {
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
            // TODO: The legacy file-based authentication via AZURE_AUTH_LOCATION is no longer
            // supported in the Track 2 Azure SDK. Replace DefaultAzureCredential with your
            // preferred TokenCredential implementation (e.g., ClientSecretCredential,
            // ManagedIdentityCredential, InteractiveBrowserCredential, etc.).
            // See: https://aka.ms/java-track2-migration-guide and
            //      https://github.com/Azure/azure-sdk-for-java/blob/main/sdk/resourcemanager/docs/AUTH.md
            TokenCredential credential = new DefaultAzureCredentialBuilder().build();
            AzureProfile profile = new AzureProfile(AzureEnvironment.AZURE);

            AzureResourceManager azure = AzureResourceManager
                    .authenticate(credential, profile)
                    .withDefaultSubscription();

            BatchManager batchManager = BatchManager
                    .authenticate(credential,
                            new AzureProfile(null, azure.subscriptionId(), AzureEnvironment.AZURE));

            // Print selected subscription
            System.out.println("Selected subscription: " + azure.subscriptionId());

            runSample(azure, batchManager);
        } catch (Exception e) {
            System.out.println(e.getMessage());
            e.printStackTrace();
        }
    }

    private ManageBatchAccount() {
    }
}
