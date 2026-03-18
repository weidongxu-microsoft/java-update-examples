# Generation Guide: `legacy-azure-sdk-for-java-upgrade` Skill

This document contains the detailed transformation steps for generating the upgrade skill.

## Inputs (Two Upstream Sources)

### Source 1: Custom Agent — `modernize-java-upgrade.agent.md`

Resolve using this 3-tier fallback:

1. **Primary — Remote URL**: Fetch from the `devdiv-azure-service-dmitryr/azure-java-migration-copilot-vscode-extension` repo on GitHub, path `agents/modernize-java-upgrade.agent.md` on the `main` branch.
2. **Fallback — Local EMU project**: Search for a local clone of `azure-java-migration-copilot-vscode-extension` adjacent to the current repo or in the user's projects folder.
3. **Last resort — Ask user**: Prompt for the absolute path to the AppMod project folder containing `agents/modernize-java-upgrade.agent.md`.

### Source 2: Migration Guidelines — `INSTRUCTION.md`

Read from the local repository at `.github/skills/run-benchmark/scripts/INSTRUCTION.md`.

## Output Structure

```
.github/skills/legacy-azure-sdk-for-java-upgrade/
├── SKILL.md                         # Concise: workflow, rules, phases
└── references/
    └── INSTRUCTION.md               # Detailed: migration context, code samples, package guides
```

- **`SKILL.md`** — Agent role, rules, workflow phases (Precheck → Plan → Execute → Validate), links to reference material.
- **`references/INSTRUCTION.md`** — Migration-specific content: Azure SDK context, pom.xml/build.gradle examples, Java code migration guidelines, code samples, package-specific source code guidelines, validation checklist, and package-specific migration guide URLs.

## Transformation Steps

### Step 1: Explore Project Structure

Before generating, explore the current repository to understand:
- Existing skill format (look at `.github/skills/*/SKILL.md` for the YAML frontmatter pattern)
- The current state of `legacy-azure-sdk-for-java-upgrade/SKILL.md` (if it exists)

### Step 2: Read Both Sources

Fetch/read both upstream sources per the instructions above.

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
| All `#appmod-report-event(...)` calls | **Remove** | Delete — internal telemetry |
| Entire "Event Reporting" section | **Remove** | Delete the section and its rules |
| `#appmod-confirm-upgrade-plan(...)` | **Replace** | "Log the migration plan, then proceed to execution without pausing for confirmation" |
| `#appmod-list-jdks(...)` | **Replace** | "Detect available JDKs: check `JAVA_HOME`, run `java -version`, scan common JDK install paths" |
| `#appmod-list-mavens(...)` | **Replace** | "Detect build tools: check for Maven/Gradle wrappers in project root, run `mvn --version` or `gradle --version`" |
| `#appmod-install-jdk(...)` | **Remove** | Replace with Prerequisites note: "JDK 8+ must be pre-installed" |
| `#appmod-install-maven(...)` | **Remove** | Replace with Prerequisites note: "Maven or Gradle must be pre-installed" |
| `#appmod-build-java-project(...)` | **Replace** | "Run `mvn clean test-compile` (or `./gradlew compileTestJava` for Gradle)" |
| `#appmod-run-tests-for-java(...)` | **Replace** | "Run `mvn clean test` (or `./gradlew test` for Gradle)" |
| `#appmod-validate-cves-for-java(...)` | **Remove** | CVE scanning is out of scope |
| `#appmod-generate-tests-for-java(...)` | **Remove** | Test generation is out of scope |
| `#askQuestions` | **Remove** | Non-interactive, autonomous mode |
| `#appmod-preview-markdown` | **Remove** | Not needed |
| `#generate_upgrade_plan` (from INSTRUCTION.md) | **Remove** | The skill itself IS the plan and executor |

#### 3c. Remove or Simplify Sections

| Section | Action |
|---|---|
| `SESSION_ID` system and all references | **Remove** — no session tracking |
| "Session ID Consistency" section | **Remove** |
| Phase 3: "Confirm Plan with User" | **Remove** — non-interactive mode |
| Phase 5: "Summarize & Cleanup" | **Simplify** — remove CVE scan, coverage collection, `.md` template references. Keep: "Verify all goals met" |
| Phase 6: "Prompt for Follow-up Actions" | **Remove** entirely |
| "Handoffs" section | **Remove** |
| References to `plan.md` / `progress.md` / `summary.md` templates | **Replace** — use inline step-by-step logging |
| "Template compliance" rule | **Remove** |
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

### Step 5: Write the Output

Write TWO files:

1. **`SKILL.md`** → `.github/skills/legacy-azure-sdk-for-java-upgrade/SKILL.md`
2. **`references/INSTRUCTION.md`** → `.github/skills/legacy-azure-sdk-for-java-upgrade/references/INSTRUCTION.md`

Create the `references/` directory if it does not exist.

### Step 6: Verify

After writing, verify both output files:
1. Grep both files for any remaining `#appmod-`, `#askQuestions`, `#generate_upgrade_plan` references — there should be **none**
2. Verify `SKILL.md` YAML frontmatter is valid (has `name` and `description`)
3. Verify `SKILL.md` contains relative links to `./references/INSTRUCTION.md`
4. Verify `references/INSTRUCTION.md` exists and contains migration content
5. Verify key sections in `SKILL.md`: Rules, Workflow (Precheck → Plan → Execute → Validate)

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

**⚠️ NEVER modify `references/INSTRUCTION.md` to fix check issues.** INSTRUCTION.md is a migration-specific reference sourced from upstream and must remain unchanged. If content needs to be moved out of SKILL.md, create a new reference file (e.g., `references/EXAMPLES.md`, `references/RULES.md`) and link to it from SKILL.md.

| Check | What it means | How to fix |
|---|---|---|
| **Compliance scoring** (Low / Medium / Medium-High / High) | Validates SKILL.md frontmatter completeness and structure | Ensure `name` and `description` are present in YAML frontmatter; add missing metadata fields suggested by the output |
| **Token budget** | Ensures SKILL.md is within the token limit | If over budget, move verbose content (examples, long tables) into a **new** reference file (e.g., `references/EXAMPLES.md`) and link to it from SKILL.md. **Do not modify `references/INSTRUCTION.md`** |
| **Evaluation presence** | Confirms an `eval.yaml` exists for the skill | Create `evals/legacy-azure-sdk-for-java-upgrade/eval.yaml` manually |

After each fix, re-run `waza check` as described in the loop above.

## Notes

- If `INSTRUCTION.md` content has already been incorporated into the existing `legacy-azure-sdk-for-java-upgrade/SKILL.md`, use it as a reference for the final structure.
- The generated skill should be fully self-contained — no external tool dependencies beyond standard shell commands, web fetch, and file system access.
