# Tool/Agent Architecture

`promptex-pips` supports agent "planning and execution" through the `tri.ai.core.agent` and `tri.ai.core.tool` packages.

## Tools

A **tool** is an executable with metadata and structured inputs/outputs. Key interfaces include:

* `tri.ai.core.tool.Executable` defines the core unit of execution, with method signature `suspend fun execute(input: JsonNode, context: ExecContext): JsonNode`

Concrete base classes:
* `tri.ai.core.tool.ToolExecutable` — for tools that accept a plain string input and return a string result (`abstract suspend fun run(input: String, context: ExecContext): ToolExecutableResult`)
* `tri.ai.core.tool.JsonToolExecutable` — for tools with a structured JSON schema input

## Agent Flows

An **agentic flow** is a means of responding to a user input (e.g. a chat message) with a structured set of tool calls and events. Key interfaces include:

* `tri.ai.core.agent.AgentChat` provides agent chat sessions and associated logging of in-progress events via `AgentChatFlow`
  * `AgentChatFlow` wraps a `Flow<AgentChatEvent>` (`AgentChatEvent` is a typealias for `ExecEvent`), where `Flow` is a Kotlin coroutine allowing intermediate progress to be "emitted" during execution
* `tri.ai.core.agent.impl.ToolChainExecutor` (implements `AgentChat`) executes a user request using a set of tools and a "plan and execute" reasoning loop with a `TextChat` model; takes `List<Executable>` as constructor argument
* `tri.ai.core.agent.impl.JsonToolExecutor` (implements `AgentChat`) executes the same but using a model API's "tool calling" feature with a `MultimodalChat` model; takes `List<Executable>` as constructor argument
* `tri.ai.core.agent.wf` provides a more complex reasoning loop, using a set of tools along with further task planning, execution, and validation capabilities

The various execution methods above can be used via the CLI or within the PromptFx UI's *Agentic View*.

--------

# Basic Examples

## Example Tool Implementation

Tools are implemented by extending `ToolExecutable` (for simple string I/O) or `JsonToolExecutable` (for structured JSON I/O):

```kotlin
    val calculator = object : ToolExecutable("Calculator", "Use this to do math") {
        override suspend fun run(input: String, context: ExecContext) =
            ToolExecutableResult("42")
    }

    val romanizer = object : ToolExecutable("Romanizer", "Converts numbers to Roman numerals") {
        override suspend fun run(input: String, context: ExecContext) =
            ToolExecutableResult(input.toInt().let {
                when (it) {
                    42 -> "XLII"
                    84 -> "LXXXIV"
                    else -> "I don't know"
                }
            })
    }
```

## Example using `ToolChainExecutor`

The `ToolChainExecutor` uses a thought-action-observation template with a `TextChat` model:

```kotlin
    @Test
    fun testToolChain() = runBlocking {
        val tools = listOf(calculator, romanizer)
        val session = AgentChatSession()
        val executor = ToolChainExecutor(tools)
        val flow = executor.sendMessage(session,
            MultimodalChatMessage.text(MChatRole.User, "Multiply 21 times 2 and then convert it to Roman numerals."))
        flow.awaitResponseWithLogging()
    }
```

Here is a sample output:
```
INFO: User Question: Multiply 21 times 2 and then convert it to Roman numerals.
INFO: Thought: We need to first multiply 21 by 2 to get the result.
Action: Calculator
Action Input: 21 * 2
INFO: Observation: 42
INFO: Thought: Now, we need to convert 42 to Roman numerals.
Action: Romanizer
Action Input: 42
INFO: Observation: XLII
INFO: Thought: I now know the final answer
Final Answer: XLII
```

## Example using `JsonToolExecutor`

The `JsonToolExecutor` uses the model API's built-in tool calling feature with a `MultimodalChat` model. Tools with structured JSON schemas can be defined using `JsonToolExecutable`:

```kotlin
    val calcTool = object : JsonToolExecutable("calc", "Use this to do math",
        """{"type":"object","properties":{"input":{"type":"string"}}}""") {
        override suspend fun run(input: JsonNode, context: ExecContext) = "42"
    }

    val romanizeTool = object : JsonToolExecutable("romanize", "Converts numbers to Roman numerals",
        """{"type":"object","properties":{"input":{"type":"integer"}}}""") {
        override suspend fun run(input: JsonNode, context: ExecContext): String {
            val value = input["input"]?.intValue() ?: throw RuntimeException("No input")
            return when (value) {
                5 -> "V"
                42 -> "XLII"
                84 -> "LXXXIV"
                else -> "I don't know"
            }
        }
    }

    @Test
    fun testJsonTools() = runBlocking {
        val tools = listOf(calcTool, romanizeTool)
        val executor = JsonToolExecutor(tools)
        val session = AgentChatSession()
        executor.sendMessage(session,
            MultimodalChatMessage.text(MChatRole.User, "Multiply 21 times 2 and then convert it to Roman numerals.")
        ).awaitResponseWithLogging()
    }
```

Sample output:
```
INFO: User Question: Multiply 21 times 2 and then convert it to Roman numerals.
INFO: Call Function: calc with parameters {"input":"21*2"}
INFO: Result: 42
INFO: Call Function: romanize with parameters {"input":42}
INFO: Result: XLII
INFO: Final Response: 21 multiplied by 2 is 42, which is represented in Roman numerals as XLII.
```

--------

# Legacy API (through 0.12.0)

Prior to 0.12.1, `promptkt-pips` used a different tool chain API in the package `tri.ai.tools`:
- A `Tool` class (now replaced by `Executable`/`ToolExecutable`)
- `ToolChainExecutor` took a `TextChat` model directly (e.g. `ToolChainExecutor(OpenAiCompletionChat())`) and had an `executeChain(question, tools)` method
- `JsonFunctionExecutor` used OpenAI's deprecated function calling API (now replaced by `JsonToolExecutor` which uses the tool calling API)
- `JsonToolExecutor` (old) took an explicit client and model ID (e.g. `JsonToolExecutor(OpenAiClient.INSTANCE, GPT35_TURBO, tools)`)

These classes have been removed. See the current API above.
