# PromptFx config/plugins Folder

This folder contains plugin JAR files that should be loaded by PromptFx at runtime.

## Plugin Types

PromptFx supports two types of plugins:

### 1. API Plugins (TextPlugin)

API plugins extend PromptFx with support for additional AI model providers. They implement the `TextPlugin` interface and can provide:
- Chat models
- Text completion models  
- Embedding models
- Vision models
- Audio models

**Example:** A plugin that adds support for a custom AI API endpoint would implement `TextPlugin` and register custom model implementations.

### 2. View Plugins (NavigableWorkspaceView)

View plugins add custom UI components to the PromptFx workspace. They extend `NavigableWorkspaceViewImpl` and can provide:
- Custom workflows and interfaces
- Specialized visualizations
- Integration with external tools
- Domain-specific AI interactions

**Example:** A plugin that adds a specialized document analysis view would implement `NavigableWorkspaceView` with custom UI components.

## Installing Plugins

1. **Build** your plugin as a JAR file (or download a pre-built plugin JAR)
2. **Copy** the JAR file to this `config/plugins/` directory
3. **Restart** PromptFx

Plugins are automatically discovered at startup using Java's ServiceLoader mechanism.

## Plugin Requirements

Plugins must:
- Be packaged as standard JAR files
- Register their services via `META-INF/services/` or `module-info.java`
- Implement one of the supported plugin interfaces:
  - `tri.ai.core.TextPlugin` for API plugins
  - `tri.util.ui.NavigableWorkspaceView` for view plugins

## Example Plugins

The PromptFx repository includes two sample plugins as references:

- **promptkt-provider-sample**: Demonstrates how to create a custom `TextPlugin` to provide AI models
- **promptfx-sample-view-plugin**: Demonstrates how to create a custom `NavigableWorkspaceView` for the UI

See the README files in those modules for detailed implementation guides.

## Troubleshooting

If a plugin doesn't load:
1. Check that the JAR is in the `config/plugins/` directory
2. Verify the plugin is properly registered in `META-INF/services/` or `module-info.java`
3. Check the console output for any error messages during startup
4. Ensure the plugin was compiled against a compatible version of PromptFx