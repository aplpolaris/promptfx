# Prompt Group Definition Files

Example File:
```yaml
groupId: examples
defaults:
  category: examples
  tags: [ examples, testing ]
prompts:
  - id: examples/hello-world@1.0.0
    title: Hello World Example
    args:
      - name: input
        description: The input text to include in the example
        required: true
        type: string
    template: |
      Hello, world!
      This is an example prompt.
      Input: {{{input}}}
      Output: 
```

## Enhanced Example with Multiple Arguments:
```yaml
groupId: text-summarize
defaults:
  category: text
prompts:
  - id: text-summarize/summarize@1.0.0
    title: Summarize Text
    description: Summarize the provided text, using the given audience, style, and format.
    args:
      - name: input
        description: The text to be summarized
        required: true
        type: string
      - name: audience
        description: The intended audience for the summary (optional)
        required: false
        type: string
      - name: style
        description: The writing style for the summary (optional)
        required: false
        type: string
      - name: format
        description: The structural format for the summary (optional)
        required: false
        type: string
    template: |
      Summarize the following{{#audience}}, written for {{audience}}{{/audience}}{{#style}}, in the style of {{style}}{{/style}}{{#format}}, structured as {{format}}{{/format}}:
      ```
      {{{input}}}
      ```
```

**Group Parameters:**
 - each file is a "prompt group" with a `groupId` and a set of `defaults`
 - `defaults` are optional and applied to all prompts in the group, and may include `category` and/or `tags`
 - if no `category` is specified, the group default will be the `groupId`

**Prompt Parameters:**
 - each prompt must have an `id` and a `template`
 - optional fields include `name`, `title`, `description`, `args`, `contextInject`, `version`
 - prompts may also overwrite `category` and `tags`
 - generally, `id` should be in the format `groupId/name@version`, so `name` and `version` will be inferred from the `id` if they are not present
 - if `contextInject` includes parameter `today: true`, the current date will be injected into the prompt context as `{{today}}` (this is also default behavior)

**Prompt Argument Parameters:**
 - `args` is a list of named arguments that the prompt expects, which may be used for validation or documentation purposes. Fields for each argument include `name`, `description`, `required`, `type`, `defaultValue`, `allowedValues`
 - if not provided, `args` will be inferred from the template, looking for `{{input}}`, `{{instruct}}`, and other named placeholders
 - `type` may be one of `string`, `integer`, `number`, `boolean`, `json`, or `enumeration`
 - `allowedValues` is a list of valid values for the argument, if the type is `enumeration`

**Prompt Template Syntax**
 - mustache templates: https://mustache.github.io/mustache.5.html
 - double braces to insert text {{...}}
 - triple braces to insert text without escaping HTML {{{...}}}