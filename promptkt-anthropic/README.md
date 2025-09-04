# Anthropic Claude API Support for PromptFx

This implementation adds support for Anthropic's Claude models in PromptFx.

## Setup

To use the Anthropic API, you need to provide your API key in one of these ways:

1. **File**: Create a file named `apikey-anthropic.txt` in the root directory containing your API key
2. **Environment Variable**: Set the `ANTHROPIC_API_KEY` environment variable

## Supported Models

The integration supports all current Claude models:

### Claude 3.5 (Latest)
- `claude-3-5-sonnet-20241022` - Most intelligent model for complex reasoning
- `claude-3-5-sonnet-20240620` - Previous version of Claude 3.5 Sonnet
- `claude-3-5-haiku-20241022` - Fast and cost-effective model

### Claude 3
- `claude-3-opus-20240229` - Most powerful model for highly complex tasks
- `claude-3-sonnet-20240229` - Balanced model for a wide range of tasks
- `claude-3-haiku-20240307` - Fastest model for simple queries

### Claude 2 (Legacy)
- `claude-2.1` - Previous generation with extended context
- `claude-2.0` - Previous generation
- `claude-instant-1.2` - Fast, affordable model

## Capabilities

The Anthropic plugin provides:

- **Text Chat**: Standard conversational interface
- **Text Completion**: Single-turn text generation
- **Vision/Multimodal**: Image understanding with Claude 3+ models
- **Large Context**: Up to 200K tokens for supported models

## Usage Example

```kotlin
import tri.ai.anthropic.*
import tri.ai.core.*

// Create client
val client = AnthropicClient()

// Text chat
val chatModel = AnthropicTextChat("claude-3-5-sonnet-20241022", client)
val messages = listOf(TextChatMessage(MChatRole.User, "Hello, Claude!"))
val response = chatModel.chat(messages, MChatVariation())

// Vision model (for images)
val visionModel = AnthropicVisionLanguageChat("claude-3-5-sonnet-20241022", client)
val visionMessages = listOf(
    VisionLanguageChatMessage(
        MChatRole.User, 
        "What do you see in this image?", 
        URI("data:image/jpeg;base64,...")
    )
)
val visionResponse = visionModel.chat(visionMessages)
```

## Integration

The plugin is automatically discovered by PromptFx through Java's ServiceLoader mechanism. No additional configuration is needed beyond providing the API key.

## API Compatibility

This implementation follows Anthropic's Messages API v2023-06-01 specification and supports:

- System messages
- Multi-turn conversations
- Image inputs (base64 encoded)
- Temperature and top-p parameters
- Custom token limits
- Stop sequences