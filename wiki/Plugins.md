# PromptFx Plugin Development

## PromptKt Model Plugins

Model plugins are part of the `promptkt` package (or could potentially in the future be located in a separate package).

### Managing Model Implementations and API Support via `TextPlugin`

Models are provided via implementations of `TextPlugin` (TBD rename), which includes support for:

* model information (`Modelinfo` interface)
* embedding models (`EmbeddingService` interface)
* chat models (`TextChat` interface)
* text completion models (`TextCompletion` interface)
* vision language models (`VisionLanguageChat` interface)
* image generation models (`ImageGenerator` interface)
* (TBD) audio, moderation, and other models

Most model interfaces are defined in the `tri.ai.core` package. A current exception is `EmbeddingService`, which is defined in `tri.ai.embedding`. These are the core interfaces standardizing use of different kinds of models within `promptkt` and `promptfx`.

For use at runtime, these plugins are registered by

* adding a line to `META-INF.services/tri.ai.core.TextPlugin` (https://github.com/aplpolaris/promptfx/blob/main/promptkt/src/main/resources/META-INF/services/tri.ai.core.TextPlugin)
* adding a `provides` clause to `module-info.java` (https://github.com/aplpolaris/promptfx/blob/main/promptkt/src/main/kotlin/module-info.java), e.g.:
```java
    provides TextPlugin with OpenAiPlugin, GeminiAiPlugin;
```

At runtime, all registered plugins can be accessed via static methods within `TextPlugin` (see https://github.com/aplpolaris/promptfx/blob/main/promptkt/src/main/kotlin/tri/ai/core/TextPlugin.kt).

### Managing Model Information using `ModelIndex` Interface

Model listings can be managed within a `ModelIndex` object, which allows for the list of available models to be configured in three ways: (i) built-in resources, (ii) runtime YAML model file in either the runtime folder or `config/` folder, e.g. `openai-models.yaml`.

These files can be configured to include both model information and the list of "active" models within the UI. For example, the OpenAI model index includes the following built-in YAML:

```yaml
models: # model detail listing
audio: [ whisper-1 ]
chat: [ gpt-3.5-turbo, gpt-4o, gpt-4-turbo, gpt-4 ]
completion: [ gpt-3.5-turbo-instruct, davinci-002, babbage-002 ]
...
```

The runtime version of this file is:

```yaml
# Use lists below to override the OpenAI API models shown in PromptFx.
# Leave a list blank to use the defaults.
---
audio: [ ]
chat: [ gpt-3.5-turbo, gpt-4o, gpt-4-turbo, gpt-4, gpt-4-turbo-preview ]
completion: [ gpt-3.5-turbo-instruct, davinci-002, babbage-002 ]
embeddings: [ ]
moderation: [ ]
tts: [ ]
image_generator: [ ]
vision_language: [ ]
```

Note that blank lists leave the list of models in the UI unchanged, while providing a list with values changes what models are available in the UI.

### Model Plugin Implementations for Common GAI/LLM APIs

Currently, the following plugins are included in this repository:

* `OpenAiPlugin`, using registry of models from `OpenAiModelIndex` (`openai-models.yaml`)
* `GeminiPlugin`, using registry of models from `GeminiModelIndex` (`gemini-models.yaml`)

## PromptFx UI Plugins

### Managing UI Models and Appearance via `PromptFxPolicy`.

For use in the UI, there is an additional layer setting a *policy* via `PromptFxPolicy`, which makes it easier to switch between different configurations of models/plugins/APIs. The policy class allows configuration of:

* available embedding, text completion, chat, vision language, and image models
* default model settings for these
* available views/tabs across the UI (via `fun supportsView(simpleName: String)`)
* optional banner, usage buttons, and API key configuration buttons for the toolbar

Currently, the UI policy is hard-coded to the `PromptFxPolicyUnrestricted` implementation, which loads models from all available `TextPlugin`s. The policy is available for use within the UI via the global object `PromptFxModels`.

### Managing UI Views via `NavigableWorkspaceView`

(TBD: rename this to `NavigableWorkspaceViewPlugin`)

PromptFx views are designed to be configured and connected independently, making it easy to add a new view to any of the existing category tabs. All views within tabs are configured and registered as instances of `NavigableWorkspaceView`. This interface defines a view's *category*, *name*, and *affordances*, along with the ability to *dock* the view within a workspace. An implementation `NavigableWorkspaceViewImpl` provides default support for docking a *tornadofx* `UIComponent`.

The `NavigableWorkspaceView` is usually quite simple, e.g. for the translation view:
```kotlin
class TranslationPlugin : NavigableWorkspaceViewImpl<TranslationView>("Text", "Translation", WorkspaceViewAffordance.INPUT_ONLY, TranslationView::class)
```
This serves primarily to determine where the view shows up in the UI (here under the `Translation` link on the `Text` tab) and specify some input/output options (affordances).

#### UI View Affordances

The following affordance options are available for views (`WorkspaceViewAffordance`):
* `NONE` - no input/output support
* `INPUT_ONLY` - accepts input text
* `COLLECTION_ONLY` - accepts input of collections
* `INPUT_AND_COLLECTION` - accepts input of both text and collections

Here, "collection" indicates a collection of text chunks, e.g. as used in the *Text Library* and *Document Q&A* views.

### Managing Standardized View Layouts with `AiTaskView`

While technically a view plugin can use any JavaFx component for its view, most views in PromptFx use the abstract class `AiTaskView` for layout consistency. This abstrac class includes designated input, output, parameters, and execution sections, each of which can be enabled/disabled as needed by sub-classes. Additional functionality is provided in the abstract classes `AiPlanTaskView` and `RuntimePromptView`. Here is a brief summary of the differences between these:

* `AiTaskView` - most general, with support for input, output, and parameters section, and *Run* button attached to an abstract `suspend fun processUserInput(): AiPipelineResult`
* `AiPlanTaskView` - implements `processUserInput()` via an abstract `fun plan(): AiPlanner`
* `RuntimePromptView` - implements `AiPlanTaskView`, with `plan()` consisting of a single text completion call where the prompt is formed from a user input string and template parameters

### Configuring `RuntimePromptView`s

`RuntimePromptView` is defined for configuration at runtime. Here is a sample entry in `views.yaml` for the translation view:
```yaml
translation:
  category: Text
  title: Translation
  description: Translate text from one language to another.
  modeOptions:
    - id: translation
      templateId: instruct
      label: Language
  promptConfig:
    id: translate-text
  isShowModelParameters: true
```

These settings are as follows:

- `category` determines the tab the view will be placed in
- `title` is the view's title
- `description` is the short description for the view shown under the title
- `modeOptions` contains a list of user-configurable parts of a prompt template
  - `id` is a unique identifier that will be cross-referenced with lists in `modes.yaml` to give the user a list of options to choose from
  - `templateId` is the mustache template parameter that will show up in the prompt
  - `label` is the UI label displayed
- `promptConfig` provides the `id` of the prompt template to be used, and an optional `isVisible` flag to hide the prompt (defaults to true)
- `isShowModelParameters` is a flag that can be used to hide model parameters (required)
- `isShowMultipleResponseOption` is an optional flag that can be used to show the multiple response option even if model parameters are not visible (defaults to false)

Here is how the above configuration shows up in the UI:

![image](https://github.com/user-attachments/assets/dfe61c89-725d-4ddd-aed7-4af0fec90f5b)

These configurations can be overridden at runtime within the `config/views.yaml` file.