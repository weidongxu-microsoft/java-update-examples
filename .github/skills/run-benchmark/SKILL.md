---
name: run-benchmark
description: '**UTILITY SKILL** - Run benchmark case in local. USE FOR: "run benchmark". INVOKES: "run_benchmark.py" script.'
---

Call the "run_benchmark.py" script, with argument of the **absolute folder path** of the project folder.

E.g.
```
python run_benchmark.py [project-absolute-path]
```

Do not attempt to fix the error. Just report whether the benchmark passed or failed, depends on the output of the script.

## Prerequisites

- JDK required for the project is installed (typically, JDK 11 and 17).
- Latest GitHub Copilot CLI is installed.

## Before running the script

- Read root README.md, and build tool file ("pom.xml" or "build.gradle"), to decide whether the project require JDK 11 or 17.
- Find the path to the JDK, e.g. installed OpenJDK from Microsoft is typically in folder "C:\Program Files\Microsoft"
- Set "JAVA_HOME" env to the proper JDK path, when running the script.

## Additional resources

- ["run_benchmark.py" script](./scripts/run_benchmark.py)
