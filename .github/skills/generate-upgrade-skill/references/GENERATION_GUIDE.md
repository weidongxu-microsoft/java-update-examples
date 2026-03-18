# Generation Guide: `legacy-azure-sdk-for-java-upgrade` Skill

This document contains the detailed transformation steps for generating the upgrade skill.

## Inputs (Three Upstream Sources)

### Source 1: Custom Agent — `modernize-java-upgrade.agent.md`

Resolve using this 3-tier fallback:

1. **Primary — Remote URL**: Fetch from the `devdiv-azure-service-dmitryr/azure-java-migration-copilot-vscode-extension` repo on GitHub, path `agents/modernize-java-upgrade.agent.md` on the `main` branch.
2. **Fallback — Local EMU project**: Search for a local clone of `azure-java-migration-copilot-vscode-extension` adjacent to the current repo or in the user's projects folder.
3. **Last resort — Ask user**: Prompt for the absolute path to the AppMod project folder containing `agents/modernize-java-upgrade.agent.md`.

### Source 2: Migration Guidelines — `INSTRUCTION.md`

Read from the local repository at `.github/skills/run-benchmark/scripts/INSTRUCTION.md`.

### Source 3: Upgrade Workflow Templates

Read the following template files from the local EMU project clone (same location as Source 1):

- `agents/java-upgrade.plan.template.md` — Plan template with embedded LLM instructions
- `agents/java-upgrade.progress.template.md` — Progress tracking template with execution rules
- `agents/java-upgrade.summary.template.md` — Summary report template with content guidelines

These templates contain **HTML comments with LLM instructions** that guide the agent through populating each section. The comments are the primary mechanism for structured plan/progress/summary generation.

## Output Structure

```
.github/skills/legacy-azure-sdk-for-java-upgrade/
├── SKILL.md                         # Concise: workflow, rules, phases
└── references/
    ├── INSTRUCTION.md               # Detailed: migration context, code samples, package guides
    ├── PLAN_TEMPLATE.md             # Plan template with embedded LLM instructions
    ├── PROGRESS_TEMPLATE.md         # Progress tracking template with execution rules
    └── SUMMARY_TEMPLATE.md          # Summary report template with content guidelines
```

- **`SKILL.md`** — Agent role, rules, workflow phases (Precheck → Plan → Execute → Validate), links to reference material.
- **`references/INSTRUCTION.md`** — Migration-specific content: Azure SDK context, pom.xml/build.gradle examples, Java code migration guidelines, code samples, package-specific source code guidelines, validation checklist, and package-specific migration guide URLs.
- **`references/PLAN_TEMPLATE.md`** — Template for generating the migration plan. Contains sections for Available Tools, Upgrade Goals, Technology Stack, Derived Upgrades, Upgrade Steps, and Key Challenges — each with HTML-comment instructions for the agent.
- **`references/PROGRESS_TEMPLATE.md`** — Template for tracking execution progress. Contains execution rules (success criteria, verification expectations, review rules, commit format) and a step-tracking format with status, changes, verification, and commit info.
- **`references/SUMMARY_TEMPLATE.md`** — Template for generating the final summary report. Contains sections for Upgrade Result, Tech Stack Changes, Commits, Challenges, Limitations, Review Summary, and Next Steps.

## Transformation Steps

### Step 1: Explore Project Structure

Before generating, explore the current repository to understand:
- Existing skill format (look at `.github/skills/*/SKILL.md` for the YAML frontmatter pattern)
- The current state of `legacy-azure-sdk-for-java-upgrade/SKILL.md` (if it exists)

### Step 2: Read All Sources

Fetch/read all three upstream sources per the instructions above.

### Step 3: Strip and Transform the Custom Agent

The custom agent is designed for VS Code with MCP tools. Transform it for standalone Copilot skill use:

#### 3a. Remove YAML Frontmatter Metadata

Strip the agent's YAML frontmatter (`tools:`, `handoffs:`, `model:`, `argument-hint:` fields). Replace with the skill's own frontmatter:

```yaml
---
name: legacy-azure-sdk-for-java-upgrade
description: 'Upgrades legacy Azure Java SDKs (com.microsoft.azure) to modern Azure SDKs (com.azure) with structured planning and execution. USE FOR: "upgrade legacy azure sdk", "migrate azure java sdk".'
---
```

#### 3b. MCP Tool Replacements

| MCP Tool / Pattern | Action | Replacement |
|---|---|---|
| All `#appmod-report-event(...)` calls **except** template-copying milestones | **Remove** | Delete — internal telemetry |
| `#appmod-report-event(event: "precheckCompleted")` | **Replace** | "Create `plan.md` in the project directory from `references/PLAN_TEMPLATE.md` — replace placeholders (`<SESSION_ID>` → use a short UUID or timestamp, `<PROJECT_NAME>`, `<current_branch>`, `<current_commit_id>`, datetime) and follow the HTML-comment instructions to populate each section" |
| `#appmod-report-event(event: "planExecutionCompleted")` | **Replace** | "Create `summary.md` in the project directory from `references/SUMMARY_TEMPLATE.md` — replace placeholders and follow HTML-comment instructions to populate final results" |
| Entire "Event Reporting" section | **Remove** | Delete the section and its rules |
| `#appmod-confirm-upgrade-plan(...)` | **Replace** | "Create `progress.md` in the project directory from `references/PROGRESS_TEMPLATE.md` — replace placeholders. Log the migration plan, then proceed to execution without pausing for confirmation" |
| `#appmod-list-jdks(...)` | **Replace** | See expanded replacement below ¹ |
| `#appmod-list-mavens(...)` | **Replace** | See expanded replacement below ² |
| `#appmod-install-jdk(...)` | **Remove** | Replace with Prerequisites note: "JDK 8+ must be pre-installed" |
| `#appmod-install-maven(...)` | **Remove** | Replace with Prerequisites note: "Maven or Gradle must be pre-installed" |
| `#appmod-build-java-project(...)` | **Replace** | "Run `mvn clean test-compile` (or `./gradlew compileTestJava` for Gradle)" |
| `#appmod-run-tests-for-java(...)` | **Replace** | "Run `mvn clean test` (or `./gradlew test` for Gradle)" |
| `#appmod-validate-cves-for-java(...)` | **Remove** | CVE scanning is out of scope |
| `#appmod-generate-tests-for-java(...)` | **Remove** | Test generation is out of scope |
| `#askQuestions` | **Remove** | Non-interactive, autonomous mode |
| `#appmod-preview-markdown` | **Remove** | Not needed |
| `#generate_upgrade_plan` (from INSTRUCTION.md) | **Remove** | The skill itself IS the plan and executor |

**¹ Expanded replacement for `#appmod-list-jdks`:**

> Detect available JDKs using the following discovery strategy:
> 1. Check `JAVA_HOME` and `JDK_HOME` environment variables
> 2. Run `java -version` and `javac -version` to detect the default JDK
> 3. Search common JDK installation paths:
>    - Windows: `C:\Program Files\Java\`, `C:\Program Files\Eclipse Adoptium\`, `C:\Program Files\Microsoft\`, `%USERPROFILE%\.jdk\`, `%USERPROFILE%\.jdks\`
>    - Linux: `/usr/lib/jvm/`, `/opt/`, `/usr/local/java/`
>    - macOS: `/Library/Java/JavaVirtualMachines/`, `~/.sdkman/candidates/java/`
> 4. Check for version manager installations (SDKMAN, ASDF, jenv, Jabba)
> 5. Look for sibling JDK directories next to any found installation
> 6. For each found JDK, read the `release` file to determine the version
>
> Report all found JDKs with their path, version, and discovery source.

**² Expanded replacement for `#appmod-list-mavens`:**

> Detect build tools:
> 1. Check for Maven Wrapper (`mvnw`/`mvnw.cmd`) or Gradle Wrapper (`gradlew`/`gradlew.bat`) in the project root — prefer wrappers when present
> 2. If a wrapper exists, read `.mvn/wrapper/maven-wrapper.properties` or `gradle/wrapper/gradle-wrapper.properties` to determine the wrapper-defined version
> 3. Run `mvn --version` or `gradle --version` to detect system installations
> 4. Check `MAVEN_HOME`/`M2_HOME` environment variables
> 5. Search common Maven installation paths
>
> Report all found installations with their path, version, and source.

#### 3c. Remove or Simplify Sections

| Section | Action |
|---|---|
| `SESSION_ID` system and all references | **Simplify** — replace `SESSION_ID` with a simpler mechanism (use a short UUID or timestamp generated at the start of execution). Remove `SessionContextManager` and MCP session tracking, but keep the concept of a unique run identifier for plan/progress/summary file naming |
| "Session ID Consistency" section | **Remove** — the simplified ID does not require the same cross-tool consistency rules |
| Phase 3: "Confirm Plan with User" | **Remove** — non-interactive mode. Instead, after plan generation, proceed directly to creating `progress.md` and starting execution |
| Phase 5: "Summarize & Cleanup" | **Simplify** — remove CVE scan, coverage collection. Keep: "Create `summary.md` from `references/SUMMARY_TEMPLATE.md` and populate it. Verify all goals met" |
| Phase 6: "Prompt for Follow-up Actions" | **Remove** entirely |
| "Handoffs" section | **Remove** |
| References to `plan.md` / `progress.md` / `summary.md` templates | **Keep and adapt** — the templates are now skill reference files. Replace upstream template paths (`agents/java-upgrade.*.template.md`) with skill reference paths (`references/PLAN_TEMPLATE.md`, `references/PROGRESS_TEMPLATE.md`, `references/SUMMARY_TEMPLATE.md`). The workflow is: create each file from its template at the appropriate phase, populate sections following the HTML-comment instructions, then remove the HTML comments |
| "Template compliance" rule | **Adapt** — change from "follow rules in each section's HTML comments of the specific files when populating plan.md, progress.md, summary.md" to "follow the HTML-comment instructions in the template reference files when creating and populating plan.md, progress.md, summary.md" |
| "Git-optional mode" and git stash/branch management | **Simplify** — remove branch creation, stash push/pop. Keep: "commit changes if git is available" |
| "Version Knowledge" section (LTS versions, Spring Boot) | **Remove** — not relevant |
| "Intermediate Version Strategy" section | **Remove** — not relevant |

#### 3d. Narrow Scope to Azure SDK Migration

The upstream agent is generic (Java version, Spring Boot, etc.). Narrow the scope:

- **Goal**: Replace all `com.microsoft.azure.*` dependencies → `com.azure.*`
- **Remove** generic Java/Spring Boot upgrade logic
- **Retain**: Rules (success criteria, anti-excuse, do-not-stop, review, strategy), efficiency guidelines, wrapper preference

### Step 4: Merge with INSTRUCTION.md Content

The INSTRUCTION.md content is **not inlined** into SKILL.md. Instead, write it as a separate reference file.

**Generate `references/INSTRUCTION.md`** from Source 2, keeping migration-specific content intact:
- Migration Context
- pom.xml / build.gradle migration examples
- Java code migration guidelines
- Package-Specific Source Code Guidelines
- Validation checklist
- Package-Specific Migration Guides (public URLs)

**Important**: Strip all `#generate_upgrade_plan` references from the content.

**In `SKILL.md`**, reference the file at appropriate points:
- Introduction: "See `references/INSTRUCTION.md`."
- Phase 2 (Plan): "Consult `references/INSTRUCTION.md` for package mappings."
- Phase 3 Step 1 (Build Config): "Follow examples in `references/INSTRUCTION.md`."
- Phase 3 Step 2 (Source Code): "Follow guidelines and samples in `references/INSTRUCTION.md`."
- Validation: "Apply the validation checklist from `references/INSTRUCTION.md`."

### Step 4b: Transform and Write Upgrade Workflow Templates

Transform the three upstream templates (Source 3) for Azure SDK migration scope and write them as skill reference files.

#### Transformation Rules for All Templates

Apply these transformations to **all three** template files:

1. **Strip MCP tool references**: Replace all `#appmod-*` tool calls in HTML comments with their skill equivalents from the MCP Tool Replacements table (Step 3b). For example:
   - `#appmod-list-jdks(sessionId)` → "Detect available JDKs (see skill workflow)"
   - `#appmod-report-event(...)` → Delete
   - `#appmod-build-java-project(...)` → "`mvn clean test-compile` (or `./gradlew compileTestJava`)"
   - `#appmod-run-tests-for-java(...)` → "`mvn clean test` (or `./gradlew test`)"
   - `#appmod-validate-cves-for-java(...)` → Delete (out of scope)
   - `#appmod-install-jdk(...)` → Delete (JDK must be pre-installed)

2. **Replace SESSION_ID mechanism**: Replace `<SESSION_ID>` placeholder references with a simpler approach — use a timestamp or short identifier (e.g., `<RUN_ID>` generated as `azure-sdk-upgrade-YYYYMMDD-HHMMSS`). Remove any references to `SessionContextManager` or MCP session management.

3. **Remove VS Code-specific features**: Remove references to `#appmod-preview-markdown`, `previewMarkdown`, VS Code output channels, and any UI-specific instructions.

4. **Remove out-of-scope sections**:
   - CVE scanning sections/references (references to `#appmod-validate-cves-for-java`)
   - Test generation sections (references to `#appmod-generate-tests-for-java`)
   - Coverage collection sections
   - "Handoffs" or follow-up action prompts

5. **Narrow scope**: Remove generic Java/Spring Boot upgrade content. Keep only what applies to Azure SDK migration (`com.microsoft.azure.*` → `com.azure.*`). Specifically:
   - Remove "Intermediate Version Strategy" references and samples
   - Remove "Version Knowledge" (Java LTS, Spring Boot versions) from plan template
   - Simplify "Derived Upgrades" to focus on Azure SDK replacements rather than framework version chains
   - Keep "Build tool compatibility" references since Maven/Gradle version matters for Azure SDK upgrades too

#### Template-Specific Rules

**`references/PLAN_TEMPLATE.md`** (from `java-upgrade.plan.template.md`):
- Keep: Available Tools, Upgrade Goals, Technology Stack, Upgrade Steps, Key Challenges sections
- Simplify: "Available Tools" — remove JDK installation markup (`<TO_BE_INSTALLED>`) since JDK must be pre-installed. Keep build tool detection.
- Simplify: "Upgrade Steps" — remove "Setup Environment" as a mandatory step (JDK pre-installed). Keep "Setup Baseline" and "Final Validation" as mandatory.
- Keep: HTML-comment instructions that guide the agent through populating each section
- Keep: Sample step format (Rationale, Changes to Make, Verification)

**`references/PROGRESS_TEMPLATE.md`** (from `java-upgrade.progress.template.md`):
- Keep: Execution rules (success criteria, anti-excuse, verification expectations, review code changes, commit format)
- Keep: Step tracking format with status emojis (🔘 ⏳ ✅ ❗)
- Keep: Sample step entries
- Remove: References to `#appmod-version-control`
- Simplify: Git operations — "commit if git available" instead of branch management

**`references/SUMMARY_TEMPLATE.md`** (from `java-upgrade.summary.template.md`):
- Keep: Upgrade Result, Tech Stack Changes, Commits, Challenges, Limitations, Review Summary, Next Steps
- Remove: CVE Scan Results section entirely
- Remove: Test Coverage section (optional — keep if simple, remove if it references JaCoCo-specific tooling)
- Simplify: Artifacts section — remove session-directory-specific paths, use project-relative paths

#### Writing the Templates

Write the three transformed template files to:

1. **`references/PLAN_TEMPLATE.md`** → `.github/skills/legacy-azure-sdk-for-java-upgrade/references/PLAN_TEMPLATE.md`
2. **`references/PROGRESS_TEMPLATE.md`** → `.github/skills/legacy-azure-sdk-for-java-upgrade/references/PROGRESS_TEMPLATE.md`
3. **`references/SUMMARY_TEMPLATE.md`** → `.github/skills/legacy-azure-sdk-for-java-upgrade/references/SUMMARY_TEMPLATE.md`

**In `SKILL.md`**, update the workflow to reference the templates:
- Phase 1 (Precheck): "On success, create `plan.md` from `references/PLAN_TEMPLATE.md`"
- Phase 2 (Plan): "Populate `plan.md` following the HTML-comment instructions in each section"
- Phase 3 (Execute): "Create `progress.md` from `references/PROGRESS_TEMPLATE.md` before starting execution. Update `progress.md` after each step."
- Phase 4 (Validate): "Create `summary.md` from `references/SUMMARY_TEMPLATE.md` and populate with final results."

### Step 5: Write the Output

Write FIVE files:

1. **`SKILL.md`** → `.github/skills/legacy-azure-sdk-for-java-upgrade/SKILL.md`
2. **`references/INSTRUCTION.md`** → `.github/skills/legacy-azure-sdk-for-java-upgrade/references/INSTRUCTION.md`
3. **`references/PLAN_TEMPLATE.md`** → `.github/skills/legacy-azure-sdk-for-java-upgrade/references/PLAN_TEMPLATE.md`
4. **`references/PROGRESS_TEMPLATE.md`** → `.github/skills/legacy-azure-sdk-for-java-upgrade/references/PROGRESS_TEMPLATE.md`
5. **`references/SUMMARY_TEMPLATE.md`** → `.github/skills/legacy-azure-sdk-for-java-upgrade/references/SUMMARY_TEMPLATE.md`

Create the `references/` directory if it does not exist.

### Step 6: Verify

After writing, verify all output files:
1. Grep all files for any remaining `#appmod-`, `#askQuestions`, `#generate_upgrade_plan` references — there should be **none**
2. Verify `SKILL.md` YAML frontmatter is valid (has `name` and `description`)
3. Verify `SKILL.md` contains relative links to `./references/INSTRUCTION.md` and the three template files
4. Verify `references/INSTRUCTION.md` exists and contains migration content
5. Verify key sections in `SKILL.md`: Rules, Workflow (Precheck → Plan → Execute → Validate)
6. Verify `references/PLAN_TEMPLATE.md` exists and contains plan structure with HTML-comment instructions
7. Verify `references/PROGRESS_TEMPLATE.md` exists and contains progress tracking format
8. Verify `references/SUMMARY_TEMPLATE.md` exists and contains summary report structure
9. Verify all three template files have had MCP tool references stripped

### Step 7: Validate with Waza

Run `waza check` to validate the generated skill meets quality and compliance standards.

> **Important**: Only `waza check` is needed — no other waza commands are required. Assume waza is already installed. See the [Waza User Guide](https://github.com/microsoft/waza/blob/main/docs/GUIDE.md) if installation is needed.

#### 7a. Run `waza check` with Iterative Fix Loop (up to 5 attempts)

Run `waza check` on the generated skill directory. If the check reports issues, fix them and re-check — repeat **up to 5 iterations** until the check passes or the maximum attempts are reached.

**Loop procedure:**

```
attempt = 1
while attempt <= 5:
    Run: waza check .github/skills/legacy-azure-sdk-for-java-upgrade
    if all checks pass:
        break  # Validation successful
    else:
        Analyze the check output (see 7b below)
        Apply fixes to SKILL.md or add new reference files
        attempt += 1

if attempt > 5:
    Log a warning: "waza check did not fully pass after 5 attempts"
    Document remaining issues for manual review
```

**Command:**

```bash
waza check .github/skills/legacy-azure-sdk-for-java-upgrade
```

#### 7b. Interpret and Fix Check Results

`waza check` reports three categories. Address each as follows:

**⚠️ NEVER modify `references/INSTRUCTION.md` or the three template files (`references/PLAN_TEMPLATE.md`, `references/PROGRESS_TEMPLATE.md`, `references/SUMMARY_TEMPLATE.md`) to fix check issues.** These are sourced from upstream and must remain unchanged. If content needs to be moved out of SKILL.md, create a new reference file (e.g., `references/EXAMPLES.md`, `references/RULES.md`) and link to it from SKILL.md.

| Check | What it means | How to fix |
|---|---|---|
| **Compliance scoring** (Low / Medium / Medium-High / High) | Validates SKILL.md frontmatter completeness and structure | Ensure `name` and `description` are present in YAML frontmatter; add missing metadata fields suggested by the output |
| **Token budget** | Ensures SKILL.md is within the token limit | If over budget, move verbose content (examples, long tables) into a **new** reference file (e.g., `references/EXAMPLES.md`) and link to it from SKILL.md. **Do not modify `references/INSTRUCTION.md`** |
| **Evaluation presence** | Confirms an `eval.yaml` exists for the skill | Create `evals/legacy-azure-sdk-for-java-upgrade/eval.yaml` manually |

After each fix, re-run `waza check` as described in the loop above.

## Notes

- If `INSTRUCTION.md` content has already been incorporated into the existing `legacy-azure-sdk-for-java-upgrade/SKILL.md`, use it as a reference for the final structure.
- The generated skill should be fully self-contained — no external tool dependencies beyond standard shell commands, web fetch, and file system access.
- The three template files (`PLAN_TEMPLATE.md`, `PROGRESS_TEMPLATE.md`, `SUMMARY_TEMPLATE.md`) contain HTML comments that serve as LLM instructions. The agent reads these comments when creating plan.md/progress.md/summary.md, follows the instructions to populate each section, then removes the comments. This is the primary mechanism for structured plan-driven migration.
- The upstream templates are located in the `agents/` directory of the EMU project, not in `src/java-upgrade/tools/`. The MCP tools (`ReportEventTool`, `ConfirmPlanTool`) merely copy these templates to the session directory at milestone events — the template content itself is the important part.
