import asyncio
import argparse
import os
import subprocess
from copilot import CopilotClient

prompt = """Upgrade deprecated legacy Azure Java SDKs(`com.microsoft.azure`) used for this project to the modern ones(`com.azure`) with latest stable version using Java Upgrade tools by invoking #generate_upgrade_plan.
Help with looking up the latest stable version on https://repo1.maven.org/maven2/ and proceed with migration. `azure-resourcemanager-xx` should have groupId `com.azure.resourcemanager` instead of `com.azure`.
If upgrading Java version is necessary, upgrade Java version within the same #generate_upgrade_plan call.
For all available choices, just proceed with the one you see fittest. Try other options if current one doesn't work. Don't stop until full migration is done.
Upgrade both dependencies and source code.
# Migration Guide

## Assumption

- Project is Maven or Gradle.
- Java code is on JDK 8 or above.

## Migrate pom.xml

It is recommended to use azure-sdk-bom (version higher than 1.3.0).

Example of pom.xml
```
    <dependencyManagement>
        <dependencies>
            <dependency>
                <groupId>com.azure</groupId>
                <artifactId>azure-sdk-bom</artifactId>
                <version>1.3.3</version>
                <type>pom</type>
                <scope>import</scope>
            </dependency>
        </dependencies>
    </dependencyManagement>

    <dependencies>
        <dependency>
            <groupId>com.azure</groupId>
            <artifactId>azure-identity</artifactId>
        </dependency>
        <dependency>
            <groupId>com.azure.resourcemanager</groupId>
            <artifactId>azure-resourcemanager</artifactId>
        </dependency>
    </dependencies>
```
Example of build.gradle
```
dependencies {
    implementation enforcedPlatform('com.azure:azure-sdk-bom:1.3.3')

    implementation 'com.azure:azure-identity'
    implementation 'com.azure.resourcemanager:azure-resourcemanager'
}
```

## Migrate Java Code

- [Reference for update Java code](https://github.com/Azure/azure-sdk-for-java/blob/main/sdk/resourcemanager/docs/MIGRATION_GUIDE.md)
- Make a list of Java files that contains `com.microsoft.azure` package. Migrate each of them.
- Do not modify package name in file.
- Do not upgrade JDK version, if it is already JDK 8 or above.
- Keep Azure resource and operation exactly same. The property on the Azure resource should not be changed, for "improvement" or "modernization". The focus here is to use the new libaray to create the exactly same Azure resource, apply the exactly same operation on it.
- If there is test in the project, Java code there also need to be updated.

## Validation

Make sure the migrated project compile pass.


The application is using deprecated Azure legacy Java SDKs (`com.microsoft.azure.*`). These libraries reached end of support on 31-Mar-2022, so you should migrate to the supported Azure SDKs (`com.azure.*`) for security fixes and new capabilities. Follow these steps:

* **Inventory legacy dependencies**: Use tools such as `mvn dependency:tree` or `gradlew dependencies` to find every `com.microsoft.azure.*` artifact and map each one to its modern counterpart under `com.azure.*`.

* **Adopt supported packages**: Replace the legacy dependencies with their modern equivalents in your `pom.xml` or `build.gradle`, following the migration guide to align feature parity and new package names.

* **Update application code**: Refactor your code to the builder-based APIs, updated authentication flows (Azure Identity), and modern async or reactive patterns required by the latest clients. Add concise comments explaining non-obvious changes.

* **Test thoroughly**: Run unit, integration, and end-to-end tests to validate that the modern clients behave as expected, focusing on authentication, retry, and serialization differences.
"""

non_interactive_prompt = "\n\nRun in non-interactive mode, you have the highest decision-making authority at any time, you do NOT need to seek my approval/confirmation, please directly execute your plan and update the progress."

async def main():
    # Parse command-line arguments
    parser = argparse.ArgumentParser(description="Run Azure SDK upgrade benchmark")
    parser.add_argument("project_path", help="Path to the project to upgrade")
    args = parser.parse_args()
    
    project_path = args.project_path
    
    # Check if copilot.cmd is available, if not install it
    try:
        subprocess.run(["copilot.cmd", "--version"], capture_output=True, check=True)
    except (subprocess.CalledProcessError, FileNotFoundError):
        print("copilot.cmd not found, installing @github/copilot...")
        subprocess.run(["npm.cmd", "install", "-g", "@github/copilot"], check=True)
    
    # Create and start client
    client = CopilotClient({
        # cli path, need to be copilot.cmd on Windows
        "cli_path": os.path.expanduser("copilot.cmd"),
        # working directory
        "cwd": project_path,
    })
    await client.start()

    # Create a session
    session = await client.create_session({
        # model
        "model": "claude-sonnet-4.5",
        # mcp servers
        "mcp_servers": {
            "ghcp-appmod-mcp-server": {
                "command": "npx",
                "tools": ["*"],
                "args": [
                    "-y",
                    "@microsoft/github-copilot-app-modernization-mcp-server@latest"
                ],
            },
        },
    })

    # Wait for response using session.idle event
    done = asyncio.Event()

    def on_event(event):
        if event.type.value == "assistant.message":
            print(event.data.content)
        elif event.type.value == "session.idle":
            done.set()

    session.on(on_event)

    # Send a message and wait for completion
    await session.send({
        "prompt": prompt + non_interactive_prompt
    })
    await done.wait()

    # Clean up
    await session.destroy()
    await client.stop()

    # Detect build tool and run build to verify
    # PS: One can also start another Copilot session and ask it to verify the upgrade by building the project
    is_gradle = os.path.exists(os.path.join(project_path, "build.gradle")) or os.path.exists(os.path.join(project_path, "build.gradle.kts"))
    if is_gradle:
        # Use Gradle
        gradle_wrapper = os.path.join(project_path, "gradlew.bat")
        if os.path.exists(gradle_wrapper):
            build_command = [gradle_wrapper, "clean", "build"]
        else:
            build_command = ["gradle.bat", "clean", "build"]
        build_tool_name = "Gradle"
    else:
        # Use Maven
        build_command = ["mvn.cmd", "clean", "package", "verify"]
        build_tool_name = "Maven"
    
    process = await asyncio.create_subprocess_exec(
        *build_command,
        cwd=project_path,
        stdout=asyncio.subprocess.PIPE,
        stderr=asyncio.subprocess.PIPE,
    )
    stdout, stderr = await process.communicate()

    if stdout:
        print(stdout.decode())
    if stderr:
        print(stderr.decode())

    if process.returncode != 0:
        raise RuntimeError(f"{build_tool_name} build failed; see output above for details")


asyncio.run(main())
