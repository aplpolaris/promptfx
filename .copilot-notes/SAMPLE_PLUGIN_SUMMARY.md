# Sample Plugin Project - Implementation Summary

This implementation fulfills issue #501 by creating a complete sample plugin project that demonstrates how to create a custom NavigableWorkspaceView for PromptFx.

## What Was Created

### 1. Maven Module Structure
- **promptfx-sample-plugin** - New Maven module added to the parent POM
- Proper dependencies on promptfx, TornadoFX, and JavaFX
- Standard Maven project structure with src/main/kotlin and src/test/kotlin

### 2. Plugin Implementation
- **SamplePlugin** - Main plugin class extending NavigableWorkspaceViewImpl
- **SampleView** - TornadoFX-based UI view with interactive components
- Proper service registration via META-INF/services/tri.util.ui.NavigableWorkspaceView

### 3. Plugin Features
- Category: "Sample"
- Name: "Hello World"
- Affordances: INPUT_ONLY (accepts user input)
- Interactive UI with text processing demo
- Comprehensive documentation and instructions

### 4. Packaging and Deployment
- Builds to a standalone JAR: promptfx-sample-plugin-0.12.1-SNAPSHOT.jar
- Can be copied to promptfx/config/ directory for dynamic loading
- Automatically discovered by PromptFx's ServiceLoader mechanism

## How to Use

1. **Build the Plugin:**
   ```bash
   mvn package -pl promptfx-sample-plugin
   ```

2. **Deploy the Plugin:**
   ```bash
   cp promptfx-sample-plugin/target/promptfx-sample-plugin-0.12.1-SNAPSHOT.jar promptfx/config/
   ```

3. **Run PromptFx:**
   - The plugin will be automatically discovered and loaded
   - Look for "Hello World" under the "Sample" category in the UI

## Plugin Development Pattern

The sample demonstrates the standard pattern for PromptFx plugins:

1. **Extend NavigableWorkspaceViewImpl**
2. **Create a TornadoFX View class**
3. **Register via ServiceLoader**
4. **Package as JAR with dependencies**

This provides a complete template that developers can copy and modify to create their own custom views for PromptFx.

## Testing

The plugin includes unit tests that verify:
- ServiceLoader discovery works correctly
- Plugin properties are set correctly
- Plugin implements the correct interface

All tests pass successfully, confirming the plugin is properly implemented.