<!--
  This is the upgrade plan template for Azure SDK migration.
  RUN_ID should be replaced with the actual run identifier.

  ## PLANNING RULES

  !!! DON'T REMOVE THIS COMMENT BLOCK BEFORE FINAL PLAN IS GENERATED AS IT CONTAINS IMPORTANT INSTRUCTIONS.

  ### Strategy
  - **Incremental upgrades**: Stepwise dependency upgrades to avoid large jumps breaking builds
  - **Minimal changes**: Only upgrade dependencies essential for compatibility
  - **Risk-first**: Handle EOL/challenging deps early in isolated steps
  - **Necessary/Meaningful steps only**: Each step MUST change code/config. NO steps for pure analysis/validation. Merge small related changes.

  ### Mandatory Steps
  - **Step 1 (MANDATORY)**: Setup Baseline - Run compile/test with current JDK, document results.
  - **Steps 2-N**: Upgrade steps - dependency order, high-risk early, isolated breaking changes
  - **Final step (MANDATORY)**: Final Validation - verify all goals met, all TODOs resolved, 100% tests pass

  ### Verification Expectations
  - **Steps 1-N (Setup/Upgrade)**: Focus on COMPILATION SUCCESS. Tests may fail during intermediate steps.
  - **Final Validation**: COMPILATION SUCCESS + 100% TEST PASS

  ### Efficiency (IMPORTANT)
  - **Targeted reads**: Use `grep` over full file reads; read specific sections, not entire files. Template files are large - only read the sections you need.
  - **Quiet commands**: Use `-q`, `--quiet` for build/test commands when appropriate
  - **Progressive writes**: Update plan.md incrementally, not at end
-->

# Upgrade Plan: <PROJECT_NAME> (<RUN_ID>)

- **Generated**: <datetime> <!-- replace with actual date and time when generating -->
- **HEAD Branch**: <current_branch> <!-- replace with actual head branch when generating -->
- **HEAD Commit ID**: <current_commit_id> <!-- replace with actual head commit id when generating -->

## Available Tools

<!--
  List ONLY the JDKs and build tools that are required/used during the upgrade (not all discovered ones).
  Use the environment detection results from Precheck to check availability.
  Mark build tools that need upgrading for JDK compatibility as "**<TO_BE_UPGRADED>**".
  If a wrapper (mvnw/gradlew) is present, also check the wrapper-defined version in
  `.mvn/wrapper/maven-wrapper.properties` or `gradle/wrapper/gradle-wrapper.properties`.

  NOTE: This section is finalized during Upgrade Path Design (after step sequence is known), not during Environment Analysis.

  SAMPLE:
  **JDKs**
  - JDK 1.8.0: /path/to/jdk-8 (current project JDK, used by step 1)

  **Build Tools**
  - Maven 3.9.6: /path/to/maven
  - Maven Wrapper: 3.8.1 → **<TO_BE_UPGRADED>** to 3.9.6+ (current version incompatible with target)
-->

## Guidelines

<!--
  User-specified guidelines or constraints in bullet points for this upgrade.
  Extract these from the user's prompt if provided, or leave empty if none specified.
  These guidelines take precedence over default upgrade strategies.
-->

> Note: You can add any specific guidelines or constraints for the upgrade process here if needed, bullet points are preferred. <!-- this note is for users, NEVER remove it -->

## Upgrade Goals

<!--
  List the Azure SDK migration goals:
  - Replace all com.microsoft.azure.* dependencies with com.azure.* equivalents
  - Any additional user-requested goals (e.g., Java upgrade if below JDK 8)

  SAMPLE:
  - Replace all legacy Azure SDK dependencies (com.microsoft.azure.*) with modern equivalents (com.azure.*)
  - Upgrade Java from 7 to 8 (if below JDK 8)
-->

### Technology Stack

<!--
  Table of core dependencies and their compatibility with upgrade goals.
  IMPORTANT: Analyze ALL modules in multi-module projects, not just the root module.
  Only include: direct dependencies + those critical for upgrade compatibility.
  CRITICAL: Identify and clearly flag EOL (End of Life) dependencies - these pose security risks and must be upgraded.

  Columns:
  - Technology/Dependency: Name of the dependency (mark EOL dependencies with "⚠️ EOL" suffix)
  - Current: Version currently in use
  - Min Compatible Version: Minimum version that works with upgrade goals (or N/A if replaced)
  - Why Incompatible: Explanation of incompatibility, or "-" if already compatible. For EOL deps, mention security/support concerns.

  IMPORTANT: Include build tools (Maven/Gradle), wrappers, and key build plugins in this table.
  Build tools and plugins are upgrade-critical even though they are not runtime dependencies.

  SAMPLE:
  | Technology/Dependency                              | Current | Min Compatible | Why Incompatible                                             |
  | -------------------------------------------------- | ------- | -------------- | ------------------------------------------------------------ |
  | Java                                               | 8       | 8              | -                                                            |
  | com.microsoft.azure:azure ⚠️ EOL                   | 1.41.4  | N/A            | Replaced by com.azure.resourcemanager:azure-resourcemanager  |
  | com.microsoft.azure:azure-storage ⚠️ EOL           | 8.0.0   | N/A            | Replaced by com.azure:azure-storage-blob                     |
  | Maven (wrapper)                                    | 3.6.3   | 3.6.3          | -                                                            |
  | maven-compiler-plugin                              | 3.8.1   | 3.8.1          | -                                                            |
-->

| Technology/Dependency | Current | Min Compatible | Why Incompatible |
| --------------------- | ------- | -------------- | ---------------- |

### Derived Upgrades

<!--
  Required upgrades inferred from the Azure SDK migration.
  Each derived upgrade must have a justification explaining WHY it's required.
  Common derivations for Azure SDK migration:
  - com.microsoft.azure:azure → com.azure.resourcemanager:azure-resourcemanager
  - com.microsoft.azure:azure-storage → com.azure:azure-storage-blob
  - com.microsoft.azure:azure-keyvault → com.azure:azure-security-keyvault-*
  - com.microsoft.azure:azure-servicebus → com.azure:azure-messaging-servicebus
  - com.microsoft.azure:azure-eventhubs → com.azure:azure-messaging-eventhubs
  - Authentication: Azure Identity (com.azure:azure-identity) for modern auth flows
  - BOM: com.azure:azure-sdk-bom for centralized version management

  SAMPLE:
  - Replace com.microsoft.azure:azure with com.azure.resourcemanager:azure-resourcemanager (legacy SDK EOL)
  - Add com.azure:azure-identity for modern authentication (required by modern SDKs)
  - Add com.azure:azure-sdk-bom for centralized version management (recommended best practice)
-->

## Upgrade Steps

<!--
  Step-by-step upgrade plan. Each step should follow this format:
  - **Step N: <Descriptive Title>**
    - **Rationale**: Why this step is needed and why at this position
    - **Changes to Make**: ≤5 bullet points (concise)
    - **Verification**: Command, JDK, Expected Result

  VERIFICATION EXPECTATIONS:
  - Steps 1-N (Setup and Upgrade steps): Focus on COMPILATION SUCCESS. Tests may fail during intermediate steps.
  - Final step: COMPILATION SUCCESS + TEST PASS through iterative fix loop.

  MANDATORY FIRST STEP:
  The first step MUST always be:
  1. Setup Baseline (establish pre-upgrade compile/test results)

  MANDATORY SETUP BASELINE STEP SAMPLE:

  - Step 1: Setup Baseline
    - **Rationale**: Establish pre-upgrade compile and test results to measure upgrade success against.
    - **Changes to Make**:
      - [ ] Run baseline compilation with current JDK
      - [ ] Run baseline tests with current JDK
    - **Verification**:
      - Command: `mvn clean compile test-compile -q && mvn clean test -q`
      - JDK: <current project JDK path>
      - Expected: Document SUCCESS/FAILURE, test pass rate (forms acceptance criteria)

  ---

  SAMPLE STEP (dependency upgrade):

  - Step N: Update Build Configuration for Modern Azure SDK
    - **Rationale**: Replace legacy com.microsoft.azure dependencies with com.azure equivalents in build config.
    - **Changes to Make**:
      - [ ] Add azure-sdk-bom to dependency management
      - [ ] Replace com.microsoft.azure:azure with com.azure.resourcemanager:azure-resourcemanager
      - [ ] Add com.azure:azure-identity for authentication
      - [ ] Remove legacy azure dependencies
    - **Verification**:
      - Command: `mvn clean test-compile -q`
      - JDK: <JDK path>
      - Expected: Compilation SUCCESS (tests may fail - will be fixed in later steps)

  ---

  SAMPLE STEP (source code migration):

  - Step N: Migrate Source Code to Modern Azure SDK
    - **Rationale**: Update Java source files to use modern Azure SDK APIs.
    - **Changes to Make**:
      - [ ] Update import statements from com.microsoft.azure.* to com.azure.*
      - [ ] Migrate authentication to Azure Identity (ClientSecretCredential, etc.)
      - [ ] Update fluent API builder calls to match modern SDK surface
      - [ ] Migrate test code to use modern SDK APIs
    - **Verification**:
      - Command: `mvn clean test-compile -q`
      - JDK: <JDK path>
      - Expected: Compilation SUCCESS

  ---

  MANDATORY FINAL STEP (must always be the last step):

  - Step N: Final Validation
    - **Rationale**: Verify all upgrade goals met, project compiles successfully, all tests pass.
    - **Changes to Make**:
      - [ ] Verify no legacy com.microsoft.azure.* dependencies remain
      - [ ] Resolve ALL TODOs and temporary workarounds from previous steps
      - [ ] Clean rebuild with current JDK
      - [ ] Fix any remaining compilation errors
      - [ ] Run full test suite and fix ALL test failures (iterative fix loop until 100% pass)
    - **Verification**:
      - Command: `mvn clean test -q`
      - JDK: <JDK path>
      - Expected: Compilation SUCCESS + 100% tests pass
-->

## Key Challenges

<!--
  Document high-risk areas that require special attention during migration.
  Each challenge should have a mitigation strategy. Be concise.
  Common challenges for Azure SDK migration:
  - Authentication pattern changes (file-based auth, service principal, managed identity)
  - Async/reactive API changes
  - Package namespace changes (com.microsoft.azure → com.azure)
  - API surface changes (fluent builders, different method names)
  - Resource manager API differences

  SAMPLE:
  - **Authentication Pattern Migration**
     - **Challenge**: Legacy file-based authentication using Azure.configure().authenticate(credentialFile) must be replaced with explicit credential parsing.
     - **Strategy**: Parse credential JSON with Jackson, use ClientSecretCredential with AzureProfile. See Migration Guidelines for code samples.
  - **Resource Manager API Changes**
     - **Challenge**: Method signatures and return types differ between legacy and modern resource manager.
     - **Strategy**: Follow the track2 migration guide for each resource type. Use grep to find all usages before migrating.
-->
