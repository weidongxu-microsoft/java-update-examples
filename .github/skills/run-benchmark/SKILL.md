---
name: run-benchmark
description: Run benchmark case in local
argument-hint: [project-full-path]
---

Call the "run_benchmark.py" script, with argument of the **absolute folder path** of the project folder.

E.g.
```
python run_benchmark.py [project-absolute-path]
```

## Prerequisites

- JDK required for the project is installed (typically, JDK 11 and 17).
- Latest GitHub Copilot CLI is installed.
- The python requirements.txt is installed.
- Read root README.md, and build tool file ("pom.xml" or "build.gradle"), to decide whether the project require JDK 11 or 17.
- Before running the script, set JDK to 11 or 17 accordingly, e.g.
    ```
    JAVA_HOME="C:\Program Files\Microsoft\jdk-11.0.29.7-hotspot"
    ```

## Additional resources

- ["run_benchmark.py" script](./scripts/run_benchmark.py)
