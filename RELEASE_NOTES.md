# PromptFx/PromptKt Release Notes

## 0.15.0 (March 2026)

### Highlights

- **OpenAI Responses API support** — Full support for the OpenAI Responses (agentic) API, including Files API and correct base64 image encoding for multimodal inputs.
- **Unified AI model hierarchy** — New `AiModel` parent interface unifies text, image, speech-to-text, and text-to-speech models under a common API, with a new `modelSource` attribute for tracking model origins.
- **Gemini updates** — Updated Gemini model list and added support for Gemini image generation.
- **Agent workflow logging** — New workflow logger captures and summarizes tool call activity from `AgentChatFlow` sessions.
- **Multi-module project split** — The project has been reorganized into multiple multi-module Maven projects for cleaner separation of concerns.
- **Progress bar UI** — Embedding calculation tasks in PromptFx now show progress.
- **Starship View improvements** — Configurable demo mode, screen-adaptive scaling, and other visual polish.

---

### New Features

#### External API / Model Support

- **[#75](https://github.com/aplpolaris/promptfx/issues/75)** — Support for OpenAI Responses (Agentic) API *(epic)*
- **[#532](https://github.com/aplpolaris/promptfx/issues/532)** — Initial implementation of OpenAI Responses API in promptkt
- **[#9](https://github.com/aplpolaris/promptfx/issues/9)** — Support for OpenAI Files API
- **[#679](https://github.com/aplpolaris/promptfx/issues/679)** — Correctly encode base64 image data in Responses API `input_image` content items
- **[#668](https://github.com/aplpolaris/promptfx/issues/668)** — Updated Gemini model list
- **[#346](https://github.com/aplpolaris/promptfx/issues/346)** — Support for Gemini image generation

#### Core AI Model API

- **[#638](https://github.com/aplpolaris/promptfx/issues/638)** — New `AiModel` interface as a common parent for all AI model types
- **[#636](https://github.com/aplpolaris/promptfx/issues/636)** — Added `modelSource` parameter to core APIs
- **[#666](https://github.com/aplpolaris/promptfx/issues/666)** — Refactored `ModelInfo` to reduce top-level attributes
- **[#659](https://github.com/aplpolaris/promptfx/issues/659)** — Fully removed deprecated snapshots support from `tri.ai.core.ModelInfo`
- **[#654](https://github.com/aplpolaris/promptfx/issues/654)** — New `AiModel` type: `TextToSpeechModel`
- **[#653](https://github.com/aplpolaris/promptfx/issues/653)** — New `AiModel` type: `SpeechToTextModel`
- **[#633](https://github.com/aplpolaris/promptfx/issues/633)** — Deprecated `VisionLanguageChat`; usages replaced with `MultimodalChat`

#### Agent / Workflow

- **[#664](https://github.com/aplpolaris/promptfx/issues/664)** — Workflow logger that captures events from `AgentChatFlow` and prints a tool call summary

#### PromptFx UI

- **[#159](https://github.com/aplpolaris/promptfx/issues/159)** — Progress bar for vector embedding calculation tasks
- **[#491](https://github.com/aplpolaris/promptfx/issues/491)** — Runtime `PromptFx.properties` support for high-level app configuration
- **[#676](https://github.com/aplpolaris/promptfx/issues/676)** — Unified `AiChatEngine` making `TextChat` and `MultimodalChat` interchangeable in the UI
- **[#674](https://github.com/aplpolaris/promptfx/issues/674)** — JSON output strategy that retries multiple times and returns the most-repeated result
- **[#686](https://github.com/aplpolaris/promptfx/issues/686)** — Starship demo mode configuration exposed in `PromptFxSettingsView`
- **[#683](https://github.com/aplpolaris/promptfx/issues/683)** — Starship View improvements: fixed rocket indicator, disabled grid lines by default, configurable repeat delay, screen-adaptive scaling
- **[#681](https://github.com/aplpolaris/promptfx/issues/681)** — Updated icon color theme to dark green

#### CLI

- **[#396](https://github.com/aplpolaris/promptfx/issues/396)** — Improvements to Document QA CLI

---

### Bug Fixes

- **[#672](https://github.com/aplpolaris/promptfx/issues/672)** — Fixed improper use of `cellformat` causing runtime exceptions
- **[#574](https://github.com/aplpolaris/promptfx/issues/574)** — Fixed inability to select the correct model when the same model is available from multiple sources
- **[#567](https://github.com/aplpolaris/promptfx/issues/567)** — Restored support for runtime-customized Starship YAML files
- **[#523](https://github.com/aplpolaris/promptfx/issues/523)** — Fixed intermittent exception in the Text-to-Image view

---

### Build & Infrastructure

- **[#400](https://github.com/aplpolaris/promptfx/issues/400)** — Updated to Kotlin 2.x and openai-kotlin 4.x
- **[#637](https://github.com/aplpolaris/promptfx/issues/637)** — Split the project into multiple multi-module Maven projects
- **[#644](https://github.com/aplpolaris/promptfx/issues/644)** — Reviewed and standardized names across POMs and build scripts
- **[#661](https://github.com/aplpolaris/promptfx/issues/661)** — Added `OpenAiModelIndexTest` script for automated model config recommendations
- **[#655](https://github.com/aplpolaris/promptfx/issues/655)** — Set up GitHub Copilot instructions for the repository
