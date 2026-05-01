# promptfx Architecture

JavaFX desktop application that surfaces promptkt/promptex capabilities as a GUI workbench.
Package namespace: `tri.promptfx.*`

For UI patterns, TornadoFX conventions, and step-by-step view creation, see
[`JAVAFX_STYLE_GUIDE.md`](JAVAFX_STYLE_GUIDE.md).

---

## How promptfx consumes promptkt/promptex

| promptkt/ex type | How promptfx uses it |
|-----------------|----------------------|
| `AiModelProvider` | `PromptFxModels` wraps it — always use `PromptFxModels` in UI code, never `AiModelProvider` directly |
| `TextChat` / `MultimodalChat` | Held in `PromptFxController.chatEngine: SimpleObjectProperty` |
| `EmbeddingModel` + `TextChunker` | Held in `PromptFxController.embeddingEngine: SimpleObjectProperty<EmbeddingStrategy>` |
| `AiTaskBuilder` / `AiWorkflow` | `AiPlanTaskView.plan()` returns one; executor is called by the base view on Run |
| `AiWorkflowResult` | Rendered in `PromptResultArea`; traces stored in `PromptFxController.traceHistory` |
| `DocumentQaDriver` | Used inside doc-module views (`DocumentQaView`, etc.) |
| `McpProvider` / `McpToolLibrary` | Managed by MCP views in `tri.promptfx.mcp` |

---

## Key application-level singletons

| Singleton | Purpose |
|-----------|---------|
| `PromptFxController` | Central controller: active models, trace history, usage counters |
| `PromptFxWorkspace` | TornadoFX `Workspace` root; owns the navigation drawer |
| `PromptFxModels` | Filtered model registry — use this for all combo boxes and model lookups |
| `PromptFxGlobals` | Prompt library access, icon color, runtime overrides |
| `PromptFxConfig` | Persistent config: last-used directories, active view, library paths |
| `PromptFxPolicy` | Controls which views and models are visible; set once at startup, read-only thereafter |

---

## Plugin & view registration

Views are registered via Java `ServiceLoader`:

```
promptfx/promptfx/src/main/resources/META-INF/services/tri.util.ui.NavigableWorkspaceView
```

Each entry is a fully-qualified **plugin** class (e.g. `tri.promptfx.text.TranslationPlugin`),
not the view class. See `JAVAFX_STYLE_GUIDE.md` §4–5 for the full procedure.

External plugin JARs drop into `config/plugins/` and are loaded automatically at startup.
