package com.microsoft.azure.management.postgresql.samples;

import com.microsoft.azure.management.postgresql.v2017_12_01.Server;
import com.microsoft.azure.management.postgresql.v2017_12_01.ServerPropertiesForCreate;
import com.microsoft.azure.management.postgresql.v2017_12_01.ServerPropertiesForDefaultCreate;
import com.microsoft.azure.management.postgresql.v2017_12_01.Sku;
import com.microsoft.azure.management.postgresql.v2017_12_01.Servers;
import com.microsoft.azure.management.resources.fluentcore.arm.Region;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import static org.junit.Assert.assertSame;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class ManagePostgreSqlServerTest {

    private static final Region REGION = Region.US_EAST;
    private static final String RESOURCE_GROUP = "rg-test";
    private static final String SERVER_NAME = "pg-test";

    private ServerPropertiesForCreate createProperties;
    private Sku sku;

    @Mock
    private Servers servers;
    @Mock
    private Server server;
    @Mock
    private Server.DefinitionStages.Blank blankStage;
    @Mock
    private Server.DefinitionStages.WithGroup withGroupStage;
    @Mock
    private Server.DefinitionStages.WithProperties withPropertiesStage;
    @Mock
    private Server.DefinitionStages.WithCreate withCreateStage;

    @Before
    public void setUp() {
        createProperties = new ServerPropertiesForDefaultCreate();
        sku = new Sku().withName("GP_Gen5_2");

        when(servers.define(SERVER_NAME)).thenReturn(blankStage);
        when(blankStage.withRegion(REGION.name())).thenReturn(withGroupStage);
        when(withGroupStage.withExistingResourceGroup(RESOURCE_GROUP)).thenReturn(withPropertiesStage);
        when(withPropertiesStage.withProperties(createProperties)).thenReturn(withCreateStage);
        when(withCreateStage.withSku(sku)).thenReturn(withCreateStage);
        when(withCreateStage.withTag("sample", "postgresql")).thenReturn(withCreateStage);
        when(withCreateStage.withTag("language", "java")).thenReturn(withCreateStage);
        when(withCreateStage.create()).thenReturn(server);
    }

    @Test
    public void createServer_buildsServerWithExpectedFluentCalls() {
        Server result = ManagePostgreSqlServer.createServer(
            servers,
            REGION,
            RESOURCE_GROUP,
            SERVER_NAME,
            createProperties,
            sku);

        assertSame(server, result);
        verify(servers).define(SERVER_NAME);
        verify(blankStage).withRegion(REGION.name());
        verify(withGroupStage).withExistingResourceGroup(RESOURCE_GROUP);
        verify(withPropertiesStage).withProperties(createProperties);
        verify(withCreateStage).withSku(sku);
        verify(withCreateStage).withTag("sample", "postgresql");
        verify(withCreateStage).withTag("language", "java");
        verify(withCreateStage).create();
    }
}
