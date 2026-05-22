# Plan: Upgrade Legacy Azure SDKs for Java

## Scope

- Workspace: `/Users/xiaofeicao/projects/java-update-examples/azure-legacy-sdk-update-batch-java-manage-batch-accounts`
- Assessment report: `.github/modernize/assessment/reports/report-20260522081142/report.json`
- Selected category: `Azure SDKs Version Upgrade (Java)`
- Selected solution: `Upgrade from Legacy Azure SDKs for Java`
- Knowledge base id: `azure-legacy-java-sdk-upgrade`

## Goal

Replace end-of-support legacy Azure SDK for Java usage under `com.microsoft.azure.*` with supported Azure Java SDKs while preserving the sample's Batch account management behavior.

## Current Findings

- The project is a Maven-based Java sample targeting Java 8.
- The current dependency surface includes `com.microsoft.azure:azure:1.36.3` from `pom.xml`.
- The main sample imports legacy management APIs from `com.microsoft.azure.management.*` in `src/main/java/com/microsoft/azure/management/batch/samples/ManageBatchAccount.java`.
- The selected assessment category is scoped to the legacy Azure SDK migration problem only; other assessment findings are intentionally excluded from this plan.

## Execution Plan

1. Inventory the legacy Azure SDK surface in the sample and map each used management type to its supported replacement library or API pattern.
2. Update Maven dependencies to remove the legacy meta-package and add the supported Azure management libraries needed for Batch, Storage, Resources, Identity, and shared HTTP/client dependencies.
3. Refactor authentication and client construction away from the legacy `Azure` entry point to the supported Azure management client pattern.
4. Refactor Batch account, application, key, and storage management calls in `ManageBatchAccount.java` to the supported SDK APIs while keeping the sample flow intact.
5. Update any helper code or sample documentation affected by the SDK migration.
6. Build the sample and resolve any compile or API-shape regressions introduced by the migration.

## Deliverables

- Updated Maven dependency set using supported Azure Java SDKs.
- Refactored sample source that no longer depends on `com.microsoft.azure.*` management packages.
- Validation evidence that the project builds successfully after migration.

## Success Criteria

- No legacy `com.microsoft.azure.*` Azure SDK dependency remains in the build.
- The sample compiles successfully with Maven.
- The Batch account management sample retains the documented management workflow after the SDK migration.

## Notes

- This plan is intentionally limited to the selected assessment category.
- Java version upgrades, framework upgrades, and cloud-readiness items from the assessment remain out of scope for this plan.
