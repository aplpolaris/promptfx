# PromptFx Sample Plugin

This is a sample plugin demonstrating how to create a custom `NavigableWorkspaceView` for PromptFx.

## Overview

This plugin shows how to:
- Create a custom view that integrates with the PromptFx workspace
- Implement the `NavigableWorkspaceView` interface
- Register a plugin using Java's ServiceLoader mechanism
- Package the plugin as a standalone JAR

## Plugin Structure

The sample plugin consists of:

1. **SamplePlugin** - The main plugin class that extends `NavigableWorkspaceViewImpl`
2. **SampleView** - The actual UI view that extends TornadoFX's `View`
3. **Service Registration** - META-INF/services file for automatic discovery

## Building the Plugin

```bash
# Build the sample plugin
mvn package -pl promptfx-sample-view-plugin

# The JAR will be created at:
# promptfx-sample-view-plugin/target/promptfx-sample-view-plugin-0.13.1-SNAPSHOT.jar
```

## Installing the Plugin

1. Build the plugin JAR as shown above
2. Copy the JAR to the PromptFx config directory:
   ```bash
   cp promptfx-sample-view-plugin/target/promptfx-sample-view-plugin-0.13.1-SNAPSHOT.jar promptfx/config/
   ```
3. Start PromptFx - the plugin will be automatically discovered and loaded
4. Look for "Hello World" under the "Sample" category in the UI

## How It Works

### Plugin Discovery

PromptFx uses Java's ServiceLoader to automatically discover plugins at runtime. The key components are:

1. **Service Interface**: `tri.util.ui.NavigableWorkspaceView`
2. **Service Registration**: `META-INF/services/tri.util.ui.NavigableWorkspaceView`
3. **Implementation**: `tri.promptfx.sample.SamplePlugin`

### Plugin Implementation

```kotlin
class SamplePlugin : NavigableWorkspaceViewImpl<SampleView>(
    "Sample",           // Category name
    "Hello World",      // View name
    WorkspaceViewAffordance.INPUT_ONLY,  // Capabilities
    SampleView::class   // View class
)
```

### View Implementation

The view extends TornadoFX's `View` class and provides the UI:

```kotlin
class SampleView : View("Sample Plugin Demo") {
    override val root = vbox {
        // UI components here
    }
}
```

## Creating Your Own Plugin

To create your own plugin:

1. Copy this module as a template
2. Modify the plugin and view classes
3. Update the service registration file
4. Build and deploy as a JAR

The plugin will automatically appear in the PromptFx UI once the JAR is placed in the config directory.

## Dependencies

The plugin depends on:
- `promptfx` - Main PromptFx module
- `tornadofx` - JavaFX UI framework
- `javafx` - JavaFX controls and base classes

## License

Licensed under the Apache License 2.0 - see the LICENSE file for details.