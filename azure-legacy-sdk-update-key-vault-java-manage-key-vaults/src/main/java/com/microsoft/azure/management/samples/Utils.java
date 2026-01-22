/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */

package com.microsoft.azure.management.samples;

import com.google.common.base.Joiner;
import com.microsoft.azure.management.keyvault.AccessPolicy;
import com.microsoft.azure.management.keyvault.Vault;

/**
 * Common utils for Azure management samples.
 */

public final class Utils {

    /**
     * Print key vault.
     *
     * @param vault a key vault
     */
    public static void print(Vault vault) {
        StringBuilder info = new StringBuilder().append("Key Vault: ").append(vault.id())
                .append("Name: ").append(vault.name())
                .append("\n\tResource group: ").append(vault.resourceGroupName())
                .append("\n\tRegion: ").append(vault.region())
                .append("\n\tSku: ").append(vault.sku().name()).append(" - ").append(vault.sku().family())
                .append("\n\tVault URI: ").append(vault.vaultUri())
                .append("\n\tAccess policies: ");
        for (AccessPolicy accessPolicy : vault.accessPolicies()) {
            info.append("\n\t\tIdentity:").append(accessPolicy.objectId())
                    .append("\n\t\tKey permissions: ").append(Joiner.on(", ").join(accessPolicy.permissions().keys()))
                    .append("\n\t\tSecret permissions: ").append(Joiner.on(", ").join(accessPolicy.permissions().secrets()));
        }
        System.out.println(info.toString());
    }

    private Utils() {
    }
}
