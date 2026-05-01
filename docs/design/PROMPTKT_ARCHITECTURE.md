# promptkt Architecture

Core Kotlin library for AI model abstractions, provider plugin registration, and execution traces.
Package namespace: `tri.ai.*`

---

## Model Interfaces

All model types extend `AiModel` (`tri.ai.core.AiModel`):
- `val modelId: String`
- `val modelSource: String` (e.g. "OpenAI", "Gemini")

| Interface | File | Purpose |
|-----------|------|---------|
| `TextCompletion` | `tri/ai/core/TextCompletion.kt` | Single-turn text completion |
| `TextChat` | `tri/ai/core/TextChat.kt` | Multi-turn chat with message history |
| `MultimodalChat` | `tri/ai/core/MultimodalChat.kt` | Chat with text/image/audio parts |
| `EmbeddingModel` | `tri/ai/core/EmbeddingModel.kt` | Batch text → embedding vectors |
| `ImageGenerator` | `tri/ai/core/ImageGenerator.kt` | Text-to-image generation |
| `TextToSpeechModel` | `tri/ai/core/TextToSpeechModel.kt` | Text → audio |
| `SpeechToTextModel` | `tri/ai/core/SpeechToTextModel.kt` | Audio → text |

### Key signatures

```kotlin
// TextCompletion
suspend fun complete(text: String, variation: MChatVariation = MChatVariation(),
    tokens: Int? = 1000, stop: List<String>? = null, numResponses: Int? = 1): AiPromptTrace

// TextChat
suspend fun chat(messages: List<TextChatMessage>, variation: MChatVariation = MChatVariation(),
    tokens: Int? = 1000, stop: List<String>? = null, numResponses: Int? = null,
    requestJson: Boolean? = null): AiPromptTrace

// MultimodalChat
suspend fun chat(messages: List<MultimodalChatMessage>,
    parameters: MChatParameters = MChatParameters()): AiPromptTrace

// EmbeddingModel
suspend fun calculateEmbedding(text: List<String>,
    outputDimensionality: Int? = null): List<List<Double>>
```

### Message types

**`TextChatMessage`** — `role: MChatRole`, `content: String?`
Roles: `System`, `User`, `Assistant`, `Tool`, `None`

**`MultimodalChatMessage`** — `role: MChatRole`, `content: List<MChatMessagePart>?`, `toolCalls`, `toolCallId`
Part types: `TEXT`, `IMAGE`, `AUDIO`, `TOOL_CALL`, `TOOL_RESPONSE`
Factories: `user()`, `text()`, `imageUrl()`, `imageBase64()`, `audio()`, `tool()`

**`MChatVariation`** — `seed`, `temperature`, `topP`, `topK`, `frequencyPenalty`, `presencePenalty`
Helper: `MChatVariation.temp(temp: Double?)`

---

## Provider Plugin Pattern

Providers implement `AiModelProvider` (`tri/ai/core/AiModelProvider.kt`) and are discovered via Java `ServiceLoader`:

```
META-INF/services/tri.ai.core.AiModelProvider
```

### Required methods

```kotlin
fun isApiConfigured(): Boolean
fun modelSource(): String
fun modelInfo(): List<ModelInfo>
fun chatModels(): List<TextChat>
fun embeddingModels(): List<EmbeddingModel>
fun multimodalModels(): List<MultimodalChat>
fun textCompletionModels(): List<TextCompletion>
fun imageGeneratorModels(): List<ImageGenerator>
fun close()
```

Optional: `textToSpeechModels()`, `speechToTextModels()`

### Singleton access

`AiModelProvider.Companion` aggregates all loaded providers:

```kotlin
AiModelProvider.chatModels()            // all chat models across providers
AiModelProvider.chatModel(modelId)      // lookup by ID
AiModelProvider.embeddingModel(modelId) // lookup by ID
AiModelProvider.defaultPlugin           // first "OpenAI" provider or first found
```

Plugin JARs are loaded from `config/plugins/*.jar` at startup via `URLClassLoader`.
Use `promptkt-provider-sample` as the template for new providers.

---

## Result & Trace Types

All model calls return `AiPromptTrace` (alias for `AiTaskTrace`).

### `AiTaskTrace` (`tri/ai/prompt/trace/AiTaskTrace.kt`)

| Field | Type | Purpose |
|-------|------|---------|
| `taskId` | `String` | UUID per execution |
| `parentTaskId` | `String?` | Links nested traces |
| `env` | `AiEnvInfo?` | Model/provider config |
| `input` | `AiTaskInputInfo?` | Inputs passed to model |
| `exec` | `AiExecInfo` | Timing, token counts, error |
| `output` | `AiOutputInfo?` | Results |

Key accessors: `firstValue: AiOutput`, `errorMessage: String?`, `values: List<AiOutput>?`

### `AiExecInfo` (`tri/ai/prompt/trace/AiExecInfo.kt`)

```kotlin
fun succeeded(): Boolean   // false if error or throwable set
val error: String?
val stats: Map<String, Any>  // keys: QUERY_TOKENS, RESPONSE_TOKENS,
                             //       RESPONSE_TIME_MILLIS, ATTEMPTS
```

### `AiOutput` (`tri/ai/prompt/trace/AiOutput.kt`)

Sealed class — use `textContent(ifNone)` for safe extraction:

| Subtype | Content |
|---------|---------|
| `Text` | Plain string |
| `ChatMessage` | `TextChatMessage` |
| `MultimodalMessage` | `MultimodalChatMessage` |
| `Other` | Embeddings / structured data (not JSON-persisted) |

---

## Common Pitfalls

- **Never instantiate models directly** — always fetch via `AiModelProvider` companion methods.
- **Check `exec.succeeded()`** before reading output — model calls return traces even on error.
- **`Other` outputs are not serialized** — don't store embeddings in `AiTaskTrace` expecting JSON round-trip.
- **`modelId` ≠ display name** — use `parseModelId("id [source]")` to split composite IDs.
