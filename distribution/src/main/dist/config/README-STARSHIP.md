# Starship Demo Configuration Guide

The **Starship demo** is a full-screen animated display mode in PromptFX that showcases an AI pipeline running in a continuous loop. It can be enabled with the `starship` runtime flag.

This guide explains how to customize the Starship demo using YAML configuration files.

---

## Configuration File Loading

At startup, PromptFX looks for a Starship configuration file in this order:

1. `starship-custom.yaml` (in the app directory)
2. `config/starship-custom.yaml` *(this folder — recommended)*
3. `starship.yaml` (in the app directory)
4. `config/starship.yaml` *(this folder)*

If none of the above files is found, the built-in default configuration is used.

**Recommendation:** Copy `starship-custom.yaml` and edit it to customize the demo. This keeps your changes separate from the default `starship.yaml`, making it easy to update PromptFX without losing your customizations.

---

## Configuration Structure

The YAML configuration file has three main sections:

```
question   - controls how random questions are generated
pipeline   - defines the sequence of AI processing steps
layout     - configures the visual display grid and widgets
```

### `question` — Random Question Generator

Controls the randomized question generation that kicks off each demo cycle.

| Field | Description |
|-------|-------------|
| `template` | Prompt template sent to the LLM. Use `{{topic}}` and `{{example}}` as placeholders. |
| `topics` | List of topic strings. One is picked at random per cycle. May reference named lists with `{{listName}}` or `{{listName:N}}` for N random picks. |
| `examples` | List of example question strings. One is included in the prompt per cycle. |
| `lists` | Named lists of values that can be referenced in topics using `{{listName}}` syntax. |

**Example:**
```yaml
question:
  template: |
    Generate a random question about {{topic}}.
    Example: {{example}}
  topics:
    - "transformer architectures ({{types:2}})"
    - "real-world AI applications"
  examples:
    - "What is attention in a transformer?"
  lists:
    types: [ "BERT", "GPT", "T5" ]
```

---

### `pipeline` — AI Processing Steps

Defines the sequence of AI operations performed each demo cycle. Each step references a registered tool by its `tool` ID, reads from prior step outputs (via `$var`), and saves its output under `saveAs`.

| Field | Description |
|-------|-------------|
| `id` | Identifier for this pipeline version (optional). |
| `steps` | Ordered list of pipeline steps. |

**Step fields:**

| Field | Description |
|-------|-------------|
| `tool` | ID of the registered tool to call (e.g. `prompt-chat/text-summarize/simplify-audience`). |
| `description` | Human-readable description of this step (optional). |
| `input` | Map of input parameters. Use `{ "$var": "varName" }` to reference a prior step's output. |
| `saveAs` | Variable name to store this step's output, for use in later steps and widgets. |

**Built-in Starship tools:**

| Tool ID | Description |
|---------|-------------|
| `starship/random-question` | Generates a random question using the `question` config. |
| `starship/execute-view` | Sends input to the active view and returns its result. |

**Example:**
```yaml
pipeline:
  id: starship/custom@1.0.0
  steps:
    - tool: starship/random-question
      input: { }
      saveAs: question
    - tool: starship/execute-view
      input:
        input: { "$var": "question" }
      saveAs: viewResult
```

---

### `layout` — Visual Display

Controls the full-screen visual layout of the Starship demo.

| Field | Description |
|-------|-------------|
| `backgroundIcon` | FontAwesome icon name used for the background decoration (e.g. `STAR_ALT`, `ROCKET`). |
| `backgroundIconCount` | Number of background icons to render. |
| `numCols` | Number of columns in the display grid. |
| `numRows` | Number of rows in the display grid. |
| `isShowGrid` | Whether to draw grid lines. |
| `widgets` | List of widget configurations (see below). |

**Widget fields:**

| Field | Description |
|-------|-------------|
| `varRef` | Variable name from the pipeline whose value is displayed by this widget. |
| `widgetType` | One of: `ANIMATING_TEXT`, `ANIMATING_TEXT_VERTICAL`, `ANIMATING_THUMBNAILS`. |
| `pos` | Grid position: `{ x, y, width, height }` (1-based, in grid units). Use `height: 0` to stack vertically at the same position. |
| `overlay.step` | Step number shown in the explainer overlay (press `X` in Starship mode). |
| `overlay.title` | Title displayed on the widget chrome. |
| `overlay.icon` | FontAwesome icon name shown in the widget header. |
| `overlay.iconSize` | Size of the header icon in pixels. |
| `overlay.explain` | Description shown in the explainer overlay. |
| `overlay.options` | Map of option names to lists of choices, displayed as a dropdown in the widget. |

**Widget types:**

| Type | Description |
|------|-------------|
| `ANIMATING_TEXT` | Displays text content with an animated entry effect (horizontal layout). |
| `ANIMATING_TEXT_VERTICAL` | Displays text content with an animated entry effect (vertical/stacked layout). |
| `ANIMATING_THUMBNAILS` | Displays a set of document thumbnails (used for document search results). |

**Example:**
```yaml
layout:
  backgroundIcon: STAR_ALT
  backgroundIconCount: 500
  numCols: 3
  numRows: 3
  isShowGrid: false
  widgets:
    - varRef: question
      widgetType: ANIMATING_TEXT
      pos: { x: 1, y: 1, width: 2, height: 1 }
      overlay:
        step: 1
        title: Question
        icon: QUESTION
        iconSize: 48
        explain: AI generates a random question.
```

---

## Tips

- **Test your config:** PromptFX will log a warning and fall back to the built-in default if your file cannot be parsed or is missing required sections.
- **Required sections:** A valid config must have at least one `pipeline.steps` entry and at least one `layout.widgets` entry.
- **FontAwesome icons:** Icon names correspond to [FontAwesome 4.x](https://fontawesome.com/v4/icons/) icon identifiers in `UPPER_SNAKE_CASE` (e.g. `STAR_ALT`, `ROCKET`, `COGS`, `GLOBE`, `COMMENTS`).
- **Explainer overlay:** Press `X` during the Starship demo to toggle the explainer overlay showing step numbers and descriptions.
