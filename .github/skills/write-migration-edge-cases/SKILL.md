---
name: write-migration-edge-cases
description: 'Create Java projects using legacy Azure SDKs (com.microsoft.azure) with patterns that resist automated migration without a migration guide but succeed with one.'
---

# Write Migration Edge Cases

Create a Maven-based Java project using legacy Azure SDKs (`com.microsoft.azure`) with patterns that are **hard or impossible** for AI agents to migrate **without** an explicit migration guide, but **can be migrated** when the guide is provided.

## Goal

Prove the value of the [Migration Guide](./references/MIGRATION_GUIDE.md). Each generated project should demonstrate that:

1. **Without the guide** — a general-purpose coding agent fails to produce a compiling/passing migration.
2. **With the guide** — the same migration succeeds (or gets significantly closer).

This creates measurable evidence that the migration guide improves outcomes.

## Input

- **Azure service area** (e.g. Storage, Event Hubs, Key Vault, Cosmos DB, Management, Service Bus, Batch). If omitted, pick one.
- **Difficulty tier** (optional): `tier-1` (moderate), `tier-2` (hard), `tier-3` (maximum). Default: `tier-2`.
- **Folder name** (optional): defaults to `azure-legacy-sdk-update-edge-case-<service>`.

## Workflow

### Phase 1 — Scaffold

1. Read the [Edge-Case Pattern Catalog](./references/EDGE_CASE_CATALOG.md) and pick **at least 3** patterns matching the tier.
2. Read the [Legacy SDK Reference](./references/LEGACY_SDK_REFERENCE.md) for artifact versions, strategy notes, and anti-patterns.
3. Create the Maven project following [Conventions](#conventions).
4. Write production code and unit tests exercising the chosen patterns.
5. Verify: `mvn test-compile && mvn test` must both pass.

### Phase 2 — Adversarial Loop (without guide)

Test whether a bare migration instruction defeats the agent. Each iteration is committed so context can be cleared between rounds.

> **CRITICAL — You MUST run all 50 iterations.** Do NOT skip ahead to Phase 3 early just because a few rounds succeeded. The purpose of this phase is exhaustive hardening. Even if the first 10 rounds all pass, rounds 11–50 may reveal patterns that finally defeat the agent. Only after completing all 50 rounds (or achieving a failure) may you proceed to Phase 3. Stopping early undermines the entire purpose of this skill.

6. **Attempt migration WITHOUT the guide**: Launch a sub-agent (general-purpose or task agent) with only this generic instruction — _"Upgrade deprecated legacy Azure Java SDKs (`com.microsoft.azure`) used for this project to the modern ones (`com.azure`) with latest stable version."_ Do NOT include the migration guide, edge-case catalog, or any hints. The sub-agent must work from the generic instruction alone.
7. **Evaluate**: After the sub-agent finishes, **independently verify** by running `mvn test-compile && mvn test` yourself (do not trust the sub-agent's claim):
   - `mvn test-compile` **fails** → edge case succeeded for the "without guide" bar. **Revert the migrated code** (`git checkout -- .`). Go to Phase 3.
   - Compiles but `mvn test` **fails** → edge case succeeded. **Revert the migrated code**. Go to Phase 3.
   - Everything passes → not hard enough. **Revert the migrated code**. Continue to step 8.
8. **Harden & commit**: Analyze _specifically_ why migration succeeded (inspect the diff with `git diff` before reverting). Add harder patterns from the catalog to the legacy code — focus on patterns the agent hasn't seen yet and patterns that compound with existing ones. Verify `mvn test-compile && mvn test` still pass with the hardened legacy code. Then **commit** the hardened legacy code with a message summarizing this iteration:
   ```
   edge-case iteration N: <service area>

   Patterns added: <list of pattern IDs added this round, e.g. 2.3, 3.1>
   Migration result: passed (not hard enough)
   Why it succeeded: <brief analysis, e.g. "agent correctly mapped CloudBlockBlob to BlobClient">
   Next: <what to try next, e.g. "add reflection + transitive dependency patterns">
   ```
9. **Clear context**: After committing, clear the agent's conversation context. On the next iteration, the agent should read `git log --oneline` and the latest commit's full message (`git log -1 --format=%B`) to recover the history of previous iterations before proceeding.
10. **Repeat steps 6–9 for exactly 50 iterations** (or until a migration attempt fails). Track the current iteration number explicitly (e.g. in commit messages and in a session SQL table). You MUST NOT proceed to Phase 3 until either:
    - A migration attempt **fails** (compile error or test failure), OR
    - You have completed **all 50 iterations** with the migration succeeding every time.
    If all 50 rounds pass, accept the result and note it in the README.

### Phase 3 — Guided Migration Check

Verify the migration guide makes the difference.

10. Revert to the original legacy code.
11. **Attempt migration WITH the guide**: Read the [Migration Guide](./references/MIGRATION_GUIDE.md) and use its instructions, package-specific guidelines, code samples, and linked migration guides to perform the migration.
12. **Evaluate**:
    - `mvn test-compile && mvn test` both **pass** → the guide proved its value. Record this as a success. Go to Phase 4.
    - Still fails → the guide has a gap. Continue to step 13.
13. **Improve the guide**: Analyze the failure. Identify the missing knowledge (e.g., an unmapped API, a missing code sample, an unaddressed pattern). Add the necessary content to [MIGRATION_GUIDE.md](./references/MIGRATION_GUIDE.md) — new sections, code samples, or package-specific guidelines that would enable a correct migration.
14. Revert to the original legacy code. Re-attempt migration using the updated guide. Go back to step 12.
15. Repeat steps 12–14 up to **3 iterations**. If migration still fails after 3 guide updates, the edge case is genuinely beyond the guide's scope — note the gap in the README and keep the project as-is.

### Phase 4 — Finalize

16. Ensure the final project (with legacy SDKs) compiles and passes tests.
17. Write `README.md` documenting:
    - Which edge-case patterns are used.
    - Why they resist bare migration (no guide).
    - How the migration guide addresses them (or which gaps remain).
    - Any guide updates made during Phase 3.
18. Update the repository root `README.md` to add the new project to the examples list.

## Conventions

- **Java version**: `1.8` (source and target).
- **Dependencies**: JUnit 4.13.2, Mockito 4.11.0 (+ mockito-inline if mocking final classes).
- **Plugins**: maven-compiler-plugin, exec-maven-plugin (with mainClass), maven-surefire-plugin.
- **Package naming**: realistic packages (e.g. `com.microsoft.azure.storage.advanced`). No "sample", "edge-case", or "legacy" in names.
- **Code style**: production-quality. No comments hinting at migration difficulty.
- **Secrets**: never real credentials. Use placeholders like `"DefaultEndpointsProtocol=https;AccountName=devaccount;AccountKey=dGVzdGtleQ==;"`.

## References

- [Edge-Case Pattern Catalog](./references/EDGE_CASE_CATALOG.md) — 19 patterns across 3 tiers with code examples.
- [Legacy SDK Reference](./references/LEGACY_SDK_REFERENCE.md) — artifact table, strategy notes, anti-patterns.
- [Migration Guide](./references/MIGRATION_GUIDE.md) — the guide whose value is being proven (used in Phase 3 only).