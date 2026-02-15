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

### Prompt Fields

- **id** (required): Unique identifier in format `category/name@version`
  - Example: `text-extract/dates@1.0.0`
  - Used internally to reference the prompt
  
- **title** (required): Display name shown in the UI
  - Example: "Extract Dates from Text"
  
- **description** (optional): Explains what the prompt does
  - Shown as a tooltip or help text in the UI
  
- **template** (required): The actual prompt text
  - Supports Mustache templating syntax
  - Use `{{variable}}` for HTML-escaped content
  - Use `{{{variable}}}` for raw/unescaped content (recommended for user inputs)

## Common Variables

Different PromptFx views provide different variables to prompts:

### General Text Processing
- `{{{input}}}` - Main text input from the user
- `{{audience}}` - Target audience (when specified)
- `{{format}}` - Desired output format

### Document Insights (Map/Reduce)
- `{{{input}}}` - Document snippet or text chunk
- `{{name}}` - Document or snippet name

### Document Q&A
- `{{{instruct}}}` - User's question
- `{{{input}}}` - Relevant document sections
- `{{matches}}` - List of matching snippets (for snippet-joiner prompts)
- `{{name}}` - Snippet or document name
- `{{{text}}}` - Snippet text content

### Image Analysis
- Images are automatically included based on the view
- Prompts should describe what to extract or analyze

## Prompt Categories

Organize prompts by category using ID prefixes:

- **text-extract/*** - Extraction tasks (dates, names, entities, etc.)
- **text-summarize/*** - Summarization tasks
- **text-transform/*** - Text transformation and rewriting
- **docs-map/*** - Document insights map operations (run on each chunk)
- **docs-reduce/*** - Document insights reduce operations (combine results)
- **docs-qa/answer-*** - Document Q&A answer generation
- **snippet-joiner/*** - Combining text chunks for Q&A
- **image-describe/*** - Image analysis and description
- **custom/*** - Your custom category

## Overriding Default Prompts

To override a built-in prompt, use the same `id` as the default prompt. Your custom version will take precedence.

For example, to customize the date extraction prompt:

```yaml
prompts:
  - id: text-extract/dates@1.0.0
    title: Extract Dates (Custom)
    description: Extract dates with custom formatting
    template: |
      Extract a list of dates from the following text, and format as {{DATE_FORMAT}}.
      '''
      {{{input}}}
      '''
```

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
6. **Organize by category**: Use consistent ID prefixes for related prompts

## Reference

For more information on prompt templating:
- Mustache template syntax: https://mustache.github.io/
- PromptFx documentation: https://github.com/aplpolaris/promptfx/wiki
