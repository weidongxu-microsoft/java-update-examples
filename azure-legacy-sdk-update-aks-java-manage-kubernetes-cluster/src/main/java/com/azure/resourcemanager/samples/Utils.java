// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.azure.resourcemanager.samples;

import com.azure.resourcemanager.AzureResourceManager;
import com.azure.resourcemanager.containerservice.models.KubernetesCluster;

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
        return azureResourceManager.resourceGroups().manager().internalContext().randomResourceName(prefix, maxLen);
    }

    /**
     * Print Kubernetes cluster info.
     * @param resource a Kubernetes cluster
     */
    public static void print(KubernetesCluster resource) {
        System.out.println("Kubernetes Cluster: " + resource.id()
                + "\n\tName: " + resource.name()
                + "\n\tResource group: " + resource.resourceGroupName()
                + "\n\tRegion: " + resource.region()
                + "\n\tTags: " + resource.tags()
                + "\n\tKubernetes version: " + resource.version()
                + "\n\tFQDN: " + resource.fqdn());
    }

    private Utils() {
    }
}
