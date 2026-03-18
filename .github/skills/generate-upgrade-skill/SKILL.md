---
name: generate-upgrade-skill
description: '**UTILITY SKILL** - Fetch upstream agent and migration guidelines, then generate the legacy-azure-sdk-for-java-upgrade skill. USE FOR: "generate upgrade skill", "refresh upgrade skill". DO NOT USE FOR: "upgrade azure sdk", "migrate java sdk", "run benchmark". INVOKES: web_fetch, file read/write.'
---

# Generate the `legacy-azure-sdk-for-java-upgrade` Skill

Fetch the EMU/AppMod custom agent, strip MCP tool references, combine with local migration guidelines, and write the resulting skill files.

**DO NOT USE FOR:** upgrading Azure SDKs directly, running benchmarks, editing example projects, or any task other than regenerating the upgrade skill itself.

## Workflow

1. **Fetch sources** — Retrieve `modernize-java-upgrade.agent.md` from the EMU repo (with local fallback), and read migration guidelines from this repository.
2. **Transform** — Strip MCP tools, remove VS Code-specific sections, narrow scope to Azure SDK migration. See [Generation Guide](./references/GENERATION_GUIDE.md) for the full transformation table.
3. **Write output** — Generate `SKILL.md` and `references/INSTRUCTION.md` under `.github/skills/legacy-azure-sdk-for-java-upgrade/`.
4. **Verify** — Grep output for leftover MCP references; confirm frontmatter and links are valid.
5. **Validate with Waza** — Run `waza check` on the generated skill. If issues are found, fix and re-check in a loop (up to 5 attempts). **Never modify `references/INSTRUCTION.md`** to fix check issues — create additional reference files instead. See [Generation Guide → Step 7](./references/GENERATION_GUIDE.md#step-7-validate-with-waza) for details.

## Examples

```
User: "generate upgrade skill"
→ Fetches latest agent, transforms, writes two files to legacy-azure-sdk-for-java-upgrade/

User: "refresh upgrade skill"
→ Same as above, overwriting the existing skill with updated upstream content.
```

## Troubleshooting

- **Remote fetch fails**: Falls back to local EMU repo clone, then prompts user for the path.
- **Missing INSTRUCTION.md**: Verify `.github/skills/run-benchmark/scripts/INSTRUCTION.md` exists.

## References

- [Generation Guide](./references/GENERATION_GUIDE.md) — detailed transformation steps, MCP tool replacement table, and section removal rules.
