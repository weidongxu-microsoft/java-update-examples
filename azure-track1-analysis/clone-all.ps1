# Clone all repositories in parallel
 = @(
    @{name="hadoop"; url="https://github.com/apache/hadoop.git"; module="hadoop/hadoop-tools/hadoop-azure"},
    @{name="hbase"; url="https://github.com/apache/hbase.git"; module="hbase"},
    @{name="delta"; url="https://github.com/delta-io/delta.git"; module="core"},
    @{name="eventhubs-spark"; url="https://github.com/Azure/azure-event-hubs-spark.git"; module="."},
    @{name="kafka-connect"; url="https://github.com/Azure/kafka-connect-eventhubs.git"; module="."},
    @{name="azure-sdk"; url="https://github.com/Azure/azure-sdk-for-java.git"; module="sdk/spring"},
    @{name="camel"; url="https://github.com/apache/camel.git"; module="components/camel-azure"}
)

foreach ( in ) {
    Write-Host "Cloning ..." -ForegroundColor Green
    & git clone --depth 1 "" "" 2>&1 | Select-String -Pattern "Cloning|done"
}
