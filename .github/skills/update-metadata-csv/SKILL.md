---
name: update-metadata-csv
description: Ensure metadata.csv has rows for every azure-legacy-sdk-update-* sample folder
---

# Skill Instructions

## Prerequisites

- metadata.csv must be opened in the current context (even though the file does not exist in this repo by default).

## Steps

1. Enumerate every repository-root folder whose name starts with azure-legacy-sdk-update-.
2. For each folder, verify metadata.csv already lists the folder name in the instance_id column.
3. When a folder name is missing, append a new row modeled after the azure-legacy-sdk-update-compute-java-manage-vm example so formatting and column order match.
