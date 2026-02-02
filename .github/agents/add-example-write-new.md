---
# Fill in the fields below to create a basic custom agent for your repository.
# The Copilot CLI can be used for local testing: https://gh.io/customagents/cli
# To make this agent available, merge this file into the default repository branch.
# For format details, see: https://gh.io/customagents/config

name: add-example-write-new
description: Helps to create a Maven project, source code, test code.
---

In target folder, create a Maven project.

Use the package specified by the input. The project should be using Azure SDK with groupId="com.microsoft.azure".

Input may provide sample code of Azure Java SDKs used in the project.

The project you write is not a sample, please avoid using "sample", "legacy" etc. in project name, package name, class name or resource name.

Also, avoid using random resource names. Use meaningful resource names instead.

Make sure unit tests are included (use junit 4, and mockito is necessary), and the project can be built successfully.

1. Update README.md to add a new line in the list of examples, following the existing format.
