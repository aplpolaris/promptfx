`promptex-pips` supports creating and executing generic asynchronous task pipelines with dependencies in the package `tri.ai.pips`.

# Basic Example

## Example Task Chain

Here is a sample execution of a chain of three tasks, where we need `Task a` to run first, then `Task b`, then `Task c`:

```kotlin
    @Test
    fun testExecuteChain() = runTest {
        val result = AiWorkflowExecutor.execute(
            listOf(GoTask("a"), GoTask("b", setOf("a")), GoTask("c", setOf("b"))))
        assertEquals(3, result.traces.size)
        assertEquals("go", result.traces["a"]?.output?.firstValue)
        assertEquals("go", result.traces["b"]?.output?.firstValue)
        assertEquals("go", result.traces["c"]?.output?.firstValue)
    }

    class GoTask(id: String, deps: Set<String> = setOf()): AiTask<Any?, String>(id, null, deps) {
        override suspend fun execute(input: Any?, context: ExecContext) = "go"
    }
```

This requires implementing the `AiTask` abstract class with custom execution logic, and defining the dependencies that must be completed for a task to begin. `AiWorkflowExecutor` handles the ordering of tasks and shares an `ExecContext` across all tasks; each task's output and trace is stored in the context so subsequent tasks can access them.

# API Details

## Task Definition

`AiTask<I, O>` is an abstract class typed by its input type `I` and output type `O`. It is required to implement:

```kotlin
    /**
     * A task that can be executed with a typed input, returning a typed output.
     * For linear pipelines [input] is the output of the single predecessor task (or null if there is none).
     * For multi-dependency tasks, all predecessor outputs are available via [ExecContext.get].
     * Trace information should be recorded via [ExecContext.logTrace] instead of being embedded in the return value.
     */
    abstract suspend fun execute(input: I, context: ExecContext): O
```

The `ExecContext` provides access to:
- `context.monitor` — an `AiTaskMonitor` (`FlowCollector<ExecEvent>`) for emitting progress events
- `context.get(id)` — outputs from previously executed tasks
- `context.trace(id)` — `AiTaskTrace` objects logged by previously executed tasks
- `context.logTrace(id, trace)` — records an `AiTaskTrace` for the current task

In general, tasks can be any asynchronous call and do not need to have any AI-specific code.

## Task Execution

`AiWorkflowExecutor` is used to execute a list of `AiTask`s. It works by determining which of the remaining tasks have all dependent tasks completed successfully, executing these, and then looking for additional tasks. Progress events are emitted on `context.monitor` (`AiTaskMonitor`), a `FlowCollector<ExecEvent>`.

## Linear Task Chain Builder

`AiTaskBuilder` provides a fluent API for building a list of `AiTask`s for a linear execution chain:

```kotlin
    val plan: List<AiTask<*, *>> = AiTaskBuilder.task("weather-similarity-check") { context ->
            checkWeatherSimilarity(input)
        }.task("weather-api-request") { similarityResult, context ->
            completionEngine.jsonPromptTask<WeatherRequest>("weather-api-request", input, tokenLimit = 500, temp = null)
        }.task("weather-api") { request, context ->
            weatherService.getWeather(request)
        }.task("weather-response-formatter") { weatherData, context ->
            val json = jsonMapper.writeValueAsString(weatherData)
            completionEngine.instructTask("weather-response-formatter", instruct = input, userText = json, tokenLimit = 500, temp = null)
        }.plan
```

Here, `AiTaskBuilder.task(id) { context -> ... }` creates an `AiTaskBuilder` with a single root task. Each subsequent `.task(id) { input, context -> ... }` adds another task that receives the previous task's output as `input`. The final `.plan` property returns the list of tasks, which can then be passed to `AiWorkflowExecutor.execute()`.
