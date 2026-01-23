---
page_type: sample
languages:
- java
products:
- azure
extensions:
- services: Azure Database for PostgreSQL
- platforms: java
---

# Getting Started with Azure Database for PostgreSQL - Manage Server - in Java #

This sample demonstrates how to use the legacy `com.microsoft.azure.postgresql.v2017_12_01:azure-mgmt-postgresql`
management library to:
- Create a PostgreSQL server with administrator credentials, SKU, and storage settings
- Update the server to scale compute/storage and retag the resource

## Running this Sample ##

1. Run this sample on an Azure resource that has managed identity enabled (for example, a VM, App Service, or Azure Function) and grant that identity sufficient permissions (at least *Contributor*) on the target subscription. Set the environment variable `AZURE_SUBSCRIPTION_ID` to the subscription you want to use. If you are using a user-assigned managed identity, also set `AZURE_CLIENT_ID` to the identity's client ID.
2. Clone this repository and navigate to the sample folder:

       git clone https://github.com/weidongxu-microsoft/java-update-examples.git
       cd java-update-examples/azure-legacy-sdk-update-postresql-manage-server

3. Build and run the sample:

       mvn clean compile exec:java

The sample creates a temporary resource group and removes it at the end, leaving your subscription clean.

## More information ##

[http://azure.com/java](http://azure.com/java)

If you don't have a Microsoft Azure subscription you can get a FREE trial account [here](http://go.microsoft.com/fwlink/?LinkId=330212)

---

This project has adopted the [Microsoft Open Source Code of Conduct](https://opensource.microsoft.com/codeofconduct/). For more information see the
[Code of Conduct FAQ](https://opensource.microsoft.com/codeofconduct/faq/) or contact [opencode@microsoft.com](mailto:opencode@microsoft.com) with any
additional questions or comments.
