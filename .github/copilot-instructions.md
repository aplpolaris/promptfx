# Copilot Instructions for PromptFx

## Project Overview

This repository is organized into four top-level Maven modules:

- **`promptkt`** – Kotlin library for AI API interactions
- **`promptex`** – Extended prompt execution modules
- **`promptrt`** – Prompt runtime modules
- **`promptfx`** – JavaFX GUI application

It is primarily used with the OpenAI API or compatible LLM APIs.

## Repository Structure

```
promptkt/          # Kotlin library for AI API interactions
  promptkt-core/   # Core API utilities (document chunking, embeddings, pipelines, prompts)
  promptkt-provider-openai/    # OpenAI API provider
  promptkt-provider-gemini/    # Google Gemini provider (REST-based)
  promptkt-provider-gemini-sdk/ # Google Gemini provider (SDK-based)
  promptkt-provider-sample/    # Template for creating new providers

promptex/          # Extended prompt execution modules
  promptex-docs/   # Document processing
  promptex-mcp/    # MCP (Model Context Protocol) integration
  promptex-pips/   # Prompt pipelines

promptrt/          # Prompt runtime modules
  promptrt-cli/    # CLI interfaces

promptfx/          # promptfx-meta: JavaFX GUI application
  promptfx/        # Main UI module
  promptfx-sample-view-plugin/ # Sample plugin for custom UI views

distribution/      # Distribution artifacts
```

## Build and Test

This project uses **Maven** and **Kotlin** (JVM target: Java 17).

### Requirements
- Maven 3.6.3+
- Java 17+

### Build Commands
```bash
# Build individual modules (order matters due to dependencies)
mvn -B install -DskipTests --file promptkt/pom.xml
mvn -B install -DskipTests --file promptex/pom.xml
mvn -B install -DskipTests --file promptrt/pom.xml
mvn -B install -DskipTests --file promptfx/pom.xml
```

### Test Commands
```bash
# Run tests for individual modules
mvn -B test --file promptkt/pom.xml
mvn -B test --file promptex/pom.xml
mvn -B test --file promptrt/pom.xml
mvn -B test --file promptfx/pom.xml
```

> **Note:** Tests requiring OpenAI or Gemini API keys are excluded from the default test run (`excludedGroups: gemini | openai`). Use JUnit 5 test group tags `openai` or `gemini` to mark integration tests that require API keys.

### Running PromptFx
```bash
java -jar promptfx/promptfx/target/promptfx-0.1.0-SNAPSHOT-jar-with-dependencies.jar
```

## Coding Conventions

- **Language:** Kotlin (source and test directories: `src/main/kotlin`, `src/test/kotlin`)
- **Package structure:** `tri.promptfx.*` for UI code, `tri.ai.*` for library code
- **Serialization:** Uses `kotlinx-serialization` (Jackson + Kotlin serialization)
- **Async:** Uses Kotlin coroutines (`kotlinx-coroutines-core`)
- **HTTP client:** Uses Ktor
- **UI framework:** TornadoFX (Kotlin DSL wrapper around JavaFX), ControlsFX, FontAwesome
- **Prompt templates:** Uses Mustache syntax
- **Testing:** JUnit 5; tests needing external APIs are tagged with `@Tag("openai")` or `@Tag("gemini")`
- **License:** Apache 2.0; file headers are managed automatically by `license-maven-plugin`

## Key Libraries

- `openai-kotlin` (aallam/openai-kotlin) – OpenAI API client
- `ktor` – HTTP client used for API calls
- `kotlinx-serialization` – JSON serialization
- `jackson` – Additional JSON support
- `tornadofx` – JavaFX DSL for Kotlin UI
- `mustache.java` – Prompt template rendering

## Adding a New AI Provider

To add a new AI provider, use `promptkt-provider-sample` as a template. Implement the interfaces in `promptkt-core` and register your provider using the standard service-loader or plugin mechanism.

## Security Notes

- API keys are **never** committed to source code.
- API keys are loaded from local files (`apikey.txt`, `apikey-gemini.txt`) or environment variables (`OPENAI_API_KEY`).
- Always exclude files containing secrets from version control.
