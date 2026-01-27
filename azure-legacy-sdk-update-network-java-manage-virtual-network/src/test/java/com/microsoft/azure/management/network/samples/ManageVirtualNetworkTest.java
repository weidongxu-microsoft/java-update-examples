package com.microsoft.azure.management.network.samples;

import com.microsoft.azure.management.compute.KnownLinuxVirtualMachineImage;
import com.microsoft.azure.management.compute.VirtualMachine;
import com.microsoft.azure.management.compute.VirtualMachineSizeTypes;
import com.microsoft.azure.management.compute.VirtualMachines;
import com.microsoft.azure.management.network.Network;
import com.microsoft.azure.management.resources.fluentcore.arm.Region;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import static org.junit.Assert.assertSame;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class ManageVirtualNetworkTest {

    private static final Region REGION = Region.US_EAST;
    private static final String RESOURCE_GROUP = "rg-test";
    private static final String VM_NAME = "vm-test";
    private static final String SUBNET = "subnet";
    private static final String USER = "user";
    private static final String SSH_KEY = "ssh-key";
    private static final String DNS_LABEL = "dns-label";

    @Mock
    private VirtualMachines virtualMachines;
    @Mock
    private Network network;
    @Mock
    private VirtualMachine virtualMachine;
    @Mock
    private VirtualMachine.DefinitionStages.Blank blankStage;
    @Mock
    private VirtualMachine.DefinitionStages.WithGroup withGroupStage;
    @Mock
    private VirtualMachine.DefinitionStages.WithNetwork withNetworkStage;
    @Mock
    private VirtualMachine.DefinitionStages.WithSubnet withSubnetStage;
    @Mock
    private VirtualMachine.DefinitionStages.WithPrivateIP withPrivateIPStage;
    @Mock
    private VirtualMachine.DefinitionStages.WithPublicIPAddress withPublicIPAddressStage;
    @Mock
    private VirtualMachine.DefinitionStages.WithProximityPlacementGroup withProximityPlacementGroupStage;
    @Mock
    private VirtualMachine.DefinitionStages.WithLinuxRootUsernameManagedOrUnmanaged withLinuxRootUsernameStage;
    @Mock
    private VirtualMachine.DefinitionStages.WithLinuxRootPasswordOrPublicKeyManagedOrUnmanaged withLinuxCredentialsStage;
    @Mock
    private VirtualMachine.DefinitionStages.WithLinuxCreateManagedOrUnmanaged withLinuxCreateStage;
    @Mock
    private VirtualMachine.DefinitionStages.WithManagedCreate withManagedCreateStage;

    @Before
    public void setUp() {
        when(virtualMachines.define(VM_NAME)).thenReturn(blankStage);
        when(blankStage.withRegion(REGION)).thenReturn(withGroupStage);
        when(withGroupStage.withExistingResourceGroup(RESOURCE_GROUP)).thenReturn(withNetworkStage);
        when(withNetworkStage.withExistingPrimaryNetwork(network)).thenReturn(withSubnetStage);
        when(withSubnetStage.withSubnet(SUBNET)).thenReturn(withPrivateIPStage);
        when(withPrivateIPStage.withPrimaryPrivateIPAddressDynamic()).thenReturn(withPublicIPAddressStage);
        when(withPublicIPAddressStage.withNewPrimaryPublicIPAddress(DNS_LABEL)).thenReturn(withProximityPlacementGroupStage);
        when(withPublicIPAddressStage.withoutPrimaryPublicIPAddress()).thenReturn(withProximityPlacementGroupStage);
        when(withProximityPlacementGroupStage.withPopularLinuxImage(KnownLinuxVirtualMachineImage.UBUNTU_SERVER_16_04_LTS))
            .thenReturn(withLinuxRootUsernameStage);
        when(withLinuxRootUsernameStage.withRootUsername(USER)).thenReturn(withLinuxCredentialsStage);
        when(withLinuxCredentialsStage.withSsh(SSH_KEY)).thenReturn(withLinuxCreateStage);
        when(withLinuxCreateStage.withSize(VirtualMachineSizeTypes.STANDARD_D3_V2)).thenReturn(withManagedCreateStage);
        when(withManagedCreateStage.create()).thenReturn(virtualMachine);
    }

    @Test
    public void createLinuxVirtualMachine_withPublicIp_buildsExpectedPipeline() {
        VirtualMachine result = ManageVirtualNetwork.createLinuxVirtualMachine(
            virtualMachines,
            REGION,
            RESOURCE_GROUP,
            VM_NAME,
            network,
            SUBNET,
            USER,
            SSH_KEY,
            true,
            DNS_LABEL);

        assertSame(virtualMachine, result);
        verify(withPublicIPAddressStage).withNewPrimaryPublicIPAddress(DNS_LABEL);
        verify(withLinuxCreateStage).withSize(VirtualMachineSizeTypes.STANDARD_D3_V2);
        verify(withManagedCreateStage).create();
    }

    @Test
    public void createLinuxVirtualMachine_withoutPublicIp_skipsPublicIpAllocation() {
        ManageVirtualNetwork.createLinuxVirtualMachine(
            virtualMachines,
            REGION,
            RESOURCE_GROUP,
            VM_NAME,
            network,
            SUBNET,
            USER,
            SSH_KEY,
            false,
            null);

        verify(withPublicIPAddressStage).withoutPrimaryPublicIPAddress();
        verify(withPublicIPAddressStage, never()).withNewPrimaryPublicIPAddress(anyString());
    }
}
