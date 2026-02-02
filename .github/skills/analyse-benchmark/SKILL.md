---
name: analyse-benchmark
description: Analyse benchmark logs
---

# Skill Instructions

## Preparation

When user provide a local zip file, prepare the "benchmark_log" folder for analysis.

1. Unzip the file to a temporary folder in this repository. Let's call it the "benchmark_log" folder.
1. Unzip the zips within "benchmark_log" folder. Let's call them the "case_output" folder.

## Analyse

When user provide the "benchmark_log" folder for analysis. You can start the Analyse from here.

1. Read "eval/mvn_test_compile_output.txt" and "eval/mvn_test_output.txt" in each "case_output" folder. Analyse whether build pass or failed.
1. For any failed build, provide a summary on the cause of the failure.
