---
name: legacy-azure-sdk-for-java-upgrade
description: 'Upgrade legacy Azure Java SDKs (com.microsoft.azure) to modern Azure SDKs (com.azure). USE FOR: "upgrade legacy azure sdk", "migrate azure java sdk". DO NOT USE FOR: "generate upgrade skill", "run benchmark", "analyze benchmark". INVOKES: shell commands, web_fetch, file read/write.'
---

Upgrade all `com.microsoft.azure.*` dependencies to `com.azure.*` equivalents. Autonomous, non-interactive — complete the entire migration in one session.

## References

- [Rules and Workflow](./references/RULES.md) — success criteria, anti-excuse rules, do-not-stop policy, detailed phased workflow
- [Migration Guidelines](./references/INSTRUCTION.md) — package mappings, code samples, validation checklist

## Workflow

1. **Precheck** — Verify Maven/Gradle project, detect JDK and build tools
2. **Plan** — Inventory legacy deps, map to modern equivalents, consult [Migration Guidelines](./references/INSTRUCTION.md)
3. **Execute** — Update build config, then source code file by file per [Migration Guidelines](./references/INSTRUCTION.md), build and test
4. **Validate** — Apply [validation checklist](./references/INSTRUCTION.md#validation), ensure compilation passes, all tests pass, no legacy refs remain

## Key Constraints

- **100% test pass rate** — every failure must be fixed, no exceptions
- **No premature termination** — finish entire migration or document unrecoverable blocker
- **Incremental upgrades** — stepwise dependency changes to minimize breakage
- **Review each step** — verify sufficiency and necessity before proceeding
- Use wrapper scripts (`mvnw`/`gradlew`) when present; use `grep` over full file reads

See [Rules and Workflow](./references/RULES.md) for full execution rules and detailed workflow phases.
