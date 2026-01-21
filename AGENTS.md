When prompted, first check which task you are asked to perform. And only perform that task.

# Tasks

## Update metadata.csv file

Prerequisite: "metadata.csv" is opened in context. This file does not exist in this project.

For each "azure-legacy-sdk-update-" folder from root, check if metadata.csv contains the name in "instance_id".

If metadata.csv does not contain it, add a new rule to the file. Use the row "azure-legacy-sdk-update-compute-java-manage-vm" as example.

## Add example from Azure-Samples

The input would be e.g. add exmaple from "Azure-Samples/storage-java-manage-storage-accounts".

Take the `<example-name>` as the repository name. In above case, it is "storage-java-manage-storage-accounts".

1. Create a temporary folder in project root, checkout (`git clone`) the GitHub repository into that folder.
1. In the checked out folder, read git history, check out the commit with message "Release v1.36.3".
1. Delete the ".git" folder from the checked out folder.
1. Move the "<example-name>" folder in the temporary folder to "azure-legacy-sdk-update-<example-name>" folder at project root.
1. Delete the temporary folder.
1. In "azure-legacy-sdk-update-<example-name>" folder, modify "pom.xml". Replace goal=attach to single in maven-assembly-plugin. Replace 1.7 to 1.8 in maven-compiler-plugin.
1. In "azure-legacy-sdk-update-<example-name>" folder, clean up unused method in "Utils.java" (usually, it is easier to re-write the java file, so that it would now contain method that is used). Run `mvn compile` to validate build pass.
