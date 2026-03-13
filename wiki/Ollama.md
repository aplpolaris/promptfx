> THIS IS A DRAFT PAGE FOR AN UPCOMING FEATURE.

# Using PromptFx with Ollama

With versions 0.11.0 and later, you can configure OpenAI compatible API endpoints at runtime, allowing PromptFx to be used with [Ollama](https://ollama.com/) and other APIs.

These APIs are configured in `openai-api-config.yaml`, which should be placed in the `config\` directory. Here is a sample configuration of the default OpenAI API endpoint:
```yaml
# Library of OpenAI API compatible endpoints
---
endpoints:
  - source: OpenAI (pluggable config)
    settings:
      baseUrl: https://api.openai.com/v1/
      apiKeyFile: apikey.txt
      apiKeyRegistry: OPENAI_API_KEY
      logLevel: None
      timeoutSeconds: 60
    modelFileName: openai-models.yaml
```

For example, here's the Ollama setup for an LLM and embedding model:
```bash
ollama pull llama3.1
ollama pull nomic-embed-text
ollama serve
```

Here's the associated configuration in `openai-api-config.yaml`:
```yaml
endpoints:
  - source: Ollama
    settings:
      baseUrl: "http://localhost:11434/v1/"
      logLevel: None
      timeoutSeconds: 60
    modelFileName: ollama-models.yaml
```

Here's the model file in `ollama-models.yaml`:
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

Once configured properly, the models will appear as options within the UI. In addition, all models downloaded with Ollama will be shown in the `Models` tab.