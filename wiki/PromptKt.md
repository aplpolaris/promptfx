The PromptKt library provides a number of features for working with LLM APIs, including:

- `promptkt-core` [[PromptKt-Core]]
  - uniform interfaces for working with GAI/LLM model providers and services
  - OpenAI, Gemini, and Ollama support (plus any API conforming to OpenAI API spec)
  - configurable prompt templates with [mustache](https://mustache.github.io/) support
- `promptkt-pips` [[PromptKt-Task-Pipeline]] [[PromptKt-Tool-Chain]]
  - support for [AI pipelines](https://github.com/aplpolaris/promptfx/wiki/PromptKt-Task-Pipeline) (chaining prompts and/or APIs together)
  - a basic [tool chain execution service](https://github.com/aplpolaris/promptfx/wiki/PromptKt-Tool-Chain)
- `promptkt-docs` [[Prompkt-Docs]]
  - a local embedding (vector) database
  - document chunking tools
  - document Q&A pipelines
- `promptkt-mcp` [[PromptKt-MCP]]
  - MCP client/server support and tool runtimes
- `promptkt-cli` [[Promptkt-CLI]]
  - command-line chat tools
  - command-line local document management
  - command-line prompt tools