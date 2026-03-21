`promptkt-core` contains core APIs for common generative AI functionality.

# Core APIs

## AI Models

The `AiModel` class is a common parent class for all models, including id and source parameters.

Text/chat models include:
- `TextCompletion` - legacy completions API, with a simple text input and output (some legacy OpenAI models only)
- `TextChat` - legacy chat API, with a text conversation history and a single output (for models with text-only input and output)
- `MultimodalChat` - general chat with flexible multi-part conversations and multiple input/output types (for models with more general inputs and outputs)

Special types:
- `EmbeddingModel` - calculate embedding vector from one or more text strings, with optional dimensionality parameter
- `ImageGenerator` - generates an image from text, returning image URLs
- `TextToSpeechModel` - convert text to speech audio
- `SpeechToTextModel` - transcribe audio to text

Partially supported (model type metadata exists, provider implementations may vary):
- Moderation
- Responses

Deprecated:
- `VisionLanguageChat` - allowed messages with combined images and text, replaced by `MultimodalChat`

## Model Plugins

`AiModelProvider` is a one-stop shop for models from a single provider, supporting the common types above. It also provides model metadata as `ModelInfo`.

`TextPlugin` is a deprecated typealias for `AiModelProvider`. Prefer using `AiModelProvider` directly.

## Model Metadata

APIs differ in what metadata they provide about models, in whether they include snapshot models, etc. `ModelLibrary` allows user-provided model metadata, and also user constrained lists of models. It is designed to represent a `yaml` configuration file.

`ModelIndex` is used for runtime access to the model metadata, supporting a combination of built-in and runtime model information.

## Task Traces

`AiTaskTrace` (package `tri.ai.prompt.trace`) is the unified execution trace for AI tasks. It captures everything about a single task execution — identity, model/environment configuration, inputs, execution statistics, and outputs — and is compatible with OTel/LangFuse trace concepts.

### Structure

```
AiTaskTrace
 ├── taskId: String          — unique identifier for this trace
 ├── parentTaskId: String?   — links to a parent trace for nested/agentic tasks
 ├── callerId: String?       — identifies the system component that initiated the task
 ├── env: AiEnvInfo?         — model and environment configuration
 ├── input: AiTaskInputInfo? — task input information
 ├── exec: AiExecInfo        — execution metadata (timing, errors, token usage)
 └── output: AiOutputInfo?   — task output values
```

Convenience read-only properties:
- `firstValue: AiOutput` — the first output value (throws if none)
- `values: List<AiOutput>?` — all output values, or null
- `errorMessage: String?` — error message from `exec`, if any
- `annotations: MutableMap<String, Any>` — non-serialized side-channel data (e.g. rendering hints); lost on serialization

### `AiExecInfo`

Holds execution metadata:
- `error: String?` and `throwable: Throwable?` — failure information
- `stats: Map<String, Any>` — general-purpose metrics map; standard keys are constants on the companion object (`QUERY_TOKENS`, `RESPONSE_TOKENS`, `RESPONSE_TIME_MILLIS`, `RESPONSE_TIME_MILLIS_TOTAL`, `ATTEMPTS`)
- `succeeded(): Boolean` — true if no error or throwable is set

### `AiOutputInfo` and `AiOutput`

`AiOutputInfo` is a container holding a list of `AiOutput` values. `AiOutput` is a sealed class with four subtypes:

- `AiOutput.Text(text: String)` — a plain text result
- `AiOutput.ChatMessage(message: TextChatMessage)` — a chat message result (e.g. from `TextChat`)
- `AiOutput.MultimodalMessage(message: MultimodalChatMessage)` — a multimodal message result
- `AiOutput.Other(other: Any)` — an arbitrary object result; **not serialized to JSON** (lost on persistence)

All subtypes expose `textContent()`, `imageContent()`, and `content()` for uniform access regardless of type.

### Factory Methods

`AiTaskTrace.Companion` provides convenience factories:
```kotlin
AiTaskTrace.output("some text")           // wraps a String
AiTaskTrace.output(listOf("a", "b"))      // wraps multiple strings
AiTaskTrace.outputMessage(message)        // wraps a TextChatMessage
AiTaskTrace.error(modelInfo, "msg", ex)   // creates a failed trace
AiTaskTrace.invalidRequest(modelId, "msg") // failed trace for bad model ID
```