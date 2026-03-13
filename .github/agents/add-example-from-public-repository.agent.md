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
1. Whether the project has runnable tests (unit tests or integration tests). List test frameworks detected (e.g. JUnit 4/5, TestNG, Mockito).

When found, wait for user confirmation. We'd prefer to add projects that:
- Have most code in Java, with less dependencies, and are well maintained.
- **Have runnable tests** (unit tests or integration tests) that can help verify migration correctness after upgrading from legacy to modern Azure SDKs. Projects without tests are less preferred.

When adding multiple samples in parallel (e.g. fleet mode), ensure **each new sample covers a different Azure data-plane library**. For example, do NOT add two samples that both use `azure-cosmos` or both use `azure-storage`. The goal is to maximize coverage of distinct Track 1 data-plane libraries across all samples. Using a different build system (e.g. Gradle vs Maven) for the same library does NOT count as new coverage. An exception can be made for widely-used, well-known open-source projects (e.g. Apache foundation projects) which are valuable examples regardless of library overlap.

When confirmed, follow below steps to add the example:
1. Create a temporary folder in project root, checkout ("git clone") the GitHub repository into that folder.
1. Build and test it, according to README.md or CONTRIBUTING.md instructions (e.g. "mvn clean package verify"). Run existing tests (e.g. "mvn test" or "gradle test") to confirm they pass — these tests will later serve as a migration verification baseline. Stop if build failure.
1. Print the dependency tree (e.g. "mvn dependency:tree" for maven projects). Double confirm the existence of legacy Azure Java SDKs.
1. Delete the ".git" folder from the repository folder.
1. Move the "{example-name}" folder in the temporary folder to "azure-legacy-sdk-update-{example-name}" folder at project root.
1. Delete the temporary folder.
1. Update README.md to add a new line in the list of examples, following the existing format. List legacy Azure dependencies used in the project for convenience of reference. If the project is not Java dominant, add a note for what language it is mainly using. If possible, specify JDK version used in the project.
