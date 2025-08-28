# PromptLibrary Configuration

This document describes the PromptLibrary configuration feature that allows filtering prompts by patterns.

## Overview

The PromptLibrary configuration feature allows you to control which prompts are loaded from the built-in prompt library using pattern-based filtering. This is useful for:

- Focusing on specific types of prompts (e.g., only text-related prompts)
- Excluding experimental or deprecated prompts
- Creating custom prompt sets for different use cases
- Reducing memory usage by loading only needed prompts

## Configuration File Format

The configuration file uses YAML format:

```yaml
# Include specific prompt IDs by pattern (optional)
includeIds:
  - "text-*"           # Include all text-related prompts
  - "docs-qa/*"        # Include all document Q&A prompts

# Include specific categories by pattern (optional)  
includeCategories:
  - "text-*"           # Include all categories starting with 'text-'
  - "docs"             # Include 'docs' category exactly

# Exclude specific prompt IDs by pattern (optional)
excludeIds:
  - "*beta*"           # Exclude any prompts with 'beta' in the ID
  - "experimental/*"   # Exclude all experimental prompts

# Exclude specific categories by pattern (optional)
excludeCategories:
  - "deprecated"       # Exclude all deprecated prompts
  - "internal"         # Exclude internal-use prompts
```

## Pattern Matching

Patterns support simple wildcard matching using `*`:

- `text-*` matches any string starting with "text-"
- `*beta*` matches any string containing "beta"
- `*/test` matches any string ending with "/test"
- `*` matches any string
- `exact-match` matches only the exact string

## Filtering Logic

The filtering logic works as follows:

1. **Includes First**: If `includeIds` or `includeCategories` are specified, only prompts matching at least one include pattern are loaded
2. **Excludes Second**: Any prompts matching `excludeIds` or `excludeCategories` patterns are then filtered out
3. **Default Behavior**: If no includes are specified, all prompts are loaded by default, then excludes are applied

## Usage

### Command Line Interface

Use the `--prompt-config` option with CLI tools:

```bash
java -cp ... tri.ai.cli.PromptBatchRunner --prompt-config /path/to/config.yaml input.yaml output.yaml
```

### PromptFx UI

1. Open the Prompt Library view
2. Click the Filter button (🔽) in the toolbar
3. Select a configuration file
4. Restart the application for changes to take effect

### Programmatic Usage

```kotlin
// Load with default configuration (all prompts)
val defaultLibrary = PromptLibrary.loadWithConfig()

// Load with custom configuration
val config = PromptLibraryConfig(
    includeCategories = listOf("text-*"),
    excludeCategories = listOf("experimental")
)
val filteredLibrary = PromptLibrary.loadWithConfig(config)

// Load from configuration file
val libraryFromFile = PromptLibrary.loadWithConfigFile("/path/to/config.yaml")
```

## Examples

### Include Only Text-Related Prompts

```yaml
includeCategories:
  - "text-*"
```

This loads only prompts in categories starting with "text-" (e.g., text-classify, text-summarize, text-extract).

### Exclude Experimental Features

```yaml
excludeIds:
  - "*beta*"
  - "*experimental*"
excludeCategories:
  - "experimental"
```

This excludes any prompts or categories marked as experimental or beta.

### Focus on Document Processing

```yaml
includeCategories:
  - "docs-*"
includeIds:
  - "text-extract/*"
  - "text-summarize/*"
```

This includes all document-related categories plus specific text processing prompts useful for document workflows.

## File Locations

- **Built-in default**: `promptkt-core/src/main/resources/tri/ai/prompt/resources/prompt-library-config.yaml`
- **Distribution example**: `config/prompt-library-config.yaml` 
- **Development example**: `promptfx/config/prompt-library-config.yaml`

## Available Categories and IDs

To see what prompts and categories are available, you can run:

```kotlin
val library = PromptLibrary.loadWithConfig()
library.list().forEach { prompt ->
    println("ID: ${prompt.id}, Category: ${prompt.category}")
}
```

Common categories include:
- `text-*` (text-classify, text-extract, text-summarize, text-translate, text-qa)
- `docs-*` (docs-map, docs-metadata, docs-qa, docs-reduce)
- `examples` (sample prompts)
- `generate-*` (generate-config, generate-list, generate-taxonomy)
- `image-describe` (image processing prompts)
- `research-report` (research and reporting prompts)