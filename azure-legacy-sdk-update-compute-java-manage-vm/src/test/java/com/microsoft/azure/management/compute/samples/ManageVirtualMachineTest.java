package com.microsoft.azure.management.compute.samples;

import com.microsoft.azure.management.compute.Disk;
import com.microsoft.azure.management.compute.KnownWindowsVirtualMachineImage;
import com.microsoft.azure.management.compute.VirtualMachine;
import com.microsoft.azure.management.compute.VirtualMachineSizeTypes;
import com.microsoft.azure.management.compute.VirtualMachines;
import com.microsoft.azure.management.resources.fluentcore.arm.Region;
import com.microsoft.azure.management.resources.fluentcore.model.Creatable;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import static org.junit.Assert.assertSame;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class ManageVirtualMachineTest {

    private static final Region REGION = Region.US_WEST_CENTRAL;
    private static final String RESOURCE_GROUP = "rg-test";
    private static final String VM_NAME = "vm-test";
    private static final String USER = "user";
    private static final String PASSWORD = "P@ssword!23";

    @Mock
    private VirtualMachines virtualMachines;
    @Mock
    private Creatable<Disk> dataDiskCreatable;
    @Mock
    private Disk dataDisk;
    @Mock
    private VirtualMachine virtualMachine;
    @Mock
    private VirtualMachine.DefinitionStages.Blank blankStage;
    @Mock
    private VirtualMachine.DefinitionStages.WithGroup withGroupStage;
    @Mock
    private VirtualMachine.DefinitionStages.WithNetwork withNetworkStage;
    @Mock
    private VirtualMachine.DefinitionStages.WithPrivateIP withPrivateIPStage;
    @Mock
    private VirtualMachine.DefinitionStages.WithPublicIPAddress withPublicIPAddressStage;
    @Mock
    private VirtualMachine.DefinitionStages.WithProximityPlacementGroup withProximityPlacementGroupStage;
    @Mock
    private VirtualMachine.DefinitionStages.WithWindowsAdminUsernameManagedOrUnmanaged withWindowsAdminUsernameStage;
    @Mock
    private VirtualMachine.DefinitionStages.WithWindowsAdminPasswordManagedOrUnmanaged withWindowsAdminPasswordStage;
    @Mock
    private VirtualMachine.DefinitionStages.WithWindowsCreateManagedOrUnmanaged withWindowsCreateStage;
    @Mock
    private VirtualMachine.DefinitionStages.WithManagedCreate withManagedCreateStage;

    @Before
    public void setUp() {
        when(virtualMachines.define(VM_NAME)).thenReturn(blankStage);
        when(blankStage.withRegion(REGION)).thenReturn(withGroupStage);
        when(withGroupStage.withNewResourceGroup(RESOURCE_GROUP)).thenReturn(withNetworkStage);
        when(withNetworkStage.withNewPrimaryNetwork("10.0.0.0/28")).thenReturn(withPrivateIPStage);
        when(withPrivateIPStage.withPrimaryPrivateIPAddressDynamic()).thenReturn(withPublicIPAddressStage);
        when(withPublicIPAddressStage.withoutPrimaryPublicIPAddress()).thenReturn(withProximityPlacementGroupStage);
        when(withProximityPlacementGroupStage.withPopularWindowsImage(KnownWindowsVirtualMachineImage.WINDOWS_SERVER_2012_R2_DATACENTER))
            .thenReturn(withWindowsAdminUsernameStage);
        when(withWindowsAdminUsernameStage.withAdminUsername(USER)).thenReturn(withWindowsAdminPasswordStage);
        when(withWindowsAdminPasswordStage.withAdminPassword(PASSWORD)).thenReturn(withWindowsCreateStage);
        when(withWindowsCreateStage.withNewDataDisk(10)).thenReturn(withManagedCreateStage);
        when(withManagedCreateStage.withNewDataDisk(dataDiskCreatable)).thenReturn(withManagedCreateStage);
        when(withManagedCreateStage.withExistingDataDisk(dataDisk)).thenReturn(withManagedCreateStage);
        when(withManagedCreateStage.withSize(VirtualMachineSizeTypes.STANDARD_D3_V2)).thenReturn(withManagedCreateStage);
        when(withManagedCreateStage.create()).thenReturn(virtualMachine);
    }

    @Test
    public void createWindowsVirtualMachine_configuresDisksAndReturnsVm() {
        VirtualMachine result = ManageVirtualMachine.createWindowsVirtualMachine(
            virtualMachines,
                REGION,
                RESOURCE_GROUP,
                VM_NAME,
                USER,
                PASSWORD,
                dataDiskCreatable,
                dataDisk);

        assertSame(virtualMachine, result);
        verify(withWindowsCreateStage).withNewDataDisk(10);
        verify(withManagedCreateStage).withNewDataDisk(dataDiskCreatable);
        verify(withManagedCreateStage).withExistingDataDisk(dataDisk);
        verify(withManagedCreateStage).withSize(VirtualMachineSizeTypes.STANDARD_D3_V2);
        verify(withManagedCreateStage).create();
    }

    @Test
    public void createWindowsVirtualMachine_setsUpNetworkAndCredentials() {
        ManageVirtualMachine.createWindowsVirtualMachine(
            virtualMachines,
                REGION,
                RESOURCE_GROUP,
                VM_NAME,
                USER,
                PASSWORD,
                dataDiskCreatable,
                dataDisk);

        verify(blankStage).withRegion(REGION);
        verify(withGroupStage).withNewResourceGroup(RESOURCE_GROUP);
        verify(withNetworkStage).withNewPrimaryNetwork("10.0.0.0/28");
        verify(withPrivateIPStage).withPrimaryPrivateIPAddressDynamic();
        verify(withPublicIPAddressStage).withoutPrimaryPublicIPAddress();
        verify(withProximityPlacementGroupStage).withPopularWindowsImage(KnownWindowsVirtualMachineImage.WINDOWS_SERVER_2012_R2_DATACENTER);
        verify(withWindowsAdminUsernameStage).withAdminUsername(USER);
        verify(withWindowsAdminPasswordStage).withAdminPassword(PASSWORD);
    }
}
