# Agent Instructions for PromptFx

## Project Overview

PromptFx is a JavaFX desktop workbench for experimenting with and demonstrating AI/LLM capabilities.
It supports multiple LLM providers (OpenAI, Google Gemini, Anthropic) through a pluggable architecture.

## Repository Structure

```
promptkt/                        # Kotlin library for AI API interactions
  promptkt-core/                 # Core abstractions: document chunking, embeddings, pipelines, prompts
  promptkt-provider-openai/      # OpenAI API provider (Ktor-based)
  promptkt-provider-openai-sdk/  # OpenAI Java SDK provider
  promptkt-provider-gemini/      # Google Gemini provider (REST-based)
  promptkt-provider-gemini-sdk/  # Google Gemini provider (SDK-based)
  promptkt-provider-anthropic-sdk/ # Anthropic SDK provider
  promptkt-provider-sample/      # Template for creating new providers

promptex/                        # Extended prompt execution modules
  promptex-docs/                 # Document processing and RAG pipelines
  promptex-mcp/                  # MCP (Model Context Protocol) integration
  promptex-pips/                 # Prompt pipelines and agent workflows

promptrt/                        # Prompt runtime modules
  promptrt-cli/                  # CLI interfaces

promptfx/                        # JavaFX GUI application (meta-module)
  promptfx/                      # Main UI module
  promptfx-sample-view-plugin/   # Sample plugin for custom UI views

distribution/                    # Distribution build and artifacts
```

### Key package namespaces
- `tri.promptfx.*` — UI code (views, controllers, panels)
- `tri.ai.*` — Library/framework code (providers, pipelines, models)

## Build and Test

**Requirements:** Maven 3.6.3+, Java 17+, Kotlin 2.3.10 (JVM target: 17)

```bash
# Build in dependency order
mvn -B install -DskipTests --file promptkt/pom.xml
mvn -B install -DskipTests --file promptex/pom.xml
mvn -B install -DskipTests --file promptrt/pom.xml
mvn -B install -DskipTests --file promptfx/pom.xml

# Run tests (API integration tests excluded by default)
mvn -B test --file promptkt/pom.xml

# Run the app
java -jar promptfx/promptfx/target/promptfx-*-jar-with-dependencies.jar
```

Integration tests requiring external API keys are excluded from the default test run. Each provider module
excludes its own tag locally; the root pom additionally excludes `gemini | openai` as a catch-all.

| Tag | Provider module |
|-----|----------------|
| `openai` | `promptkt-provider-openai` |
| `openai-sdk` | `promptkt-provider-openai-sdk` |
| `gemini` | `promptkt-provider-gemini` |
| `gemini-sdk` | `promptkt-provider-gemini-sdk` |
| `anthropic-sdk` | `promptkt-provider-anthropic-sdk` |

When adding integration tests, tag them with the appropriate provider tag and verify the provider pom's
`excludedGroups` covers it.

## Architecture Rules

- **Provider abstraction belongs in `promptkt-*`** — never add provider-specific code to `promptfx/`
- **UI views extend `AiTaskView`** — follow existing patterns in `api/`, `docs/`, `multimodal/`
- **Adding a new view** — see sections 4 & 5 of [`docs/design/JAVAFX_STYLE_GUIDE.md`](docs/design/JAVAFX_STYLE_GUIDE.md) for the full procedure (view class, companion plugin, and registration)
- **All async work uses Kotlin coroutines** — use `runLater {}` for UI thread updates
- **UI is TornadoFX DSL only** — do not use FXML

## Coding Conventions

- **Language:** Kotlin; source at `src/main/kotlin`, tests at `src/test/kotlin`
- **Serialization:** Jackson + kotlinx-serialization
- **HTTP client:** Ktor (core library); OpenAI Java SDK and Gemini SDK for provider integrations
- **UI framework:** TornadoFX (Kotlin DSL over JavaFX), ControlsFX, FontAwesome
- **Prompt templates:** Mustache syntax
- **Testing:** JUnit 5
- **License:** Apache 2.0; file headers are managed automatically by `license-maven-plugin` — do not write them manually

## Key Libraries

- `openai-kotlin` (aallam/openai-kotlin) — OpenAI API client
- `openai-java` — OpenAI Java SDK
- `ktor` — HTTP client for API calls
- `kotlinx-serialization` — JSON serialization
- `jackson` — Additional JSON support
- `tornadofx` — JavaFX DSL for Kotlin UI
- `mustache.java` — Prompt template rendering

## Documentation & Wiki

Developer and user documentation lives in [`docs/wiki/`](docs/wiki/), covering module architecture, feature areas,
and configuration. The online mirror is at https://github.com/aplpolaris/promptfx/wiki — prefer the local copy
as it may be more up to date.

- Model list maintenance guide: [`promptkt/docs/how-to/review-and-update-model-lists.md`](promptkt/docs/how-to/review-and-update-model-lists.md)

## Architecture Docs

| Module | Doc |
|--------|-----|
| `promptkt` | [`docs/design/PROMPTKT_ARCHITECTURE.md`](docs/design/PROMPTKT_ARCHITECTURE.md) — model interfaces, provider plugin pattern, result/trace types |
| `promptex` | [`docs/design/PROMPTEX_ARCHITECTURE.md`](docs/design/PROMPTEX_ARCHITECTURE.md) — task pipeline, RAG/embedding stack, MCP |
| `promptrt` | [`docs/design/PROMPTRT_ARCHITECTURE.md`](docs/design/PROMPTRT_ARCHITECTURE.md) — CLI entry points, memory service |
| `promptfx` | [`docs/design/PROMPTFX_ARCHITECTURE.md`](docs/design/PROMPTFX_ARCHITECTURE.md) — how the UI layer consumes the library |

## UI Style Guide

See [`docs/design/JAVAFX_STYLE_GUIDE.md`](docs/design/JAVAFX_STYLE_GUIDE.md) for JavaFX/TornadoFX coding conventions,
architectural patterns, and step-by-step procedures for the UI layer.

## Adding a New AI Provider

Use `promptkt-provider-sample` as a template. Implement the interfaces in `promptkt-core`
and register your provider via the standard service-loader or plugin mechanism.

## Security

- API keys are **never** committed to source code
- Keys are loaded from local files (`apikey.txt`, `apikey-gemini.txt`) or environment variables (`OPENAI_API_KEY`)
- Always exclude secret-containing files from version control
