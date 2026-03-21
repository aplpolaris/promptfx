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

## Prompt Templates and the Prompt Library

Prompt templates and their library are in the package `tri.ai.prompt`.

### `PromptTemplate`

`PromptTemplate` wraps a [Mustache](https://mustache.github.io/) template string. It can be constructed directly from a string and filled with named fields:

```kotlin
val template = PromptTemplate("Summarize the following: {{{input}}}")

// fill with named fields
val prompt = template.fill("input" to "The quick brown fox...")

// shorthand for a single 'input' field
val prompt = template.fillInput("The quick brown fox...")

// shorthand for 'input' and 'instruct' fields
val prompt = template.fillInstruct(input = "The quick brown fox...", instruct = "Use bullet points.")
```

By default, `{{today}}` is automatically injected with the current ISO-8601 date. This can be disabled by setting `injectToday = false`.

`findFields()` returns the list of all Mustache field names found in the template.

Standard field name constants are defined on `PromptTemplate.Companion`: `INPUT`, `INSTRUCT`, `TODAY`.

### `PromptDef`

`PromptDef` is the serializable descriptor for a single prompt template. Key fields:

- `id: String` — canonical identifier, optionally versioned: `group/name[@MAJOR.MINOR.PATCH]`
- `template: String?` — the Mustache template string
- `args: List<PromptArgDef>` — declared argument contract (name, type, description, required, defaultValue); auto-generated from template fields if omitted
- `category`, `tags`, `name`, `title`, `description`, `version` — metadata for discoverability
- `contextInject: ContextConfig?` — rendering hints, e.g. `today: true` to auto-inject the date

`bareId` returns the id without any version suffix. `title()` returns the best available display title.

Extension functions on `PromptDef` (from `PromptTemplate.kt`):
- `fun PromptDef.template(): PromptTemplate` — creates a `PromptTemplate` from the def
- `fun PromptDef.fill(vararg fields: Pair<String, Any>): String` — fills the template directly

### `PromptGroup`

`PromptGroup` represents a collection of related `PromptDef`s loaded from a single YAML file. Fields:

- `groupId: String` — identifier for the group (defaults to `"Uncategorized"`)
- `prompts: List<PromptDef>` — the prompts in the group
- `defaults: PromptGroupDefaults` — default `category` and `tags` applied to all prompts in the group
- `version: String?` — default version applied to prompts that don't specify one

Calling `resolved()` on a `PromptGroup` propagates defaults into each `PromptDef` (category, name, version, tags, args) and is called automatically on load.

### YAML File Format

Prompt groups are defined in YAML files. Example:

```yaml
groupId: text-summarize
defaults:
  category: text
prompts:
  - id: text-summarize/summarize@1.0.0
    title: Summarize Text
    description: Summarize the provided text.
    args:
      - name: input
        required: true
        type: string
      - name: audience
        required: false
        type: string
    template: |
      Summarize the following{{#audience}}, written for {{audience}}{{/audience}}:
      ```
      {{{input}}}
      ```
```

The `id` format is `group/name@version`. Triple-brace `{{{field}}}` suppresses HTML escaping; double-brace `{{field}}` escapes HTML. Mustache sections (`{{#field}}...{{/field}}`) can be used to conditionally include content when a field is non-empty.

### `PromptLibrary`

`PromptLibrary` manages a collection of `PromptDef`s indexed by id, bare id, category, and tag.

```kotlin
// get the shared default instance (built-in prompts + prompts/ directory)
val prompt = PromptLibrary.INSTANCE.get("text-summarize/summarize")

// get only runtime-loaded prompts (from prompts/ directory)
val prompt = PromptLibrary.RUNTIME_INSTANCE.get("my-group/my-prompt")

// load from a specific path
val lib = PromptLibrary.loadFromPath("path/to/prompts/")

// list prompts filtered by category or tag
val prompts = PromptLibrary.INSTANCE.list(category = "text")
```

Key methods:
- `get(idOrBare): PromptDef?` — look up by exact versioned id or bare id (returns the first/latest version)
- `list(category, tag, prefix): List<PromptDef>` — list all prompts, optionally filtered
- `addGroup(group)` / `addPrompt(prompt)` — add prompts to an existing library instance
- `refreshRuntimePrompts()` — reloads both the default and runtime instances from disk

At startup, `PromptLibrary.INSTANCE` loads built-in prompts from the classpath (under `tri/ai/prompt/resources/`) and also scans the `prompts/` directory in the working directory for user-defined YAML files.

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
