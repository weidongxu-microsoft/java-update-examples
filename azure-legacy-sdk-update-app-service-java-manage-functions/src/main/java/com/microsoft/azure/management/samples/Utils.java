package com.microsoft.azure.management.samples;

import com.microsoft.azure.management.appservice.AppSetting;
import com.microsoft.azure.management.appservice.ConnectionString;
import com.microsoft.azure.management.appservice.FunctionApp;
import com.microsoft.azure.management.appservice.HostNameBinding;
import com.microsoft.azure.management.appservice.HostNameSslState;
import com.microsoft.azure.management.appservice.SslState;

import java.util.Map;

/**
 * Lightweight helpers that keep this sample focused on Function App behavior.
 */
public final class Utils {

    private Utils() {
    }

    /**
     * Print the most relevant properties for a function app in a predictable format.
     *
     * @param functionApp the function app to describe
     */
    public static void print(FunctionApp functionApp) {
        if (functionApp == null) {
            System.out.println("Function app: <null>");
            return;
        }

        StringBuilder builder = new StringBuilder()
                .append("Function app: ").append(functionApp.id())
                .append("\n\tName: ").append(functionApp.name())
                .append("\n\tState: ").append(functionApp.state())
                .append("\n\tResource group: ").append(functionApp.resourceGroupName())
                .append("\n\tRegion: ").append(functionApp.region())
                .append("\n\tDefault hostname: ").append(functionApp.defaultHostName())
                .append("\n\tApp service plan: ").append(functionApp.appServicePlanId());

        appendHostNames(builder, functionApp);
        appendSslBindings(builder, functionApp);
        appendAppSettings(builder, functionApp);
        appendConnectionStrings(builder, functionApp);

        System.out.println(builder.toString());
    }

    private static void appendHostNames(StringBuilder builder, FunctionApp functionApp) {
        Map<String, HostNameBinding> hostNames = functionApp.getHostNameBindings();
        builder.append("\n\tHost name bindings:");
        if (hostNames == null || hostNames.isEmpty()) {
            builder.append(" <none>");
            return;
        }
        for (HostNameBinding binding : hostNames.values()) {
            builder.append("\n\t\t").append(binding);
        }
    }

    private static void appendSslBindings(StringBuilder builder, FunctionApp functionApp) {
        Map<String, HostNameSslState> sslStates = functionApp.hostNameSslStates();
        builder.append("\n\tSSL bindings:");
        if (sslStates == null || sslStates.isEmpty()) {
            builder.append(" <none>");
            return;
        }
        for (HostNameSslState sslState : sslStates.values()) {
            builder.append("\n\t\t")
                    .append(sslState.name())
                    .append(": ")
                    .append(sslState.sslState());
            SslState state = sslState.sslState();
            if (state != null && state != SslState.DISABLED) {
                builder.append(" - ").append(sslState.thumbprint());
            }
        }
    }

    private static void appendAppSettings(StringBuilder builder, FunctionApp functionApp) {
        Map<String, AppSetting> settings = functionApp.getAppSettings();
        builder.append("\n\tApp settings:");
        if (settings == null || settings.isEmpty()) {
            builder.append(" <none>");
            return;
        }
        for (AppSetting setting : settings.values()) {
            builder.append("\n\t\t")
                    .append(setting.key())
                    .append(": ")
                    .append(setting.value());
            if (setting.sticky()) {
                builder.append(" - slot setting");
            }
        }
    }

    private static void appendConnectionStrings(StringBuilder builder, FunctionApp functionApp) {
        Map<String, ConnectionString> connections = functionApp.getConnectionStrings();
        builder.append("\n\tConnection strings:");
        if (connections == null || connections.isEmpty()) {
            builder.append(" <none>");
            return;
        }
        for (ConnectionString connection : connections.values()) {
            builder.append("\n\t\t")
                    .append(connection.name())
                    .append(": ")
                    .append(connection.value())
                    .append(" - ")
                    .append(connection.type());
            if (connection.sticky()) {
                builder.append(" - slot setting");
            }
        }
    }
}
