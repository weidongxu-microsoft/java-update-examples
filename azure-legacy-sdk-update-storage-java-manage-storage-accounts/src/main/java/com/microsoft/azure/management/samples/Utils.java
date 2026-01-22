/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */

package com.microsoft.azure.management.samples;

import com.microsoft.azure.management.storage.StorageAccount;
import com.microsoft.azure.management.storage.StorageAccountKey;

import java.util.List;

/**
 * Minimal helpers shared across the legacy storage samples.
 */
public final class Utils {

    private Utils() {
    }

    /**
     * Prints details about the provided storage account.
     *
     * @param storageAccount storage account to describe
     */
    public static void print(StorageAccount storageAccount) {
        if (storageAccount == null) {
            System.out.println("Storage account is null.");
            return;
        }

        StringBuilder builder = new StringBuilder("Storage Account: ")
            .append(storageAccount.id())
            .append("\n\tName: ").append(storageAccount.name())
            .append("\n\tResource group: ").append(storageAccount.resourceGroupName())
            .append("\n\tRegion: ").append(storageAccount.regionName())
            .append("\n\tSKU: ").append(storageAccount.skuType())
            .append("\n\tKind: ").append(storageAccount.kind())
            .append("\n\tCreation time: ").append(storageAccount.creationTime());

        System.out.println(builder.toString());
    }

    /**
     * Prints all storage account keys with their permissions.
     *
     * @param storageAccountKeys list of keys returned by Azure
     */
    public static void print(List<StorageAccountKey> storageAccountKeys) {
        if (storageAccountKeys == null || storageAccountKeys.isEmpty()) {
            System.out.println("No storage account keys returned.");
            return;
        }

        for (int i = 0; i < storageAccountKeys.size(); i++) {
            StorageAccountKey key = storageAccountKeys.get(i);
            System.out.println(new StringBuilder("Key ").append(i)
                .append(':')
                .append("\n\tName: ").append(key.keyName())
                .append("\n\tPermissions: ").append(key.permissions())
                .append("\n\tValue: ").append(key.value())
                .toString());
        }
    }
}
