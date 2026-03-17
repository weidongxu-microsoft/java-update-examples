---
name: generate-upgrade-skill
description: 'Generates/refreshes the legacy-azure-sdk-for-java-upgrade skill by fetching the latest custom agent from EMU/AppMod and combining with migration guidelines. USE FOR: "generate upgrade skill", "refresh upgrade skill".'
---

# Meta-Skill: Generate the `legacy-azure-sdk-for-java-upgrade` Skill

This skill produces (or refreshes) the `legacy-azure-sdk-for-java-upgrade` skill by combining two upstream sources and transforming MCP tool references into standalone shell-command equivalents.

## Inputs (Two Upstream Sources)

### Source 1: Custom Agent — `modernize-java-upgrade.agent.md`

This is the EMU (AppMod) project's upgrade agent. Resolve the content using this 3-tier fallback:

1. **Primary — Remote URL**: Fetch from:
   ```
   https://raw.githubusercontent.com/devdiv-azure-service-dmitryr/azure-java-migration-copilot-vscode-extension/main/agents/modernize-java-upgrade.agent.md
   ```
2. **Fallback — Local EMU project**: If the remote URL is unavailable (404, network error), search for the local EMU/AppMod project folder. Look for `agents/modernize-java-upgrade.agent.md` relative to common locations:
   - Adjacent to the current repo (e.g., `../azure-java-migration-copilot-vscode-extension/`)
   - In the user's projects folder (e.g., `~/projects/azure-java-migration-copilot-vscode-extension/`)
3. **Last resort — Ask user**: If the file cannot be found locally, ask the user to provide the absolute path to the AppMod project folder that contains `agents/modernize-java-upgrade.agent.md`.

### Source 2: Migration Guidelines — `INSTRUCTION.md`

Read from the local repository:
```
.github/skills/run-benchmark/scripts/INSTRUCTION.md
```

## Output (Progressive Disclosure Pattern)

The generated skill follows a progressive disclosure pattern — concise SKILL.md with detailed migration guidelines in a separate reference file:

```
.github/skills/legacy-azure-sdk-for-java-upgrade/
├── SKILL.md                         # Concise: workflow, rules, phases; references INSTRUCTION.md
└── references/
    └── INSTRUCTION.md               # Detailed: migration context, code samples, package guides
```

- **`SKILL.md`** — Contains the agent role, rules, workflow phases (Precheck → Plan → Execute → Validate), and links to the reference material via `[Migration Guidelines](./references/INSTRUCTION.md)`.
- **`references/INSTRUCTION.md`** — Contains the migration-specific content: Azure SDK context, pom.xml/build.gradle examples, Java code migration guidelines, code samples, package-specific source code guidelines, validation checklist, and package-specific migration guide URLs.

## Transformation Steps

### Step 1: Explore Project Structure

Before generating, explore the current repository to understand:
- Existing skill format (look at `.github/skills/*/SKILL.md` for the YAML frontmatter pattern)
- The current state of `legacy-azure-sdk-for-java-upgrade/SKILL.md` (if it exists)

### Step 2: Read Both Sources

Fetch/read both upstream sources per the instructions above.

### Step 3: Strip and Transform the Custom Agent

The custom agent (`modernize-java-upgrade.agent.md`) is designed for VS Code with MCP tools. Transform it for standalone Copilot skill use:

#### 3a. Remove YAML Frontmatter Metadata

Strip the agent's YAML frontmatter (`tools:`, `handoffs:`, `model:`, `argument-hint:` fields). Replace with the skill's own frontmatter:

```yaml
---
name: legacy-azure-sdk-for-java-upgrade
description: 'Upgrades legacy Azure Java SDKs (com.microsoft.azure) to modern Azure SDKs (com.azure) with structured planning and execution. USE FOR: "upgrade legacy azure sdk", "migrate azure java sdk".'
---
```

#### 3b. MCP Tool Replacements

Apply these substitutions throughout the content:

| MCP Tool / Pattern | Action | Replacement |
|---|---|---|
| All `#appmod-report-event(...)` calls | **Remove** | Delete the call entirely — this is internal telemetry |
| Entire "Event Reporting" section | **Remove** | Delete the section and its rules |
| `#appmod-confirm-upgrade-plan(...)` | **Replace** | "Log the migration plan, then proceed to execution without pausing for confirmation" |
| `#appmod-list-jdks(...)` | **Replace** | "Detect available JDKs: check `JAVA_HOME` environment variable, run `java -version`, scan common JDK install paths (e.g., `C:\Program Files\Microsoft\`, `C:\Program Files\Java\`, `/usr/lib/jvm/`)" |
| `#appmod-list-mavens(...)` | **Replace** | "Detect build tools: check for Maven wrapper (`mvnw`/`mvnw.cmd`) or Gradle wrapper (`gradlew`/`gradlew.bat`) in project root, run `mvn --version` or `gradle --version`" |
| `#appmod-install-jdk(...)` | **Remove** | Replace with a Prerequisites note: "JDK 8 or above must be pre-installed" |
| `#appmod-install-maven(...)` | **Remove** | Replace with a Prerequisites note: "Maven or Gradle must be pre-installed" |
| `#appmod-build-java-project(...)` | **Replace** | "Run `mvn clean test-compile` (or `./gradlew compileTestJava` for Gradle) to verify compilation" |
| `#appmod-run-tests-for-java(...)` | **Replace** | "Run `mvn clean test` (or `./gradlew test` for Gradle) to execute all tests" |
| `#appmod-validate-cves-for-java(...)` | **Remove** | CVE scanning is out of scope for Azure SDK migration |
| `#appmod-generate-tests-for-java(...)` | **Remove** | Test generation is out of scope |
| `#askQuestions` | **Remove** | The skill runs in non-interactive, autonomous mode |
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
| References to `plan.md` / `progress.md` / `summary.md` templates | **Replace** — use inline step-by-step logging instead of file-based progress tracking |
| "Template compliance" rule | **Remove** |
| "Git-optional mode" and git stash/branch management | **Simplify** — remove git branch creation (`appmod/java-upgrade-*`), stash push/pop. Keep: "commit changes if git is available" |
| "Version Knowledge" section (LTS versions, Spring Boot) | **Remove** — not relevant to Azure SDK migration |
| "Intermediate Version Strategy" section | **Remove** — not relevant to Azure SDK migration (direct replacement, no intermediates) |

#### 3d. Narrow Scope to Azure SDK Migration

The upstream agent is generic (Java version, Spring Boot, etc.). Narrow the scope:

- **Goal**: Replace all `com.microsoft.azure.*` dependencies → `com.azure.*`
- **Remove** generic Java/Spring Boot upgrade logic (JDK version upgrades beyond ensuring minimum JDK 8, intermediate version strategy, Spring Boot migration paths)
- **Retain**: Rules (success criteria, anti-excuse, do-not-stop, review, strategy), efficiency guidelines, wrapper preference

### Step 4: Merge with INSTRUCTION.md Content

The INSTRUCTION.md content is **not inlined** into SKILL.md. Instead, it is written as a separate reference file.

**Generate `references/INSTRUCTION.md`** from Source 2 (`INSTRUCTION.md`), keeping the migration-specific content intact:

- Migration Context (legacy SDKs end-of-support, need to migrate)
- pom.xml / build.gradle migration examples
- Java code migration guidelines
- Package-Specific Source Code Guidelines (com.microsoft.azure.management.**)
  - Code Checklist
  - Code Samples (Authentication with File, ProviderRegistrationInterceptor)
- Validation checklist
- Package-Specific Migration Guides (public URLs — instruct the agent to fetch these at runtime)

**Important**: Strip all `#generate_upgrade_plan` references from the INSTRUCTION.md content (search the entire file for this pattern and remove every occurrence). The skill itself IS the plan and executor.

**In `SKILL.md`**, reference the file at appropriate points:
- In the introduction: "For detailed Azure SDK migration guidelines, code samples, and package mappings, see [Migration Guidelines](./references/INSTRUCTION.md)."
- In Phase 2 (Plan): "Consult [Migration Guidelines](./references/INSTRUCTION.md) for package mappings and migration guides."
- In Phase 3 Step 1 (Build Config): "Follow the pom.xml/build.gradle examples in [Migration Guidelines](./references/INSTRUCTION.md)."
- In Phase 3 Step 2 (Source Code): "Follow the code migration guidelines and samples in [Migration Guidelines](./references/INSTRUCTION.md)."
- In Validation: "Apply the validation checklist from [Migration Guidelines](./references/INSTRUCTION.md)."

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
4. Verify `references/INSTRUCTION.md` exists and contains migration content (package guides, code samples, validation)
5. Verify key sections in `SKILL.md`: Rules, Workflow (Precheck → Plan → Execute → Validate)

## Additional Resources

- [Migration Guidelines (INSTRUCTION.md)](../run-benchmark/scripts/INSTRUCTION.md) — upstream source for the generated `references/INSTRUCTION.md`

## Notes

- If `INSTRUCTION.md` content has already been incorporated into the existing `legacy-azure-sdk-for-java-upgrade/SKILL.md`, use the existing skill as a reference for the final structure — it represents the desired merged format.
- The generated skill should be fully self-contained — no external tool dependencies beyond standard shell commands, web fetch (for migration guide URLs), and file system access.
