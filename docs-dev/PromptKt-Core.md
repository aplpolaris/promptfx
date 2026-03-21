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