/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */

package com.microsoft.azure.management.samples;

import com.microsoft.azure.management.batch.Application;
import com.microsoft.azure.management.batch.ApplicationPackage;
import com.microsoft.azure.management.batch.BatchAccount;
import com.microsoft.azure.management.batch.BatchAccountKeys;
import com.microsoft.azure.management.storage.StorageAccountKey;

import java.util.List;
import java.util.Map;

/**
 * Minimal helpers shared across the legacy batch samples.
 */
public final class Utils {

    private Utils() {
    }

    /**
     * Prints batch account keys.
     *
     * @param batchAccountKeys batch account keys
     */
    public static void print(BatchAccountKeys batchAccountKeys) {
        System.out.println("Primary Key (" + batchAccountKeys.primary() + ") Secondary key = ("
                + batchAccountKeys.secondary() + ")");
    }

    /**
     * Prints batch account.
     *
     * @param batchAccount a Batch Account
     */
    public static void print(BatchAccount batchAccount) {
        StringBuilder applicationsOutput = new StringBuilder().append("\n\tapplications: ");

        if (batchAccount.applications().size() > 0) {
            for (Map.Entry<String, Application> applicationEntry : batchAccount.applications().entrySet()) {
                Application application = applicationEntry.getValue();
                StringBuilder applicationPackages = new StringBuilder().append("\n\t\t\tapplicationPackages : ");

                for (Map.Entry<String, ApplicationPackage> applicationPackageEntry : application.applicationPackages().entrySet()) {
                    ApplicationPackage applicationPackage = applicationPackageEntry.getValue();
                    StringBuilder singleApplicationPackage = new StringBuilder().append("\n\t\t\t\tapplicationPackage : " + applicationPackage.name());
                    singleApplicationPackage.append("\n\t\t\t\tapplicationPackageState : " + applicationPackage.state());

                    applicationPackages.append(singleApplicationPackage);
                    singleApplicationPackage.append("\n");
                }

                StringBuilder singleApplication = new StringBuilder().append("\n\t\tapplication: " + application.name());
                singleApplication.append("\n\t\tdisplayName: " + application.displayName());
                singleApplication.append("\n\t\tdefaultVersion: " + application.defaultVersion());
                singleApplication.append(applicationPackages);
                applicationsOutput.append(singleApplication);
                applicationsOutput.append("\n");
            }
        }

        System.out.println(new StringBuilder().append("BatchAccount: ").append(batchAccount.id())
                .append("Name: ").append(batchAccount.name())
                .append("\n\tResource group: ").append(batchAccount.resourceGroupName())
                .append("\n\tRegion: ").append(batchAccount.region())
                .append("\n\tTags: ").append(batchAccount.tags())
                .append("\n\tAccountEndpoint: ").append(batchAccount.accountEndpoint())
                .append("\n\tPoolQuota: ").append(batchAccount.poolQuota())
                .append("\n\tActiveJobAndJobScheduleQuota: ").append(batchAccount.activeJobAndJobScheduleQuota())
                .append("\n\tStorageAccount: ").append(batchAccount.autoStorage() == null ? "No storage account attached" : batchAccount.autoStorage().storageAccountId())
                .append(applicationsOutput)
                .toString());
    }

    /**
     * Prints all storage account keys.
     *
     * @param storageAccountKeys list of keys returned by Azure
     */
    public static void print(List<StorageAccountKey> storageAccountKeys) {
        for (int i = 0; i < storageAccountKeys.size(); i++) {
            StorageAccountKey storageAccountKey = storageAccountKeys.get(i);
            System.out.println("Key (" + i + ") " + storageAccountKey.keyName() + "="
                    + storageAccountKey.value());
        }
    }
}
