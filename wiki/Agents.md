# Agentic Workflow View

The **Agents** tab provides an agentic workflow view where you can describe a task and have an AI agent use available PromptFx views as tools to complete it.

## Overview

The **Agentic Workflow** view allows you to:

1. **Describe a task** in natural language (e.g., "Summarize this document and then translate it to French").
2. **Select tools** — any PromptFx view that accepts text input can be used as a tool. Available views are listed with their category, name, and description.
3. **Choose a workflow engine** to control how the agent reasons and executes.
4. **Run** the task and observe intermediate steps and tool calls in the output log.

## Selecting Tools

The tool list displays all available PromptFx views as selectable tools. Each entry shows:
- The **category** (e.g., Text, Documents) in bold
- The **tool name** (derived from the view title)
- A brief **description** of what the tool does

Use the **Selection** menu to quickly select or deselect all tools. The **Quick Search** field lets you filter the list by name, category, or description.

## Workflow Engines

Three workflow execution engines are available:

| Engine | Description |
|--------|-------------|
| **Tool Chain** | Uses a plan-and-execute reasoning loop with a chat model to select and call tools in sequence. |
| **Tool Chain with JSON Schemas** | Same as Tool Chain, but uses the model API's built-in tool-calling feature with JSON schemas for input/output. |
| **Workflow Planner** | A more complex multi-step reasoning loop with task planning, execution, and validation capabilities. |

## Output Log

The log panel shows the agent's intermediate reasoning and tool interactions:

- `[USER]` — the original task input
- `[PROGRESS]` — status updates during execution
- `[REASONING]` — internal reasoning steps
- `[TASK]` — task planning steps (Workflow Planner only)
- `[TOOL-IN]` — input sent to a tool
- `[TOOL-OUT]` — output returned from a tool
- `[FINAL]` — the agent's final response

## Developer Notes

The Agentic Workflow view is implemented in `AgenticView.kt`. It uses the `tri.ai.core.agent` APIs from `promptkt-pips`:

- `ToolChainExecutor` — implements the Tool Chain engine
- `JsonToolExecutor` — implements the Tool Chain with JSON Schemas engine
- `WorkflowExecutor` — implements the Workflow Planner engine

See [PromptKt Tool Chain](PromptKt-Tool-Chain) for more on the underlying agent/tool architecture.
