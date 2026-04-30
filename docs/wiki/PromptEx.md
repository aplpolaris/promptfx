PromptEx modules provide an execution/service layer for AI workflows, RAG pipelines, and MCP integration, supporting various workflow, planning/reasoning, and execution models.

- `promptex-pips` provides workflow/pipeline, tool, and agent abstractions and implementations
  - support for [AI pipelines](https://github.com/aplpolaris/promptfx/wiki/PromptEx-Task-Pipeline) (chaining prompts and/or APIs together)
  - a basic [tool chain execution service](https://github.com/aplpolaris/promptfx/wiki/PromptEx-Tool-Chain)
- `promptex-docs` enables document management and RAG pipelines [[PrompEx-Docs]]
  - a local embedding (vector) database
  - document chunking tools
  - document Q&A pipelines
- `promptex-mcp` adds support for MCP (Model Context Protocol) server and client implementations [[PromptEx-MCP]]
  - MCP client/server support and tool runtimes