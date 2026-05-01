# promptrt Architecture

CLI runtime module built on promptkt and promptex. Provides runnable entry points and a memory service.
Package namespace: `tri.ai.cli`, `tri.ai.memory`

---

## promptrt-cli — CLI Entry Points

| Class | Purpose |
|-------|---------|
| `SimpleChatCli` | Interactive single-turn chat session |
| `MemoryChatCli` | Chat session with persistent `BotMemory` |
| `AgentChatCli` | Agentic chat with tool-calling loop |
| `DocumentCli` | Document Q&A via `LocalDocumentQaDriver` |
| `McpCli` | MCP server/tool interaction from the command line |
| `PromptBatchRunner` | Run a prompt template against a batch of inputs |

These are thin wrappers — they configure a model via `AiModelProvider`, build an `ExecContext`, and delegate to promptkt/promptex primitives.

---

## Memory Service

| Class | File | Purpose |
|-------|------|---------|
| `BotMemory` | `tri/ai/memory/BotMemory.kt` | Persistent store of `MemoryItem` entries for a session |
| `BotPersona` | `tri/ai/memory/BotPersona.kt` | System prompt / personality config for a bot |
| `MemoryItem` | `tri/ai/memory/MemoryItem.kt` | A single remembered fact or event |
| `MemoryService` | `tri/ai/memory/MemoryService.kt` | Read/write/search memory items |

`BotMemory` is injected into `MemoryChatCli` and `AgentChatCli` to maintain conversation context across turns. It is not used in the promptfx UI layer — the UI manages its own chat history via `AiTaskTraceHistoryModel`.
