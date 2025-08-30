# Flexible Runtime View Configuration

This document describes the new flexible argument configuration system for RuntimePromptView, which allows configuring complex views like Question Answering and Structured Data extraction directly in YAML instead of requiring hardcoded UI components.

## Overview

The new `ArgConfig` system replaces the limited `ModeConfig` approach and supports three display types:
- **TEXT_AREA**: Input text areas on the left side with descriptive headers
- **COMBO_BOX**: Dropdown selections in the parameters panel (backward compatible)
- **HIDDEN**: Arguments with default values that don't appear in the UI

## ArgConfig Properties

```yaml
argOptions:
  - templateId: input              # Template variable name (required)
    label: Source Text             # UI label (required)
    description: Enter your text   # Tooltip/placeholder text (optional)
    displayType: TEXT_AREA         # How to display: TEXT_AREA, COMBO_BOX, HIDDEN
    defaultValue: "default"        # Default value (optional)
    id: mode-reference             # Reference to modes.yaml (for COMBO_BOX)
    values: ["a", "b", "c"]        # Inline values (for COMBO_BOX)
```

## Display Types

### TEXT_AREA
Creates an input text area on the left side with a header showing the label. The description becomes the placeholder text and tooltip.

```yaml
- templateId: input
  label: Source Text
  description: The text to extract information from
  displayType: TEXT_AREA
```

### COMBO_BOX
Creates a dropdown in the parameters panel. Can use either inline values or reference modes.yaml.

```yaml
- templateId: format
  label: Output Format
  description: Choose the desired output format
  displayType: COMBO_BOX
  values: ["json", "xml", "csv"]
  defaultValue: "json"
```

### HIDDEN
Uses a default value without showing any UI element. Useful for configuration parameters.

```yaml
- templateId: include_metadata
  label: Include Metadata
  displayType: HIDDEN
  defaultValue: "true"
```

## Additional Configuration Options

### JSON Output
Enable JSON response format:
```yaml
requestJson: true
```

### Model Parameters
Show model parameters (temperature, max tokens, etc.):
```yaml
isShowModelParameters: true
```

## Example Configurations

### Structured Data Extraction
```yaml
structured-data-extraction:
  prompt:
    id: text-extract/text-to-json
    category: Text
    title: Structured Data Extraction
  argOptions:
    - templateId: input
      label: Source Text
      description: The text to extract structured information from
      displayType: TEXT_AREA
    - templateId: example
      label: Sample Output
      description: Example JSON, YAML, XML, CSV, or other structured format
      displayType: TEXT_AREA
    - templateId: format
      label: Format as
      displayType: COMBO_BOX
      id: structured-format
  isShowModelParameters: true
  requestJson: true
```

### Question Answering
```yaml
question-answering:
  prompt:
    id: docs-qa/answer
    category: RAG QA 
    title: Question Answering
  argOptions:
    - templateId: question
      label: Question
      description: Enter your question here
      displayType: TEXT_AREA
    - templateId: context
      label: Context
      description: Relevant context or documents
      displayType: TEXT_AREA
    - templateId: answer_style
      label: Answer Style
      displayType: COMBO_BOX
      values: ["brief", "detailed", "comprehensive"]
      defaultValue: "detailed"
    - templateId: include_sources
      label: Include Sources
      displayType: HIDDEN
      defaultValue: "yes"
  isShowModelParameters: true
```

## Backward Compatibility

Existing configurations using `modeOptions` continue to work unchanged:

```yaml
entity-extraction:
  prompt:
    id: text-extract/entities
    category: Text
  modeOptions:  # Still supported
    - id: entities
      templateId: mode
      label: Mode
```

## Migration Guide

To convert existing hardcoded views to YAML configuration:

1. **Identify template variables** in your prompt template (e.g., `{{{input}}}`, `{{mode}}`)
2. **Create argOptions** for each variable:
   - Text inputs → `displayType: TEXT_AREA`
   - Dropdowns → `displayType: COMBO_BOX`
   - Hidden config → `displayType: HIDDEN`
3. **Add descriptions** for better UX
4. **Set requestJson: true** if your view needs JSON output
5. **Test** the configuration loads properly

This new system enables creating sophisticated views entirely through YAML configuration without requiring any Kotlin code changes.