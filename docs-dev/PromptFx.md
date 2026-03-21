# PromptFx Runtime Capabilities

## OpenAI API Configuration (e.g. Ollama)

In versions 0.11.1+, you can configure PromptFx to use arbitrary APIs that are OpenAI API compatible, such as [Ollama](https://ollama.com/).
Update `config\openai-api-config.yaml` as follows:

```yaml
# Library of OpenAI API compatible endpoints
---
endpoints: 
  - source: Ollama
    settings:
      baseUrl: "http://localhost:11434/v1/"
      logLevel: None
      timeoutSeconds: 120
    modelFileName: ollama-models.yaml
```

You'll also need to indicate your local model names in `config/ollama-models.yaml`:

```yaml
# Library of Ollama Models
---
audio: [ ]
chat: [ llama3.1 ]
completion: [ ]
embeddings: [ nomic-embed-text ]
moderation: [ ]
multimodal: [ ]
tts: [ ]
image_generator: [ ]
vision_language: [ ]
```

Then run PromptFx and you should see the models you specified available in the main model dropdowns.

<img width="320" height="325" alt="image" src="https://github.com/user-attachments/assets/9a2ab98f-7a99-40c8-9743-1363a4a21f66" />

## Configuring Views at Runtime

PromptFx views can be configured entirely at runtime, if they are limited to a single prompt with text input and output. To do this, modify the files `views.yaml`, `prompts.yaml`, and `modes.yaml` in the runtime `config\` folder. (Versions of these yaml files are also included in the codebase, but the runtime versions will add to and/or replace the default configurations.)

For example, let's say we want to build a view around a prompt for extracting dates. Add the following to `prompts.yaml`:
```yaml
date-extraction: |
  Extract a list of dates from the following text, and format as {{DATE_FORMAT}}.
  '''
  {{{input}}}
  '''
```

Here is how to add a simple view to extract a list of dates from input text. Add the following to `views.yaml`. Note that the `id` under `promptConfig` should match the key used in the prompts file.
```yaml
date-extraction:
  category: Text
  title: Date Extraction
  description: Extract all dates from input text and provide as a list.
  modeOptions:
    - id: date-formats
      templateId: DATE_FORMAT
      label: Date Format
  promptConfig:
    id: date-extraction
  isShowModelParameters: false
  isShowMultipleResponseOption: false
```
For each option listed under `modeOptions`, add a corresponding entry to `modes.yaml` to provide the list of options for users to pick from for dates (they will also be able to edit the option in the UI at runtime). Here the value can be a list, or a map of key-value pairs (second example below). The key must match the `id` under `modeOptions`.
```yaml
date-formats:
  - ISO8601 Date
  - MMM dd, yyyy
  - yyyy/MM/dd
  - the 12th of February

date-formats-as-map:
  "ISO8601": ISO8601 Date
  "Standard": MMM dd, yyyy
  "Year First": yyyy/MM/dd
  "Long form": the 12th of February
```

Running PromptFx with this configuration, the following view is added under the `Text` tab:

![image](https://github.com/user-attachments/assets/338c6cc3-be10-4fa5-a688-9c012f82970e)

> You can also use a new category, and a new tab will be added to the navigation bar.