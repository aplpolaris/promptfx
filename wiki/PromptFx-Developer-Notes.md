# Package Overview

- `promptfx-meta` (maven multi-module project)
  - `promptkt-core` - LLM and prompt engineering, core API definitions.
  - **PROVIDER PLUGINS**
    - `promptkt-openai` (`OpenAiPlugin`, `OpenAiApiPlugin`) - OpenAI plugin for prompt engineering and LLM interactions, also supports alternate APIs compatible with the OpenAI API.
    - `promptkt-gemini` (`GeminiAiPlugin`) - Gemini AI plugin for prompt engineering and LLM interactions.
    - `promptkt-gemini-sdk` (`GeminiSdkPlugin`) - Gemini AI plugin using Google's official Java SDK for prompt engineering and LLM interactions.
  - **WORKFLOWS**
    - `promptkt-pips` - Agent, tool, and pipeline logic.
    - `promptkt-docs` - Document management and RAG pipelines.
  - **TOOLING**
    - `promptkt-mcp` - MCP (Model Context Protocol) server and client implementations.
    - `promptkt-cli` - Command-line utilities for working with AI and AI pipelines.
  - **UI**
    - `promptfx` - LLM demo application.
    - `promptfx-sample-plugin` - Sample plugin demonstrating NavigableWorkspaceView.

# Building PromptFx

PromptFx uses the [Java](https://www.oracle.com/corporate/features/understanding-java-9-modules.html) [module system](https://www.baeldung.com/java-9-modularity), which has some advantages but can complicate the build process. Some common issues and fixes are described below.

## IntelliJ

IntelliJ run/debug configurations (in the `Run/Edit Configurations...` dialog) may need to be modified to run/debug PromptFx.

When running the PromptFx application within IntelliJ, you'll need to add a few VM options to explicitly open modules for use with `TornadoFx` (`--add-opens javafx.graphics/javafx.scene=tornadofx,javafx.graphics/javafx.scene=tornadofx`):

- ``--add-opens javafx.graphics/javafx.scene=tornadofx``
- ``--add-opens javafx.controls/javafx.scene.control.skin=tornadofx``
- ``--add-reads kotlin.stdlib=kotlinx.coroutines.core``

![image](https://github.com/aplpolaris/promptfx/assets/13057929/d588915c-a8bf-47d7-8b34-b1889fe6ba38)

## Running Tests

When running tests within IntelliJ, you may need to add an `--add-reads kotlin.stdlib=kotlinx.coroutines.core`:

![image](https://github.com/aplpolaris/promptfx/assets/13057929/ad38cf1b-f68b-403f-9efe-550457cb40b2)

In some cases, use of test jars may cause tests to fail (e.g. `MultimodalChatTestKt` class not found exceptions). To fix this in IntelliJ:
- Open `Edit Configurations...` dialog
- At the left, click the blue link that says `Edit configuration templates...`
- Select JUnit, then click the blue link that says `Modify options`
- Make sure the `Do not use --module-path option` is enabled

PromptFx uses [JUnit5 Tags](https://www.baeldung.com/junit-filtering-tests) to customize test execution. In IntelliJ, you can selectively execute integration tests using the `Tags` configuration option in the `Run/Debug Configurations` dialog:

![image](https://github.com/user-attachments/assets/2cc67364-5f77-40a0-bf1c-958bae7603f6)

By default, tests tagged with `gemini` and `openai` are excluded when running `mvn test` from the command-line. (Working on ways to run groups individually from the command-line but for no swap out the `<excludedGroups>` portion of the `maven-surefire-plugin` in the promptkt pom file.)

## Platform-Specific Builds

The distribution project (`promptfx-distribution`) builds distribution zips for PromptFx using Gradle.
Use the platform-specific release profiles `windows macos mac64 linux` to create platform-specific jars and executables, e.g.

```bash
mvn package -Plinux -DskipTests
```

> Before performing a platform-specific build, make sure you've set the right version in `build.gradle.kts`.

# Performing Releases

## Performing Releases for Maven Central

1. Ensure all pull requests and code changes are complete, and all tests functional.
2. Build and release `promptfx` meta-project:
```bash
mvn release:prepare
mvn release:perform -Pwindows
```
3. Navigate to https://central.sonatype.com/ and login, then https://central.sonatype.com/publishing and publish the validated artifacts.
4. After a half hour or so they should be available at https://central.sonatype.com/namespace/com.googlecode.blaisemath, e.g. https://central.sonatype.com/artifact/com.googlecode.blaisemath/promptfx

## Creating Platform-Specific Artifacts

The `promptfx` sub-module has platform-specific release profiles, allowing for custom executable jars to be built using maven. To prepare these:

1. Checkout the promptfx tag created during release (e.g. `git checkout promptfx-meta-0.12.0`) -- or find this tag in the `target/checkout/` folder after performing the release.
2. Navigate to the `promptfx/` module folder and run platform-specific packaging:
```bash
for PLATFORM in windows macos mac64 linux; do
     mvn install -P$PLATFORM,release -DskipTests;
done
```

> This installs the artifacts `promptfx-0.12.0-windows.jar`, etc. to your local maven repository. We'd like to modify this to deploy platform-specific jars to maven central in the future.

Then, in the `promptfx-distribution` folder, construct the distribution zips (locally):

1. Change the versions in `build.gradle.kts` to match the artifacts just created.
2. Run the distribution script:
```bash
cd promptfx-distribution
./gradlew zipAll
```