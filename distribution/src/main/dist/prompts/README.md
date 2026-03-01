# PromptFx prompts Folder

This folder contains custom prompt template files that extend or override the default prompts available in PromptFx.

## Overview

Custom prompts allow you to:
- Create reusable prompt templates with variables
- Override built-in prompts with your own versions
- Organize prompts by category or use case
- Build custom views around specific prompt workflows

Prompts defined in this folder are automatically loaded at startup and appear in the **Prompt Library** view in the PromptFx UI.

## File Structure

The `prompts/` folder is scanned recursively, so you can organize your prompt files into subdirectories:

```
prompts/
├── custom-prompts.yaml          # Main custom prompts file
├── text-analysis/
│   └── sentiment-prompts.yaml   # Category-specific prompts
└── document-processing/
    └── summarization-prompts.yaml
```

All YAML files in this directory tree will be loaded as prompt definitions.

## Prompt File Format

Prompts are defined in YAML format with the following structure:

```yaml
prompts:
  - id: category/name@version
    title: Human Readable Title
    description: Optional description of what this prompt does
    template: |
      Your prompt template here.
      Use {{variable}} for escaped variables.
      Use {{{variable}}} for unescaped variables (recommended for user input).
      
      Example:
      Process the following text for {{audience}}:
      ```
      {{{input}}}
      ```
```

**Notes:**
- Prompts use Mustache templating syntax. The variable `{{{input}}}` is commonly used for the main text input from the user when prompts are used within views.
- Prompt IDs can be organized by category using prefixes, e.g., in `text-extract/dates@1.0.0`, the `text-extract` prefix is used as a category, and `1.0.0` is the version.
- Custom prompts with the same ID as a built-in prompt will override the built-in prompt.

## Examples

### Simple Extraction Prompt

```yaml
prompts:
  - id: text-extract/author@1.0.0
    title: Extract Author
    description: Extracts the author name from input text
    template: |
      Input: {{{input}}}
      Author:
```

### Prompt with Multiple Variables

```yaml
prompts:
  - id: text-summarize/simplify-audience@1.0.0
    title: Simplify Text for Audience
    template: |
      Simplify the following text into 1-2 short sentences explaining the main idea.
      Write it for {{audience}}.
      For younger audiences, keep it short with minimal jargon.
      For technical audiences, use precise technical terminology.
      ```
      {{{input}}}
      ```
```

### Document Map/Reduce Prompts

```yaml
prompts:
  # Map: Run on each document chunk
  - id: docs-map/key-points@1.0.0
    title: Extract Key Points
    template: |
      Extract the three most important points from this text:
      ```
      {{{input}}}
      ```
  
  # Reduce: Combine results from all chunks
  - id: docs-reduce/summary@1.0.0
    title: Create Final Summary
    template: |
      Combine the following key points into a coherent summary:
      ```
      {{{input}}}
      ```
```

### Document Q&A Prompt

```yaml
prompts:
  - id: docs-qa/answer-detailed@1.0.0
    title: Detailed Document Answer
    template: |
      Answer the following question based on the provided document sections.
      Include specific quotes and citations.
      
      Question: {{{instruct}}}
      
      Document sections:
      ```
      {{{input}}}
      ```
      
      Answer:
```

## Using Custom Prompts

Once defined, your custom prompts can be used in several ways:

1. **Prompt Library View**: Browse and test prompts interactively
2. **Custom Views**: Reference prompts by ID in `config/views.yaml`
3. **Document Insights**: Select from custom map/reduce prompts
4. **Document Q&A**: Choose custom Q&A prompts

## Best Practices

1. **Use descriptive IDs**: Make it clear what the prompt does
2. **Add descriptions**: Help users understand when to use each prompt
3. **Use versions**: Include `@1.0.0` in IDs for future compatibility
4. **Triple braces for input**: Use `{{{input}}}` to avoid HTML escaping issues
5. **Test thoroughly**: Try prompts with various inputs before deploying

## Reference

For more information on prompt templating:
- Mustache template syntax: https://mustache.github.io/
- PromptFx documentation: https://github.com/aplpolaris/promptfx/wiki
