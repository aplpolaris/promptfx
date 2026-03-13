# Updated Tool/Agent Architecture (0.12.1+)

`promptkt-pips` supports agent "planning and execution" through the `tri.ai.core.agent` and `tri.ai.core.tool` packages.

## Tools

A **tool** is an executable with metadata and structured inputs/outputs. Key interfaces include:

* `tri.ai.core.tool.Executable` defines the core unit of execution, with method signature `fun execute(input: JsonNode, context: ExecContext): JsonNode`

## Agent Flows

An **agentic flow** is a means of responding to a user input (e.g. a chat message) with a structured set of tool calls and events. Key interfaces include:

* `tri.ai.core.agent.AgentChat` provides agent chat sessions and associated logging of in-progress events via `AgentChatFlow`
  * `AgentChatFlow` provides a `Flow<AgentChatEvent>`, where `Flow` is a kotlin coroutine allowing intermediate progress to be "emitted" during execution
* `tri.ai.core.agent.impl.ToolChainExecutor` (implements `AgentChat`) executes a user request using a set of tools and a "plan and execute" reasoning loop with a chat model
* `tri.ai.core.agent.impl.JsonToolExecutor` (implements `AgentChat`) executes the same, but using a model API's "tool calling" feature
* `tri.ai.core.agent.wf` provides a more complex reasoning loop, using a set of tools along with further task planning, execution, and validation capabilities

The various execution methods above can be used via the CLI or within the PromptFx UI's *Agentic View*.

--------

# Previous Version (through 0.12.0)

`promptkt-pips` supports "planning and execution" chains through tool selection and use in the package `tri.ai.tools`.

# Basic Example

## Example Tool Use

The `ToolChainExecutor` uses a thought-action-observation template:

```kotlin
    @Test
    fun testTools() {
        OpenAiClient.INSTANCE.settings.logLevel = LogLevel.None

        val tool1 = object : Tool("Calculator", "Use this to do math") {
            override suspend fun run(input: String) = "42"
        }
        val tool2 = object : Tool("Romanizer", "Converts numbers to Roman numerals") {
            override suspend fun run(input: String) = input.toInt().let {
                when (it) {
                    42 -> "XLII"
                    84 -> "LXXXIV"
                    else -> "I don't know"
                }
            }
        }

        ToolChainExecutor(OpenAiCompletionChat())
            .executeChain("Multiply 21 times 2 and then convert it to Roman numerals.", listOf(tool1, tool2))
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

## Example using `JsonFunctionExecutor`

The `JsonFunctionExecutor` uses OpenAI's (deprecated) [function API](https://platform.openai.com/docs/api-reference/chat/create#chat-create-functions).

```kotlin
    @Test
    fun testTools() {
        val SAMPLE_TOOL1 = tool("calc", "Use this to do math",
            """{"type":"object","properties":{"input":{"type":"string"}}}""") {
            "42"
        }
        val SAMPLE_TOOL2 = tool("romanize", "Converts numbers to Roman numerals",
            """{"type":"object","properties":{"input":{"type":"integer"}}}""") {
            val value = it["input"]?.jsonPrimitive?.int ?: throw RuntimeException("No input")
            when (value) {
                5 -> "V"
                42 -> "XLII"
                84 -> "LXXXIV"
                else -> "I don't know"
            }
        }
        val SAMPLE_TOOL4 = tool("other", "Answer a question that cannot be answered by the other tools",
            """{"type":"object","properties":{"input":{"type":"string"}}}""") {
            "I don't know"
        }
        val SAMPLE_TOOLS = listOf(SAMPLE_TOOL1, SAMPLE_TOOL2, SAMPLE_TOOL4)

        val exec = JsonFunctionExecutor(OpenAiClient.INSTANCE, GPT35_TURBO, SAMPLE_TOOLS)

        runBlocking {
            exec.execute("Multiply 21 times 2 and then convert it to Roman numerals.")
            exec.execute("Convert 5 to a Roman numeral.")
            exec.execute("What year was Jurassic Park?")
        }
    }
```

Sample output:
```
INFO: User Question: Multiply 21 times 2 and then convert it to Roman numerals.
INFO: Call Function: calc with parameters {"input":"21*2"}
INFO: Result: 42
INFO: Call Function: romanize with parameters {"input":42}
INFO: Result: XLII
INFO: Final Response: The result of multiplying 21 by 2 is 42, which is represented in Roman numerals as XLII.
INFO: User Question: Convert 5 to a Roman numeral.
INFO: Call Function: romanize with parameters {"input":5}
INFO: Result: V
INFO: Final Response: The Roman numeral equivalent of 5 is V.
INFO: User Question: What year was Jurassic Park?
INFO: Final Response: I am unable to provide the release year of Jurassic Park as it requires general knowledge that falls outside the scope of the tools available. You can easily find the information by searching online or consulting a movie database. If you have any other questions related to functions or calculations, feel free to ask!
```

## Example using `JsonToolExecutor`

The `JsonFunctionExecutor` uses OpenAI's [tool API](https://platform.openai.com/docs/api-reference/chat/create#chat-create-tools).

```kotlin
    @Test
    fun testTools() {
        OpenAiClient.INSTANCE.settings.logLevel = LogLevel.None

        runBlocking {
            JsonToolExecutor(OpenAiClient.INSTANCE, GPT35_TURBO, SAMPLE_TOOLS)
                .execute("Multiply 21 times 2 and then convert it to Roman numerals.")

            JsonToolExecutor(OpenAiClient.INSTANCE, GPT35_TURBO, SAMPLE_TOOLS)
                .execute("Convert 5 to a Roman numeral.")

            JsonToolExecutor(OpenAiClient.INSTANCE, GPT35_TURBO, SAMPLE_TOOLS)
                .execute("What year was Jurassic Park?")
        }
    }
```

Sample output:
```
INFO: User Question: Multiply 21 times 2 and then convert it to Roman numerals.
INFO: Call Function: calc with parameters {"input": "21*2"}
INFO: Result: 42
INFO: Call Function: romanize with parameters {"input": 42}
INFO: Result: XLII
INFO: Final Response: 21 multiplied by 2 is 42, which is represented in Roman numerals as XLII.
INFO: User Question: Convert 5 to a Roman numeral.
INFO: Call Function: romanize with parameters {"input":5}
INFO: Result: V
INFO: Final Response: The Roman numeral representation of 5 is "V".
INFO: User Question: What year was Jurassic Park?
INFO: Call Function: other with parameters {"input":"Jurassic Park was released in 1993."}
INFO: Result: I don't know
INFO: Final Response: I'm unable to provide the exact year for the movie "Jurassic Park". Would you like me to look it up for you?
```