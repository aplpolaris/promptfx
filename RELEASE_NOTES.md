# PromptFx/PromptKt Release Notes

## 0.15.0 (March 2026)

## What's Changed

### 🚀 OpenAI Responses (Agentic) API

- **[Epic]** Full support for OpenAI Responses (Agentic) API [#75](https://github.com/aplpolaris/promptfx/issues/75)
- Initial implementation of the Responses API in promptkt [#532](https://github.com/aplpolaris/promptfx/issues/532)
- Added support for **OpenAI Files API** [#9](https://github.com/aplpolaris/promptfx/issues/9)
- Fixed base64 encoding of image data in Responses API `input_image` content items [#679](https://github.com/aplpolaris/promptfx/issues/679)

### 🏗️ Core AI Model API Unification

- Introduced **`AiModel`** as a new common parent interface for all AI model types [#638](https://github.com/aplpolaris/promptfx/issues/638)
- Added `modelSource` parameter to core APIs to track model origins [#636](https://github.com/aplpolaris/promptfx/issues/636)
- Refactored `ModelInfo` to reduce top-level attributes [#666](https://github.com/aplpolaris/promptfx/issues/666)
- Fully removed deprecated snapshots support from `tri.ai.core.ModelInfo` [#659](https://github.com/aplpolaris/promptfx/issues/659)
- New `AiModel` types: **`TextToSpeechModel`** and **`SpeechToTextModel`** [#654](https://github.com/aplpolaris/promptfx/issues/654), [#653](https://github.com/aplpolaris/promptfx/issues/653)
- Deprecated `VisionLanguageChat`; all usages replaced with `MultimodalChat` [#633](https://github.com/aplpolaris/promptfx/issues/633)

### 🌐 Gemini Support

- Updated Gemini model list [#668](https://github.com/aplpolaris/promptfx/issues/668)
- Added support for **Gemini image generation** [#346](https://github.com/aplpolaris/promptfx/issues/346)

### 🤖 Agent & Workflow

- New **workflow logger** that captures events from `AgentChatFlow` and prints a tool call summary [#664](https://github.com/aplpolaris/promptfx/issues/664)

### 🖥️ PromptFx UI

- Added **progress bar** for vector embedding calculation tasks [#159](https://github.com/aplpolaris/promptfx/issues/159)
- Added runtime **`PromptFx.properties`** support for high-level app configuration [#491](https://github.com/aplpolaris/promptfx/issues/491)
- Created a unified **`AiChatEngine`** making `TextChat` and `MultimodalChat` interchangeable in the UI [#676](https://github.com/aplpolaris/promptfx/issues/676)
- New **JSON output retry strategy** that re-attempts multiple times and returns the most-repeated result [#674](https://github.com/aplpolaris/promptfx/issues/674)
- Starship View improvements: fixed rocket indicator, disabled grid lines by default, configurable repeat delay, screen-adaptive scaling [#683](https://github.com/aplpolaris/promptfx/issues/683)
- Exposed Starship **demo mode configuration** in `PromptFxSettingsView` [#686](https://github.com/aplpolaris/promptfx/issues/686)
- Updated icon color theme to dark green [#681](https://github.com/aplpolaris/promptfx/issues/681)
- Improvements to **Document QA CLI** [#396](https://github.com/aplpolaris/promptfx/issues/396)

### 🐞 Bug Fixes

- Fixed improper use of `cellformat` causing runtime exceptions [#672](https://github.com/aplpolaris/promptfx/issues/672)
- Fixed inability to select the correct model when the same model is available from multiple sources [#574](https://github.com/aplpolaris/promptfx/issues/574)
- Restored support for runtime-customized Starship YAML files [#567](https://github.com/aplpolaris/promptfx/issues/567)
- Fixed intermittent exception in the Text-to-Image view [#523](https://github.com/aplpolaris/promptfx/issues/523)

### 📝 Build & Infrastructure

- Updated to **Kotlin 2.x** and **openai-kotlin 4.x** [#400](https://github.com/aplpolaris/promptfx/issues/400)
- Split the project into multiple multi-module Maven projects [#637](https://github.com/aplpolaris/promptfx/issues/637)
- Reviewed and standardized names across POMs and build scripts [#644](https://github.com/aplpolaris/promptfx/issues/644)
- Added `OpenAiModelIndexTest` script for automated model config recommendations [#661](https://github.com/aplpolaris/promptfx/issues/661)
- Set up GitHub Copilot instructions for the repository [#655](https://github.com/aplpolaris/promptfx/issues/655)
