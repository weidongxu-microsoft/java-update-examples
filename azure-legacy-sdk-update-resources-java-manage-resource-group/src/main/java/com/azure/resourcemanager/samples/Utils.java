// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.azure.resourcemanager.samples;

import com.azure.resourcemanager.AzureResourceManager;

/**
 * Common utils for Azure management samples.
 */
public final class Utils {

    /**
     * Generate random resource name.
     * @param azureResourceManager the AzureResourceManager
     * @param prefix the prefix
     * @param maxLen the maximum length
     * @return the random resource name
     */
    public static String randomResourceName(AzureResourceManager azureResourceManager, String prefix, int maxLen) {
        return azureResourceManager.resourceManager().internalContext().randomResourceName(prefix, maxLen);
    }

    private Utils() {
    }
}
