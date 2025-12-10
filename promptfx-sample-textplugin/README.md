# PromptFx Sample TextPlugin

This is a sample plugin demonstrating how to create a custom `TextPlugin` for PromptFx.

## Overview

This plugin shows how to:
- Create a custom AI model provider by implementing the `TextPlugin` interface
- Provide custom text completion and chat models
- Register a plugin using Java's ServiceLoader mechanism
- Package the plugin as a standalone JAR

## Plugin Structure

The sample plugin consists of:

1. **SampleTextPlugin** - The main plugin class that implements `TextPlugin`
2. **SampleTextCompletionModel** - A simple text completion model that echoes input
3. **SampleChatModel** - A simple chat model that responds to messages
4. **Service Registration** - module-info.java for automatic discovery

## Building the Plugin

```bash
# Build the sample plugin
mvn package -pl promptfx-sample-textplugin

# The JAR will be created at:
# promptfx-sample-textplugin/target/promptfx-sample-textplugin-0.13.1-SNAPSHOT.jar
```

## Installing the Plugin

1. Build the plugin JAR as shown above
2. Copy the JAR to the PromptFx config directory:
   ```bash
   cp promptfx-sample-textplugin/target/promptfx-sample-textplugin-0.13.1-SNAPSHOT.jar config/modules/
   ```
3. Start PromptFx - the plugin will be automatically discovered and loaded
4. The models will be available under the "SampleText" source in model selection dropdowns

## How It Works

### Plugin Discovery

PromptFx uses Java's ServiceLoader to automatically discover plugins at runtime. The key components are:

1. **Service Interface**: `tri.ai.core.TextPlugin`
2. **Service Registration**: `module-info.java` with `provides TextPlugin with SampleTextPlugin`
3. **Implementation**: `tri.promptfx.sample.textplugin.SampleTextPlugin`

### Plugin Implementation

```kotlin
class SampleTextPlugin : TextPlugin {
    override fun modelSource() = "SampleText"
    
    override fun modelInfo() = listOf(
        ModelInfo("sample-echo-v1", ModelType.TEXT_COMPLETION, modelSource()),
        ModelInfo("sample-chat-v1", ModelType.TEXT_CHAT, modelSource())
    )
    
    override fun chatModels() = listOf(SampleChatModel())
    
    override fun textCompletionModels() = listOf(SampleTextCompletionModel())
    
    // Other methods return empty lists for models not provided
    override fun embeddingModels() = emptyList<EmbeddingModel>()
    // ...
}
```

### Model Implementation

The plugin provides two simple models:

1. **SampleTextCompletionModel** - Echoes the input text with a prefix
2. **SampleChatModel** - Responds to chat messages by echoing them

These models are intentionally simple for demonstration purposes. Real implementations would typically:
- Make API calls to external services (like OpenAI, Gemini, etc.)
- Handle authentication and configuration
- Implement proper error handling
- Support various model parameters

## Creating Your Own Plugin

To create your own `TextPlugin`:

1. Copy this module as a template
2. Implement the `TextPlugin` interface with your custom models
3. Create model classes that implement:
   - `TextCompletion` for text completion models
   - `TextChat` for chat models
   - `EmbeddingModel` for embedding models
   - `MultimodalChat` for multimodal models
   - `VisionLanguageChat` for vision-language models
   - `ImageGenerator` for image generation models
4. Update the service registration in `module-info.java`
5. Build and deploy as a JAR to the `config/modules/` directory

The plugin will automatically appear in the PromptFx model selection dropdowns once loaded.

## Differences from NavigableWorkspaceView Plugin

This plugin differs from the `promptfx-sample-plugin` in that:

- **promptfx-sample-plugin**: Demonstrates UI extension via `NavigableWorkspaceView`
  - Adds custom views/tabs to the PromptFx UI
  - Requires JavaFX and TornadoFX dependencies
  
- **promptfx-sample-textplugin**: Demonstrates AI model provider via `TextPlugin`
  - Adds custom AI models that can be used throughout PromptFx
  - Only requires promptkt-core dependency
  - Models appear in standard model selection dropdowns

## Dependencies

The plugin depends on:
- `promptkt-core` - Core PromptKt module with TextPlugin interface
- `kotlin-stdlib` - Kotlin standard library

## Testing

Run the tests with:

```bash
mvn test -pl promptfx-sample-textplugin
```

The tests verify:
- Plugin is discoverable via ServiceLoader
- Models are properly registered
- Models execute correctly

## License

Licensed under the Apache License 2.0 - see the LICENSE file for details.
