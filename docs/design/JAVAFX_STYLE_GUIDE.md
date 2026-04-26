# PromptFx JavaFX/TornadoFX Style Guide

This guide summarises the coding conventions, architectural patterns, and step-by-step procedures that developers and agents should follow when working on the JavaFX/TornadoFX UI layer of **PromptFx**.

---

## Table of Contents

1. [UI Architecture Overview](#1-ui-architecture-overview)
2. [Package Structure](#2-package-structure)
3. [TornadoFX Conventions](#3-tornadofx-conventions)
4. [Creating a New View](#4-creating-a-new-view)
5. [Registering a View in the Application](#5-registering-a-view-in-the-application)
6. [View Affordances](#6-view-affordances)
7. [Controllers](#7-controllers)
8. [Globals and Singletons](#8-globals-and-singletons)
9. [Properties and State Management](#9-properties-and-state-management)
10. [Theming and Styling](#10-theming-and-styling)
11. [Common Pitfalls](#11-common-pitfalls)

---

## 1. UI Architecture Overview

PromptFx is a JavaFX application built with the **TornadoFX** Kotlin DSL. Its top-level container is `PromptFxWorkspace`, which extends TornadoFX's `Workspace`. Navigation is driven by a left-side `Drawer` whose items each contain hyperlinks to individual views.

### View Class Hierarchy

```
tornadofx.View / tornadofx.Fragment
    └── AiTaskView                  – base for all AI task views
            └── AiPlanTaskView      – base for views driven by an AiTaskBuilder plan
```

| Class | Use when |
|---|---|
| `tornadofx.View` | A standalone, injectable view that lives in the workspace (e.g. settings, about). |
| `tornadofx.Fragment` | A reusable sub-component that is embedded inside another view (e.g. `PromptResultArea`). |
| `AiTaskView` | A full workspace view that submits tasks to an AI backend and shows results. Override `processUserInput()`. |
| `AiPlanTaskView` | Like `AiTaskView` but task execution is described declaratively via an `AiTaskBuilder`. Override `plan()`. |

---

## 2. Package Structure

```
tri.promptfx          – core workspace, controller, globals, base view classes
tri.promptfx.agents   – agent workflow views
tri.promptfx.api      – direct API explorer views (chat, completions, embeddings, …)
tri.promptfx.docs     – document-based views (Q&A, clustering, text manager, …)
tri.promptfx.fun      – fun/demo views
tri.promptfx.mcp      – MCP server/tool/resource views
tri.promptfx.multimodal – image, audio, speech views
tri.promptfx.prompts  – prompt library, template, history views
tri.promptfx.settings – settings and about views
tri.promptfx.text     – text manipulation views
tri.promptfx.ui       – reusable UI fragments and utilities
tri.util.ui           – generic UI utilities shared across modules
```

Place new views in the package that best matches their functional category. Pair each view class with a plugin class in the **same file**.

---

## 3. TornadoFX Conventions

### 3.1 Prefer the TornadoFX DSL

Always use TornadoFX builder functions rather than instantiating JavaFX nodes manually:

```kotlin
// ✅ preferred
vbox(10) {
    label("Title")
    textfield(myProperty)
}

// ❌ avoid in view/fragment DSL bodies
VBox(10.0).apply {
    children.add(Label("Title"))
}
```

### 3.2 Exception: `cellFormat { }` Blocks

Inside a `cellFormat { }` (or `cellformat { }`) lambda, the receiver is a `ListCell` or `TableCell`, **not** an `EventTarget`. TornadoFX builder extension functions that expect `EventTarget` receivers (such as `vbox { }`, `hbox { }`, `label { }`) will **not** work correctly there.

> **Rule:** Inside a `cellFormat { }` block you must use plain JavaFX patterns — e.g. `VBox().apply { … }` — rather than TornadoFX DSL patterns like `vbox { … }`.

```kotlin
// ✅ correct — plain JavaFX inside cellFormat
listview(items) {
    cellFormat {
        text = it.title
        graphic = VBox(4.0).apply {
            children += Label(it.subtitle).apply {
                style = "-fx-font-style: italic;"
            }
        }
    }
}

// ❌ incorrect — TornadoFX DSL won't attach properly inside cellFormat
listview(items) {
    cellFormat {
        text = it.title
        graphic = vbox(4) {          // <- wrong: vbox { } is an EventTarget extension
            label(it.subtitle) {
                style = "-fx-font-style: italic;"
            }
        }
    }
}
```

### 3.3 Property Bindings

- Use `SimpleStringProperty`, `SimpleIntegerProperty`, `SimpleBooleanProperty`, etc. for mutable state that the UI binds to.
- Prefer `visibleWhen(prop)` / `managedWhen(prop)` (both!) rather than setting `isVisible`/`isManaged` imperatively.
- Use `enableWhen(prop)` rather than setting `isDisable` directly.
- When a property controls both visibility and layout participation, always pair `visibleWhen` with `managedWhen` to avoid blank space for hidden nodes.

```kotlin
label("Only visible when active") {
    visibleWhen(isActive)
    managedWhen(isActive)   // keeps layout correct when hidden
}
```

### 3.4 `init` Block Order

TornadoFX evaluates the `override val root` property eagerly. Structure view classes consistently:

1. Declare properties (`val`/`var`, injections, `Simple*Property`).
2. Declare `override val root = ...` with the full layout DSL.
3. Use `init { }` blocks **after** `root` for side-effects that depend on UI nodes being present (e.g., calling `addInputTextArea(...)`, `parameters(...)`, `output { ... }`, `onCompleted { ... }`).

### 3.5 Dependency Injection

Use TornadoFX's `inject()` / `find<T>()` for component lookup; never instantiate views with `new`:

```kotlin
val controller: PromptFxController by inject()
val workspace: PromptFxWorkspace by inject()
val progress: AiProgressView by inject()
```

Use `find<T>()` for one-off lookups from outside a view context.

---

## 4. Creating a New View

### Step 1 – Choose the right base class

| Scenario | Base class |
|---|---|
| Simple AI call (override `processUserInput()`) | `AiTaskView` |
| Declarative multi-step AI pipeline (override `plan()`) | `AiPlanTaskView` |
| Standalone view with no AI task run button | `View` |
| Reusable embedded fragment | `Fragment` |

### Step 2 – Create the view class

```kotlin
package tri.promptfx.text

import javafx.beans.property.SimpleStringProperty
import tornadofx.*
import tri.ai.pips.AiWorkflowResult
import tri.promptfx.AiTaskView

class MyNewView : AiTaskView(
    title = "My New View",
    instruction = "Enter text below and click Run."
) {
    private val input = SimpleStringProperty("")

    init {
        addInputTextArea(input)     // adds a TextArea to the input pane
        parameters("Chat Model") {
            addDefaultChatParameters(ModelParameters())
        }
    }

    override suspend fun processUserInput(): AiWorkflowResult {
        // call the AI backend and return a result
        TODO("implement me")
    }
}
```

For `AiPlanTaskView`, override `plan()` instead of `processUserInput()`:

```kotlin
override fun plan(): AiTaskBuilder<*> =
    common.completionBuilder()
        .prompt(myPrompt)
        .params("input" to input.get())
        .taskPlan(chatEngine)
```

### Step 3 – Create the companion plugin class

In the **same file**, add a one-liner plugin that registers the view with the workspace:

```kotlin
class MyNewPlugin : NavigableWorkspaceViewImpl<MyNewView>(
    category = "Text",            // navigation tab
    name     = "My New View",     // label shown in the drawer hyperlink
    affordances = WorkspaceViewAffordance.INPUT_ONLY,
    type     = MyNewView::class
)
```

Choose the `category` from the list of built-in categories or provide a new string (a new tab is created automatically):

```
API, Prompts, Text, Documents, Multimodal, MCP, Agents, Settings
```

---

## 5. Registering a View in the Application

### 5.1 Built-in (compiled into the main JAR)

Add the fully-qualified name of the **plugin** class (not the view class) to:

```
promptfx/promptfx/src/main/resources/META-INF/services/tri.util.ui.NavigableWorkspaceView
```

```
tri.promptfx.text.MyNewPlugin
```

### 5.2 External plugin JAR

1. Create a separate Maven module that depends on `promptfx` (see `promptfx-sample-view-plugin` as a reference).
2. Implement `NavigableWorkspaceViewImpl` in the plugin module.
3. Declare the service in the module's `module-info.java`:

```java
provides NavigableWorkspaceView with tri.myplugin.MyNewPlugin;
```

4. Build the JAR and copy it to `config/plugins/` next to the application JAR at runtime.

### 5.3 Runtime-only (no Kotlin required)

For simple single-prompt views, define the view entirely in `config/views.yaml`, `config/prompts.yaml`, and (optionally) `config/modes.yaml` at runtime. See the [PromptFx wiki](../wiki/PromptFx.md) for full details.

---

## 6. View Affordances

`WorkspaceViewAffordance` declares what kind of data a view accepts or produces. Set the correct value when creating a plugin so that cross-view features (e.g. "Send result to…") work properly:

| Value | Meaning |
|---|---|
| `NONE` | No special input/output capability (default). |
| `INPUT_ONLY` | Accepts plain text input from other views. |
| `COLLECTION_ONLY` | Accepts a document collection (e.g. a `TextLibrary`). |
| `INPUT_AND_COLLECTION` | Accepts both text input and a document collection. |
| `OUTPUT_ONLY` | Produces output that other views can consume. |

---

## 7. Controllers

### `PromptFxController`

The central application controller. Inject it in any view:

```kotlin
val controller: PromptFxController by inject()
```

Key properties:

| Property | Type | Purpose |
|---|---|---|
| `chatEngine` | `SimpleObjectProperty<AiChatEngine>` | Currently selected chat/multimodal model. |
| `embeddingEngine` | `SimpleObjectProperty<EmbeddingStrategy>` | Currently selected embedding model + chunking strategy. |
| `traceHistory` | `AiTaskTraceHistoryModel` | History of all prompt traces during the session. |

Key methods:

| Method | Purpose |
|---|---|
| `addPromptTraces(title, result)` | Record the result of a task in trace history. |
| `updateUsage()` | Refresh token/usage counters after a task completes. |

### `AiProgressView`

Tracks background-task progress. Also injected automatically in `AiTaskView`:

```kotlin
val progress: AiProgressView by inject()
```

### `PromptFxConfig`

Manages persistent configuration (directory persistence, library file paths, last active view). Inject via `find<PromptFxConfig>()`. Use the provided helper extensions for file chooser dialogs:

```kotlin
promptFxFileChooser("Open file", arrayOf(FF_TXT), dirKey = DIR_KEY_TXT) { files -> ... }
promptFxDirectoryChooser("Select folder", dirKey = DIR_KEY_TEXTLIB) { dir -> ... }
```

Do **not** call `chooseFile` / `chooseDirectory` directly; the helpers ensure the last-used directory is remembered.

---

## 8. Globals and Singletons

### `PromptFxGlobals`

Provides global, singleton-style access to prompt libraries and runtime configuration. Use this **instead of** constructing library instances directly:

```kotlin
// ✅ preferred
val promptIds = PromptFxGlobals.promptsWithPrefix("generate-list")
val prompt = PromptFxGlobals.lookupPrompt("generate-list/default")
val filled = PromptFxGlobals.fillPrompt("generate-list/default", "input" to myText)
```

Do **not** read `PromptLibrary.INSTANCE` directly in UI code; go through `PromptFxGlobals` so runtime overrides are respected.

### `PromptFxModels`

Central registry of all AI models available to the application. Always use this to obtain model lists for combo boxes:

```kotlin
combobox(controller.chatEngine, PromptFxModels.chatEngines())
```

Never hard-code model IDs in UI code. `PromptFxModels` filters models through the active `PromptFxPolicy` and the runtime include/exclude configuration.

### `PromptFxPolicy`

Determines which models are available and which views are shown. Policies are set once at application start and should not be changed at runtime. UI code can read `PromptFxModels.policy` to adjust behaviour (e.g. show/hide a banner), but should not modify the policy object.

---

## 9. Properties and State Management

- Declare all mutable view state as `Simple*Property` fields at the top of the class.
- Bind UI controls directly to properties using TornadoFX bindings (e.g. `textfield(myProperty)`).
- Use `objectBinding`, `stringBinding`, `booleanBinding` for derived read-only computed values.
- Prefer immutable `val` for property holders; the property's *value* changes, not the holder.
- Use `observableListOf<T>()` (TornadoFX) for observable lists bound to list views or tables.
- Long-running operations must run on a background thread. Use `runAsync { ... }.ui { ... }` (TornadoFX) or the `AiTaskView.runTask { }` method; never block the JavaFX Application Thread.

```kotlin
// ✅ correct async pattern
runAsync {
    expensiveComputation()
}.ui { result ->
    myProperty.set(result)
}

// ❌ never block the UI thread
myProperty.set(expensiveComputation())
```

---

## 10. Theming and Styling

- The application icon accent color is defined in `PromptFxGlobals.kt` as `iconColor` (changes each minor version for a seasonal theme). Use it for sidebar icons via the `.themed` extension:

```kotlin
FontAwesomeIcon.CLOUD.graphic.themed    // applies iconColor as fill
```

- Prefer CSS class-based styling (`.css` files under `src/main/resources`) over inline styles.
- When inline styles are unavoidable, use the TornadoFX type-safe `style { }` DSL instead of raw strings where possible:

```kotlin
label("Title") {
    style {
        fontSize = 18.px
        fontWeight = FontWeight.BOLD
    }
}
```

- Raw `-fx-*` strings are acceptable for properties not covered by the DSL.
- For theming purposes `textThemedDarker` provides a darker variant of the current accent color.

---

## 11. Common Pitfalls

### `cellFormat { }` — TornadoFX DSL does not work

As stated in [section 3.2](#32-exception-cellformat--blocks), TornadoFX builder extensions are **not** available inside `cellFormat { }`. Always use plain JavaFX:

```kotlin
// ✅ correct
cellFormat {
    text = it.label
    graphic = HBox(6.0).apply {
        children += ImageView(it.icon)
        children += Label(it.detail)
    }
}

// ❌ incorrect
cellFormat {
    text = it.label
    graphic = hbox(6) { imageview(it.icon); label(it.detail) }
}
```

### Missing `managedWhen` causes blank space

When hiding a node with `visibleWhen`, always also call `managedWhen` with the same condition, otherwise the hidden node still occupies layout space.

### Blocking the UI thread

Never call suspending or blocking functions directly in an event handler. Use `runAsync` or delegate to the task execution mechanism in `AiTaskView`.

### Instantiating views with `new`

Never write `MyView()` to obtain a view; use `find<MyView>()` (or `inject()` inside a component). TornadoFX's DI container manages scoping and lifecycle.

### Modifying `PromptFxGlobals` or `PromptFxModels` at runtime

These are global singletons. Avoid mutating them from view code. Read-only access is fine.
