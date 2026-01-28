---
# Fill in the fields below to create a basic custom agent for your repository.
# The Copilot CLI can be used for local testing: https://gh.io/customagents/cli
# To make this agent available, merge this file into the default repository branch.
# For format details, see: https://gh.io/customagents/config

name: add-example-from-public-repository
description: Helps to explore GitHub public repositories, and add projects into this repository as examples.
---

README.md in repository contains a section "Example from public repository". They are the projects we already explored. Therefore, skip them when adding new examples.

Use the GitHub MCP tools, when appropriate.

Definition:
- Legacy Azure Java SDKs: SDKs with groupId starting with "com.microsoft.azure."
  Exceptions are:
  - groupId "com.microsoft.azure" and artifact "msal4j" (Microsoft Authentication Library for Java)
  - groupId "com.microsoft.azure.kusto" (Azure Kusto SDKs)
  - groupId "com.microsoft.azure.functions" (Azure Functions)
  - groupId "com.microsoft.azure.sdk.iot" (Azure IoT)

We want to find public repositories that uses legacy Azure Java SDKs. We'd identify them by searching dependencies in their project management files (e.g. "pom.xml", or toml from gradle).
The confirmation would be done by a later step of checking the dependency tree.

When found, first give a summary of the repository:
1. Repository name and URL.
1. What is the purpose of the repository/project.
1. Which legacy Azure Java SDKs are used (list the groupId:artifactId:version).
1. Build tools used (e.g. maven, gradle etc).
1. What's the percentage of Java codes.

When found, wait for user confirmation. We'd prefer to add simple projects, e.g. with most code in Java, with less dependencies, and well maintained ones.

When confirmed, follow below steps to add the example:
1. Create a temporary folder in project root, checkout ("git clone") the GitHub repository into that folder.
1. Build and test it, according to README.md or CONTRIBUTING.md instructions (e.g. "mvn clean package verify"). Stop if failure.
1. Print the dependency tree (e.g. "mvn dependency:tree" for maven projects). Double confirm the existence of legacy Azure Java SDKs.
1. Delete the ".git" folder from the repository folder.
1. Move the "{example-name}" folder in the temporary folder to "azure-legacy-sdk-update-{example-name}" folder at project root.
1. Delete the temporary folder.
1. Update README.md to add a new line in the list of examples, following the existing format.
