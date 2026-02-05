import asyncio
import argparse
import os
import platform
import subprocess
from copilot import CopilotClient

# Helper function to add .cmd extension on Windows
def get_command(base_cmd):
    """Add .cmd extension on Windows"""
    return f"{base_cmd}.cmd" if platform.system() == "Windows" else base_cmd

# Read prompt from INSTRUCTION.md
script_dir = os.path.dirname(os.path.abspath(__file__))
instruction_md_path = os.path.join(script_dir, "INSTRUCTION.md")
with open(instruction_md_path, "r", encoding="utf-8") as f:
    prompt = f.read()

non_interactive_prompt = "\n\nRun in non-interactive mode, you have the highest decision-making authority at any time, you do NOT need to seek my approval/confirmation, please directly execute your plan and update the progress."

async def main():
    # Parse command-line arguments
    parser = argparse.ArgumentParser(description="Run Azure SDK upgrade benchmark")
    parser.add_argument("project_path", help="Path to the project to upgrade")
    args = parser.parse_args()
    
    project_path = args.project_path
    
    # Check if copilot is available, if not install it
    try:
        subprocess.run([get_command("copilot"), "--version"], capture_output=True, check=True)
    except (subprocess.CalledProcessError, FileNotFoundError):
        print("copilot not found, installing @github/copilot...")
        subprocess.run([get_command("npm"), "install", "-g", "@github/copilot"], check=True)
    
    # Create and start client
    client = CopilotClient({
        # cli path, cross-platform
        "cli_path": get_command("copilot"),
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
    is_windows = platform.system() == "Windows"
    is_gradle = os.path.exists(os.path.join(project_path, "build.gradle")) or os.path.exists(os.path.join(project_path, "build.gradle.kts"))
    if is_gradle:
        # Use Gradle
        gradle_wrapper = os.path.join(project_path, "gradlew.bat" if is_windows else "gradlew")
        if os.path.exists(gradle_wrapper):
            build_command = [gradle_wrapper, "clean", "build"]
        else:
            build_command = [get_command("gradle"), "clean", "build"]
        build_tool_name = "Gradle"
    else:
        # Use Maven
        build_command = [get_command("mvn"), "clean", "package", "verify"]
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
