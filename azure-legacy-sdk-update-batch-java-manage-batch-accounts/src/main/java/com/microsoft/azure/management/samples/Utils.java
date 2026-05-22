/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */

package com.microsoft.azure.management.samples;

import com.azure.resourcemanager.batch.models.BatchAccount;
import com.azure.resourcemanager.batch.models.BatchAccountKeys;
import com.azure.resourcemanager.storage.models.StorageAccountKey;

import java.util.List;

/**
 * Minimal helpers shared across the batch samples.
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
        System.out.println(new StringBuilder().append("BatchAccount: ").append(batchAccount.id())
                .append("Name: ").append(batchAccount.name())
                .append("\n\tResource group: ").append(batchAccount.resourceGroupName())
                .append("\n\tRegion: ").append(batchAccount.location())
                .append("\n\tTags: ").append(batchAccount.tags())
                .append("\n\tAccountEndpoint: ").append(batchAccount.accountEndpoint())
                .append("\n\tPoolQuota: ").append(batchAccount.poolQuota())
                .append("\n\tActiveJobAndJobScheduleQuota: ").append(batchAccount.activeJobAndJobScheduleQuota())
                .append("\n\tStorageAccount: ").append(
                        batchAccount.autoStorage() == null
                                ? "No storage account attached"
                                : batchAccount.autoStorage().storageAccountId())
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
