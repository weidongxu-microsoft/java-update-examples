---
name: analyze-benchmark
description: '**UTILITY SKILL** - Analyze benchmark logs and generate a summary. USE FOR: "analyze benchmark".
---

# Skill Instructions

## Steps

### Preparation

When user provide a local zip file, prepare the "benchmark_log" folder for analysis.

1. Unzip the file to a temporary folder in this repository. Let's call it the "benchmark_log" folder.
1. Unzip the zips within "benchmark_log" folder. Let's call them the "case_output" folder.

### Analyze

When user provide the "benchmark_log" folder for analysis. You can start the Analyze from here.

1. Read "eval/mvn_test_compile_output.txt" and "eval/mvn_test_output.txt" in each "case_output" folder. Analyze whether build pass or failed.
1. **For any failed build**, provide a summary on the cause of the failure.
1. **For any passed build**, further check:
    - "benchmark/before-dependency.csv" and "benchmark/after-dependency.csv" to analyze the dependency changes, whether all lgecy dependencies are removed and replaced with the new dependencies.
    - "benchmark/evaluation-report.json" to identify potential issue during the migration.
1. Generate a summary report that aggregate for each "case_output" folder, including:
    - Build status (pass/fail)
    - Cause of failure (if any)
    - Dependency changes summary
    - Potential issues identified during migration (if any)
