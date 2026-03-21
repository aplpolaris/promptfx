`promptkt-pips` supports creating an executing generic asynchronous task pipelines with dependencies in the package `tri.ai.pips`.

# Basic Example

## Example Task Chain

Here is a sample execution of a chain of three tasks, where we need `Task a` to run first, then `Task b`, then `Task c`:

```kotlin
    @Test
    fun testExecuteChain() = runTest {
        val results = AiPipelineExecutor.execute(
            listOf(GoTask("a"), GoTask("b", setOf("a")), GoTask("c", setOf("b"))),
            PrintMonitor()).interimResults
        assertEquals(3, results.size)
        assertEquals("go", results["a"]?.firstValue!!)
        assertEquals("go", results["b"]?.firstValue!!)
        assertEquals("go", results["c"]?.firstValue!!)
    }

    class GoTask(id: String, deps: Set<String> = setOf()): AiTask<String>(id, null, deps) {
        override suspend fun execute(inputs: Map<String, AiPromptTraceSupport<*>>, monitor: AiTaskMonitor) =
            AiPromptTrace(outputInfo = AiOutputInfo(listOf("go")))
    }
```

This requires implementing the `AiTask` interface with custom execution logic, and defining the dependencies that must be completed for a task to begin. The `AiPipelineExecutor` handles the ordering of tasks, while the `PrintMonitor` observes progress and reports it as needed.

# API Details

## Task Definition

An `AiTask` is required to implement the following:

```kotlin
    /**
     * A task that can be executed, using a table of input results indexed by key.
     * Input types are not specified.
     */
    abstract suspend fun execute(inputs: Map<String, AiPromptTraceSupport<*>>, monitor: AiTaskMonitor): AiPromptTraceSupport<T>
```

The `AiPromptTraceSupport<T>` object may contain information about the prompt (`AiPromptInfo`), the model (`AiModelInfo`), the execution (`AiExecInfo`), and the output (`AiOutputInfo<T>`). Of these, only execution info is required. This allows tracking of everything from the direct output of the task to errors to prompt and model settings.

In general, tasks can be any asynchronous call, and do not need to have any AI-specific code.

## Task Execution

`AiPipelineExecutor` is used to execute a list of `AiTask`s. This works by determining which of the remaining tasks have all dependent tasks completed successfully, executing these, and then looking for additional tasks. The `AiTaskMonitor` is notified when the task starts, when it completes successfully, and when it fails.

## Linear Task Chain Builder

`AiPlanner` provides a single interface for returning a list of `AiTask`'s and can be used to prepare an execution chain. Builders are provided to assist in creating plans for basic linear task chains, as in the following:

```kotlin
    override fun plan() =
        aitask("weather-similarity-check") {
            checkWeatherSimilarity(input)
        }.aitask("weather-api-request") {
            completionEngine.jsonPromptTask<WeatherRequest>("weather-api-request", input, tokenLimit = 500, temp = null)
        }.task("weather-api") {
            weatherService.getWeather(it)
        }.aitask("weather-response-formatter") {
            val json = jsonMapper.writeValueAsString(it)
            completionEngine.instructTask("weather-response-formatter", instruct = input, userText = json, tokenLimit = 500, temp = null)
        }.plan
```

Here, the first `aitask` is a builder that returns an `AiTaskList` with a single task (executes the functionality between braces). The remaining operations (`aitask` and `task`) add additional tasks to this list, and the final `plan` operation wraps the list within an `AiPlanner` object.

