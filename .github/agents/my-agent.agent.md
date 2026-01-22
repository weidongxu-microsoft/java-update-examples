---
# Fill in the fields below to create a basic custom agent for your repository.
# The Copilot CLI can be used for local testing: https://gh.io/customagents/cli
# To make this agent available, merge this file into the default repository branch.
# For format details, see: https://gh.io/customagents/config

name: add-example-from-Azure-Samples
description: Helps to copy and refine Java example from Azure-Samples repositories
---

The input would be e.g. add exmaple from "Azure-Samples/storage-java-manage-storage-accounts".

Take the "{example-name}" as the repository name. In above case, it is "storage-java-manage-storage-accounts".

1. Create a temporary folder in project root, checkout ("git clone") the GitHub repository into that folder.
1. In the repository folder, read git history, checkout the commit with message "Release v1.36.3".
1. Delete the ".git" folder from the repository folder.
1. Move the "{example-name}" folder in the temporary folder to "azure-legacy-sdk-update-{example-name}" folder at project root.
1. Delete the temporary folder.

In "azure-legacy-sdk-update-{example-name}" folder
1. Modify "pom.xml". Replace goal=attach to single in maven-assembly-plugin. Replace 1.7 to 1.8 in maven-compiler-plugin.
1. Avoid calling `createRandomName` method in Utils. Use a meaningful resource name instead. Do not use "legacy" in the name.
1. Clean up unused method in "Utils.java" (usually, it is easier to re-write the java file, so that it would now contain method that is used). Run mvn compile to validate build pass.
