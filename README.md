# azure-legacy-sdk-update-examples

- **azure-legacy-sdk-update-aks-java-manage-kubernetes-cluster** – AKS cluster lifecycle sample (create/update).
- **azure-legacy-sdk-update-batch-java-manage-batch-accounts** – Batch account management sample.
- **azure-legacy-sdk-update-app-service-java-manage-functions** – Function App provisioning and management.
- **azure-legacy-sdk-update-azure-client-initialization** – Shared snippets for initializing Azure clients.
- **azure-legacy-sdk-update-data-transfer-project** – Data Transfer Project sample showcasing cross-service portability with Azure Key Vault and storage integrations. From https://github.com/dtinit/data-transfer-project
- **azure-legacy-sdk-update-compute-java-manage-vm** – Virtual machine creation and management walkthrough.
- **azure-legacy-sdk-update-compute-java-create-vms-async-tracking-related-resources** – Parallel VM provisioning sample with asynchronous tracking and cleanup of related resources.
- **azure-legacy-sdk-update-documentdb-java-todo-app** – Java EE todo list sample backed by Azure Cosmos DB (DocumentDB).
- **azure-legacy-sdk-update-eventhubs-v3** – Event Hubs v3 SDK (azure-eventhubs and azure-eventhubs-eph) for sending and receiving events with Event Processor Host.
- **azure-legacy-sdk-update-key-vault-java-manage-key-vaults** – Key Vault configuration and access policy sample.
- **azure-legacy-sdk-update-servicebus-v3** – Service Bus v3 SDK (azure-servicebus) for sending and receiving queue messages with async CompletableFuture patterns.
- **azure-legacy-sdk-update-keyvault-v1** – Key Vault v1 SDK (azure-keyvault) for managing keys, secrets and certificates.
- **azure-legacy-sdk-update-network-java-manage-virtual-network** – Virtual network, subnet, and VM networking sample.
- **azure-legacy-sdk-update-resources-java-manage-resource-group** – Resource group CRUD operations.
- **azure-legacy-sdk-update-rundeck-plugins** – Rundeck plugin code for Azure VM automation. From https://github.com/rundeck-plugins/rundeck-azure-plugin
- **azure-legacy-sdk-update-snowflake-jdbc** – Snowflake JDBC integration and build/test assets. From https://github.com/snowflakedb/snowflake-jdbc
- **azure-legacy-sdk-update-storage-java-manage-storage-accounts** – Storage account management sample.
- **azure-legacy-sdk-update-storage-v8** – Blob lifecycle operations built with the Azure Storage SDK v8.
- **azure-legacy-sdk-update-storage-v10** – Blob lifecycle operations implemented with the Azure Storage SDK v10 (ServiceURL/ContainerURL pattern).
- **azure-legacy-sdk-update-storage-v10-advanced** – Concurrent blob upload operations with asynchronous support using Azure Storage SDK v10 (TransferManager with configurable block size and concurrency).
- **azure-legacy-sdk-update-postgresql-manage-server** – Azure Database for PostgreSQL server creation and update sample.
- **azure-legacy-sdk-update-ranger-kms** – Apache Ranger KMS with Azure Key Vault integration. From https://github.com/apache/ranger
- **azure-legacy-sdk-update-resources-java-deploy-using-arm-template** – ARM template deployment sample for Azure resources.
- **azure-legacy-sdk-update-eventhub-akka-connector** – Akka Streams connector for Azure Event Hub. From https://github.com/adobe/eventhub-akka-connector
- **azure-legacy-sdk-update-cloud-connectors** – JavaEE/Jakarta EE JCA connectors for cloud services including Azure Service Bus. From https://github.com/payara/Cloud-Connectors
- **azure-legacy-sdk-update-idam-health-checker** – Spring Boot health checker for ForgeRock system components with Azure Key Vault integration. From https://github.com/hmcts/idam-health-checker
- **azure-legacy-sdk-update-teamcity-azure-agent** – TeamCity Azure cloud plugin that elastically scales build agents. From https://github.com/JetBrains/teamcity-azure-agent
- **azure-legacy-sdk-update-logstash-input-azure-event-hubs** – Logstash input plugin for consuming events from Azure Event Hubs. Legacy Azure SDKs: `com.microsoft.azure:azure-eventhubs:3.3.0`, `com.microsoft.azure:azure-eventhubs-eph:3.3.0`, `com.microsoft.azure:qpid-proton-j-extensions:1.2.4`, `com.microsoft.azure:azure-storage:8.6.6`. Note: primarily a Ruby project (79.2% Ruby, 17.9% Java8). From https://github.com/logstash-plugins/logstash-input-azure_event_hubs
- **azure-legacy-sdk-update-plugin-azure** – Kestra workflow orchestration plugin for Azure services. Legacy Azure SDKs: `com.microsoft.azure:azure-batch:11.2.0`, `com.microsoft.azure:azure-storage:8.6.6`. Uses Gradle, JDK 21, has 49 JUnit 5 test files. From https://github.com/kestra-io/plugin-azure
- **azure-legacy-sdk-update-learn-azure-cosmosdb** – Getting started samples for Azure Cosmos DB (SQL API, MongoDB, Gremlin, Cassandra). Legacy Azure SDKs: `com.microsoft.azure:azure-cosmosdb:2.4.3`, `com.microsoft.azure:azure-documentdb:2.4.3`. Uses Maven, JDK 8. From https://github.com/yaravind/learn-azure-cosmosdb
- **azure-legacy-sdk-update-eventgrid-connector** – Spring Boot application demonstrating connectivity to Azure Event Grid. Legacy Azure SDKs: `com.microsoft.azure:azure-eventgrid:1.2.0`, `com.microsoft.azure:azure-storage-spring-boot-starter`. Uses Maven, JDK 14. From https://github.com/crostonj/EventGridConnector
- **azure-legacy-sdk-update-llmops-java** – LLMOps platform based on LangChain4j with Azure Cognitive Services integration. Legacy Azure SDKs: `com.microsoft.azure.cognitiveservices:azure-cognitiveservices-websearch:1.0.1`, `com.microsoft.azure.cognitiveservices:azure-cognitiveservices-customsearch:1.0.1`. Uses Maven, JDK 17. From https://github.com/llm-manus/llmops-java
- **azure-legacy-sdk-update-samza** – Apache Samza distributed stream processing framework with Azure integrations. Legacy Azure SDKs: `com.microsoft.azure:azure-eventhubs:3.3.0`, `com.microsoft.azure:azure-storage:8.6.6`. Uses Gradle, has 19 test files in samza-azure module. From https://github.com/apache/samza

## Example from public repository

- https://github.com/rundeck-plugins/rundeck-azure-plugin
- https://github.com/snowflakedb/snowflake-jdbc (they've upgraded [recently](https://github.com/snowflakedb/snowflake-jdbc/commit/59332c030746a43c551f3f2ae7767c6e33527e24))
- https://github.com/apache/ranger
- https://github.com/dtinit/data-transfer-project (use JDK11)
- https://github.com/adobe/eventhub-akka-connector
- https://github.com/payara/Cloud-Connectors (use JDK11)
- https://github.com/hmcts/idam-health-checker (use JDK17)
- https://github.com/JetBrains/teamcity-azure-agent
- https://github.com/logstash-plugins/logstash-input-azure_event_hubs (use Gradle, primarily Ruby with 17.9% Java8)
- https://github.com/kestra-io/plugin-azure (use Gradle, JDK 21)
- https://github.com/yaravind/learn-azure-cosmosdb (use Maven, JDK 8)
- https://github.com/crostonj/EventGridConnector (use Maven/Spring Boot, JDK 14)
- https://github.com/llm-manus/llmops-java (use Maven, JDK 17)
- https://github.com/apache/samza (use Gradle)

## Attempted but failed

- https://github.com/wso2-extensions/siddhi-store-cosmosdb – build blocked by parent using custom remote repository.

# azure-sdk-update-examples

- **azure-sdk-update-storage-v12** – Blob lifecycle operations implemented with the Azure Storage SDK v12 (BlobServiceClient/BlobContainerClient pattern).
