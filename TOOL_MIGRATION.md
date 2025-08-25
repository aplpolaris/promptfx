# Tool to Executable Migration Guide

This document shows how to migrate from the legacy `Tool` and `JsonTool` classes to the new `Executable` interface, and how to use them with the new `AgentExecutable` class.

## Overview

The new `Executable` interface provides:
- Consistent JsonNode input/output format
- Schema support for input/output validation
- Shared execution context (ExecContext)
- Better integration with the workflow system

## Migration Examples

### Converting Tool to Executable

**Before (Legacy Tool):**
```kotlin
val calculatorTool = object : Tool("Calculator", "Use this to do math") {
    override suspend fun run(input: ToolDict): ToolResult {
        val inputText = input["input"] ?: ""
        return when {
            "2+2" in inputText -> ToolResult("4")
            else -> ToolResult("Unknown calculation")
        }
    }
}
```

**After (Wrapped as Executable):**
```kotlin
val calculatorExecutable = ToolExecutable.wrap(calculatorTool)
// or directly:
val calculatorExecutable = ToolExecutable(calculatorTool)
```

### Converting JsonTool to Executable

**Before (Legacy JsonTool):**
```kotlin
val romanizerTool = object : JsonTool("Romanizer", "Converts numbers to Roman numerals",
    """{"type":"object","properties":{"input":{"type":"integer"}}}""") {
    override suspend fun run(input: JsonObject): String {
        val value = input["input"]?.toString()?.toIntOrNull() ?: 0
        return when (value) {
            42 -> "XLII"
            else -> "Unknown number"
        }
    }
}
```

**After (Wrapped as Executable):**
```kotlin
val romanizerExecutable = JsonToolExecutable.wrap(romanizerTool)
// or directly:
val romanizerExecutable = JsonToolExecutable(romanizerTool)
```

### Creating Agents with Executables

**Before (ToolChainExecutor with Tools):**
```kotlin
val tools = listOf(calculatorTool, romanizerTool)
val executor = ToolChainExecutor(completionEngine)
val result = executor.executeChain("Calculate 2+2 and convert to Roman numerals", tools)
```

**After (AgentExecutable with Executables):**
```kotlin
val executables = listOf(
    ToolExecutable.wrap(calculatorTool),
    JsonToolExecutable.wrap(romanizerTool)
)
val agent = AgentExecutable(
    name = "MathAgent",
    description = "An agent that can do math and convert to Roman numerals",
    version = "1.0.0",
    inputSchema = null,
    outputSchema = null,
    tools = executables
)

val context = ExecContext(
    resources = mapOf("completionService" to completionEngine)
)
val input = context.mapper.createObjectNode().put("request", "Calculate 2+2 and convert to Roman numerals")
val result = agent.execute(input, context)
```

## Key Differences

### Input/Output Format
- **Tool**: Uses `ToolDict` (Map<String, String>) and returns `ToolResult`
- **JsonTool**: Uses `JsonObject` and returns `String`
- **Executable**: Uses `JsonNode` for both input and output, providing more flexibility

### Schema Support
- **Tool**: No built-in schema support
- **JsonTool**: Has schema in constructor but not exposed as JsonNode
- **Executable**: Explicit `inputSchema` and `outputSchema` properties as JsonNode

### Execution Context
- **Tool/JsonTool**: No shared context
- **Executable**: Uses `ExecContext` for shared resources, variables, and tracing

### Versioning
- **Tool/JsonTool**: No versioning
- **Executable**: Explicit version property

## Bridge Classes

The migration uses bridge classes to wrap legacy objects:

- `ToolExecutable`: Wraps a `Tool` object
- `JsonToolExecutable`: Wraps a `JsonTool` object

These maintain backward compatibility while providing the new `Executable` interface.

## Benefits of Migration

1. **Consistency**: All tools use the same JsonNode interface
2. **Schema validation**: Input/output schemas for better tooling
3. **Context sharing**: Shared execution context for resources
4. **Workflow integration**: Better integration with WorkflowExecutor
5. **Future-proofing**: Prepared for future enhancements

## Testing

All bridge classes are thoroughly tested:
- `ToolExecutableTest`: Tests Tool wrapping
- `JsonToolExecutableTest`: Tests JsonTool wrapping
- `AgentExecutableTest`: Tests AgentExecutable functionality
- `ExecutableMigrationIntegrationTest`: Tests the full migration workflow