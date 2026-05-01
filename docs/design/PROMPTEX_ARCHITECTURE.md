# promptex Architecture

Extended execution modules built on promptkt: task pipelines, RAG/document Q&A, and MCP integration.
Package namespace: `tri.ai.*`

---

## promptex-pips — Task Pipeline

### Flow

```
AiTaskBuilder<T>  →  AiWorkflow  →  AiWorkflowExecutor  →  AiWorkflowResult
      |                                      |
  chain .task()                    RetryExecutor per task
```

### Core types

| Type | File | Purpose |
|------|------|---------|
| `AiTask<I, O>` | `tri/ai/pips/AiTask.kt` | Single executable unit |
| `AiTaskBuilder<T>` | `tri/ai/pips/AiTaskBuilder.kt` | Typed chain of tasks |
| `AiWorkflow<I, O>` | `tri/ai/pips/AiWorkflow.kt` | Named group of tasks as a single task |
| `AiWorkflowExecutor` | `tri/ai/pips/AiWorkflowExecutor.kt` | Runs a task list with dependency resolution |
| `RetryExecutor` | `tri/ai/pips/RetryExecutor.kt` | Wraps task execution with retry + backoff |
| `ExecContext` | `tri/ai/core/tool/ExecContext.kt` | Shared state, scratchpad, traces |
| `AiWorkflowResult` | `tri/ai/pips/AiWorkflowResult.kt` | Final + interim task traces |

### `AiTask`

```kotlin
abstract suspend fun execute(input: I, context: ExecContext): O
```

Factory shorthand:
```kotlin
AiTask.task(id = "myTask") { ctx -> /* return O */ }
```

Attach monitoring without changing the task:
```kotlin
task.monitor { outputs -> ... }
task.monitorTrace { trace -> ... }
```

### `AiTaskBuilder`

Type-safe chaining — each `.task()` call returns a new builder preserving the output type:

```kotlin
AiTaskBuilder<String>
    .task("step2") { str, ctx -> str.length }   // → AiTaskBuilder<Int>
    .task("step3") { n, ctx -> n * 2 }           // → AiTaskBuilder<Int>
```

Aggregate parallel tasks:
```kotlin
listOf(taskA, taskB, taskC).aggregate()          // → AiTaskBuilder<List<T>>
```

Map over a collection:
```kotlin
builderOfList.taskOnEach("perItem") { item, ctx -> transform(item) }
```

Convert to workflow:
```kotlin
builder.asWorkflow(id = "myWorkflow")
```

### `AiWorkflowExecutor`

```kotlin
AiWorkflowExecutor.execute(task, context)        // single task
AiWorkflowExecutor.execute(listOf(...), context) // list with dependency resolution
```

Execution loop: filters tasks whose dependencies succeeded → executes via `RetryExecutor` → stores output in `context` → logs trace → emits monitor events. Repeats until no runnable tasks remain.

### `ExecContext`

```kotlin
// resources — shared singletons (embedding index, model refs)
ctx.putResource("embeddingIndex", index)
ctx.resource("embeddingIndex", LocalFolderEmbeddingIndex::class.java)

// scratchpad — task outputs keyed by task ID
ctx.put("step1", result)
ctx.get("step1")

// traces — execution records
ctx.logTrace("step1", trace)
ctx.trace("step1")

// child context inherits resources but has isolated scratchpad/traces
val child = ctx.childContext()
```

### `AiWorkflowResult`

```kotlin
result.finalResult          // AiTaskTrace of the last task
result.interimResults       // Map<taskId, AiTaskTrace>
result.finalResult.exec.succeeded()  // check for error
```

Factories: `AiWorkflowResult.error(message, throwable)`, `AiWorkflowResult.todo()`

### `RetryExecutor`

Default: 3 retries, 1 s initial delay, 1.5× backoff. Stats stored in `AiExecInfo`:
`ATTEMPTS`, `RESPONSE_TIME_MILLIS` (last attempt), `RESPONSE_TIME_MILLIS_TOTAL` (all attempts).

---

## promptex-docs — RAG Pipeline

### Flow

```
documents
    ↓  TextChunker          split into TextChunk list
    ↓  EmbeddingModel       embed each chunk
    ↓  EmbeddingIndex       store + query by cosine similarity
    ↓  DocumentQaDriver     retrieve top-K chunks → build context → TextChat answer
```

### Core types

| Type | File | Purpose |
|------|------|---------|
| `TextChunker` | `tri/ai/text/chunks/TextChunker.kt` | Split text into chunks |
| `TextChunk` | `tri/ai/text/chunks/TextChunk.kt` | A single text segment with attributes |
| `EmbeddingStrategy` | `tri/ai/embedding/EmbeddingStrategy.kt` | Model + chunker pair |
| `EmbeddingIndex` | `tri/ai/embedding/EmbeddingIndex.kt` | Store and semantic-search chunk embeddings |
| `EmbeddingMatch` | `tri/ai/embedding/EmbeddingMatch.kt` | Query result: chunk + score |
| `DocumentQaDriver` | `tri/ai/text/docs/DocumentQaDriver.kt` | Full Q&A lifecycle interface |
| `LocalDocumentQaDriver` | `tri/ai/text/docs/LocalDocumentQaDriver.kt` | Filesystem-backed implementation |
| `DocumentQaPlanner` | `tri/ai/text/docs/DocumentQaPlanner.kt` | Builds the Q&A task pipeline |

### Key signatures

```kotlin
// TextChunker
fun chunkText(text: String, maxChunkSize: Int): List<TextChunk>
// Implementations: SmartTextChunker, DelimiterTextChunker, RegexTextChunker

// EmbeddingIndex
suspend fun findMostSimilar(query: String, n: Int): List<EmbeddingMatch>
var onProgress: ((String, Double) -> Unit)?   // progress callback

// DocumentQaDriver
suspend fun answerQuestion(input: String, numResponses: Int = 1,
    historySize: Int = 1, context: ExecContext = ExecContext()): AiWorkflowResult
```

### `EmbeddingStrategy`

Bundles model + chunker:
```kotlin
EmbeddingStrategy(model = embeddingModel, chunker = SmartTextChunker())
```

### `DocumentQaPlanner`

Constructs a typed `AiTaskBuilder` with three tasks:
1. Load embedding index from disk
2. Semantic search → `List<EmbeddingMatch>`
3. Build prompt context from top-K chunks → `TextChat` call → `QuestionAnswerResult`

Use `LocalDocumentQaDriver` rather than constructing `DocumentQaPlanner` directly unless customizing the pipeline.

---

## promptex-mcp — Model Context Protocol

### Roles

| Type | File | Purpose |
|------|------|---------|
| `McpProvider` | `tri/ai/mcp/McpProvider.kt` | Client interface to an MCP server |
| `McpProviderRegistry` | `tri/ai/mcp/McpProviderRegistry.kt` | Registry of active MCP providers |
| `McpProviderEmbedded` | `tri/ai/mcp/McpProviderEmbedded.kt` | In-process MCP provider |
| `McpProviderHttp` | `tri/ai/mcp/McpProviderHttp.kt` | HTTP-based MCP provider |
| `McpProviderStdio` | `tri/ai/mcp/McpProviderStdio.kt` | stdio-based MCP provider |
| `McpTool` | `tri/ai/mcp/tool/McpTool.kt` | A callable tool exposed by a provider |
| `McpToolLibrary` | `tri/ai/mcp/tool/McpToolLibrary.kt` | Collection of tools from one provider |
| `McpResource` | `tri/ai/mcp/McpResource.kt` | A resource exposed by a provider |
| `McpPrompt` | `tri/ai/mcp/McpPrompt.kt` | A prompt template exposed by a provider |

### Server implementations

- `McpServerStdio` / `McpServerHttp` — embed an MCP server in-process (stdio or HTTP transport)
- Transport entry points: `McpServerStdioMain`, `McpServerHttpMain`

### Common pitfalls

- **Task dependencies must be explicit** — `AiWorkflowExecutor` uses `dependencies: Set<String>` on each task, not execution order. Tasks with unmet dependencies are silently skipped.
- **Inner workflow failure propagates** — `AiWorkflow` throws `IllegalStateException` on inner failure; the outer executor catches it and marks the workflow task as failed.
- **`ExecContext` scratchpad is not thread-safe** — do not share a single context across concurrent coroutines; use `childContext()` for sub-workflows.
- **Chunk embeddings are not free** — `EmbeddingIndex` calls the embedding model for every new/changed chunk. Cache the index to disk (`LocalFolderEmbeddingIndex`) rather than rebuilding per request.
