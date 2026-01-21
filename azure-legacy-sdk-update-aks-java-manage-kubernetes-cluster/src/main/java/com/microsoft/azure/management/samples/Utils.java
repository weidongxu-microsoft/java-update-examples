/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */

package com.microsoft.azure.management.samples;

import com.microsoft.azure.management.containerservice.KubernetesCluster;

import java.io.File;
import java.io.FileInputStream;
import java.util.ArrayList;
import java.util.Properties;

/**
 * Common utils for Azure management samples.
 */

public final class Utils {

    /**
     * Print a Kubernetes cluster.
     * @param kubernetesCluster a Kubernetes cluster
     */
    public static void print(KubernetesCluster kubernetesCluster) {
        StringBuilder info = new StringBuilder();

        info.append("Azure Container Service: ").append(kubernetesCluster.id())
                .append("\n\tName: ").append(kubernetesCluster.name())
                .append("\n\tFQDN: ").append(kubernetesCluster.fqdn())
                .append("\n\tDNS prefix label: ").append(kubernetesCluster.dnsPrefix())
                .append("\n\t\tWith Agent pool name: ").append(new ArrayList<>(kubernetesCluster.agentPools().keySet()).get(0))
                .append("\n\t\tAgent pool count: ").append(new ArrayList<>(kubernetesCluster.agentPools().values()).get(0).count())
                .append("\n\t\tAgent pool VM size: ").append(new ArrayList<>(kubernetesCluster.agentPools().values()).get(0).vmSize().toString())
                .append("\n\tLinux user name: ").append(kubernetesCluster.linuxRootUsername())
                .append("\n\tSSH key: ").append(kubernetesCluster.sshKey())
                .append("\n\tService principal client ID: ").append(kubernetesCluster.servicePrincipalClientId());

        System.out.println(info.toString());
    }

    /**
     * Retrieve the secondary service principal client ID.
     * @param envSecondaryServicePrincipal an Azure Container Registry
     * @return a service principal client ID
     * @throws Exception exception
     */
    public static String getSecondaryServicePrincipalClientID(String envSecondaryServicePrincipal) throws Exception {
        Properties authSettings = new Properties();
        FileInputStream credentialsFileStream = new FileInputStream(new File(envSecondaryServicePrincipal));
        authSettings.load(credentialsFileStream);
        credentialsFileStream.close();

        return authSettings.getProperty("client");
    }

    /**
     * Retrieve the secondary service principal secret.
     * @param envSecondaryServicePrincipal an Azure Container Registry
     * @return a service principal secret
     * @throws Exception exception
     */
    public static String getSecondaryServicePrincipalSecret(String envSecondaryServicePrincipal) throws Exception {
        Properties authSettings = new Properties();
        FileInputStream credentialsFileStream = new FileInputStream(new File(envSecondaryServicePrincipal));
        authSettings.load(credentialsFileStream);
        credentialsFileStream.close();

        return authSettings.getProperty("key");
    }
}
