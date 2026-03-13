# Prompt Library View

The `Prompt Library` view shows built-in and custom prompts.

![image](https://github.com/aplpolaris/promptfx/assets/13057929/5e1b45a5-5f6e-442f-a9b9-efb88a30d184)

You can view a prompt by selecting it at left, or open the runtime configurable prompts in the system editor using the edit button. The `Send to Template View` will open the selected prompt in the template view (as described below) so it can be tested.

# Prompt Scripting View

The `Prompt Scripting` view is for batch executing a template prompt with a series of inputs.

![image](https://github.com/aplpolaris/promptfx/assets/13057929/c33020e4-3ab7-4e06-9904-7a8c0080899d)

## Configuring Script Inputs

Under `Prompt Settings`:

* select or provide a prompt template with a single field ``{{input}}`` to use for processing all inputs.

Under `Select Inputs`:

* Use `Inputs` to provide a series of text inputs to batch process. By default, these are assumed to be line separated, but you can set alternate separators under `Data Import` on the parameters pane (e.g. `\n\n` if each chunk is separated by an empty line).
* There is an option to skip the first line of input (e.g. when processing CSV files) on the parameters pane.
* Alternately, click the file icon to load content from a file into the text pane
* You can also use the import icon to load content from a text library JSON file. These files can be generated on the text library view (see below) or in the document views (see [[Documents]]).
* Use `Filter` to optionally set either a regex filter or an LLM filter. If the latter, the filter should contain the template field ``{{input}}`` and should be designed to include the word "yes" in the response if an item should be included in the processing.

Depending on input, batch processing can consume a lot of tokens, so the default limit on number to process is set to 10. This can be adjusted under `Batch Processing` options on the parameters pane.

## Configuring Script Outputs

The output pane shows a list of results, as well as a text area summarizing the outputs.

* Use the context menu in the list to see prompt details, or to `Try in template view` (or click the paper airplane).
* You can export the entire list of results as a prompt trace file using the download button.
* The text area may be populated with multiple text summaries based on selection under `Output Options` on the parameters pane.
  * `Unique Results` shows a list of unique results with counts of occurence
  * `All Results` shows a list of all results
  * `As CSV` shows results as a CSV column appended to input lines
  * `LLM Summary` creates a summarization of all results using prompts specified under `Result Summarization Template` on the parameters pane.

# Prompt Template View

The `Prompt Template` view is for experimenting with prompts. Enter the template at top, using [mustache](https://mustache.github.io/) syntax, and it will provide a list of inputs. This can be used, e.g. to quickly try different models, tweak the prompt template, etc.

![image](https://github.com/aplpolaris/promptfx/assets/13057929/3a1d64b7-ecb8-49df-933b-458776351c36)

# Prompt Trace History View

The `Prompt Trace History` view can be used to view a log of most prompt executions that have been run in PromptFx. You can also filter by view, by model, by status (`success`, `error`, or `missing value`), and by type (`intermediate result`, `final result`, or `unknown`). You can also export the list of filtered prompt traces as a JSON file.

You can also use context menus to (i) open a result in the `Prompt Trace History` view, or (ii) "send" a result in the history view to the template view or a compatible processing view.

![image](https://github.com/user-attachments/assets/78cee0aa-ba8a-44f9-b142-84dec7590a54)

# Prompt Validator View

The `Prompt Validator` view is for testing an initial prompt using a secondary evaluation prompt. This needs more development, but could be useful for trying out a series of two prompts, e.g. generate some JSON and then check that the JSON is valid.

![image](https://github.com/aplpolaris/promptfx/assets/13057929/d815882f-47d0-49ce-ad81-c30c69bdb26d)
