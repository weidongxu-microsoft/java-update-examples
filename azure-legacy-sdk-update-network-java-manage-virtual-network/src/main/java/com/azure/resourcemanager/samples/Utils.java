// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.azure.resourcemanager.samples;

import com.azure.resourcemanager.AzureResourceManager;
import com.azure.resourcemanager.compute.models.VirtualMachine;
import com.azure.resourcemanager.network.models.Network;
import com.azure.resourcemanager.network.models.NetworkSecurityGroup;

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

    /**
     * Print network security group info.
     * @param resource a network security group
     */
    public static void print(NetworkSecurityGroup resource) {
        System.out.println("Network Security Group: " + resource.id()
                + "\n\tName: " + resource.name()
                + "\n\tResource group: " + resource.resourceGroupName()
                + "\n\tRegion: " + resource.region()
                + "\n\tTags: " + resource.tags());
    }

    /**
     * Print virtual network info.
     * @param resource a virtual network
     */
    public static void print(Network resource) {
        System.out.println("Network: " + resource.id()
                + "\n\tName: " + resource.name()
                + "\n\tResource group: " + resource.resourceGroupName()
                + "\n\tRegion: " + resource.region()
                + "\n\tTags: " + resource.tags()
                + "\n\tAddress spaces: " + resource.addressSpaces()
                + "\n\tDNS server IPs: " + resource.dnsServerIPs());
    }

    /**
     * Print virtual machine info.
     * @param resource a virtual machine
     */
    public static void print(VirtualMachine resource) {
        System.out.println("Virtual Machine: " + resource.id()
                + "\n\tName: " + resource.name()
                + "\n\tResource group: " + resource.resourceGroupName()
                + "\n\tRegion: " + resource.region()
                + "\n\tTags: " + resource.tags()
                + "\n\tSize: " + resource.size());
    }

    private Utils() {
    }
}
