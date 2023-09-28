# PromptFx

This project provides a prototype Kotlin module for AI prompt chaining (`promptkt`) and an associated JavaFx demonstration UI (`promptfx`). It is intended primarily for demonstration and exploration purposes, and is designed to be primarily used with the OpenAI API (https://platform.openai.com/docs/api-reference) or a compatible LLM API.

See [below](https://github.com/aplpolaris/promptfx/tree/main#building-promptkt-and-promptfx) and [the wiki](https://github.com/aplpolaris/promptfx/wiki) for instructions to build and run PromptFx and troubleshooting.

## PromptFx UI Features

PromptFx has views for testing AI/ML models and for a number of basic applications. These views are organized into tabs on the left side of the UI. Briefly:

- The `OpenAI API` tab contains views for testing OpenAI API functionality, as documented at https://platform.openai.com/docs/api-reference.
- The `Text` tab contains views for natural language processing (NLP) and basic text processing (e.g. summarization, translation, sentiment analysis, etc.)
- The `Audio` tab contains a view for speech-to-text (using OpenAI's Whisper model)
- The `Vision` tab contains a view for speech-to-image (using OpenAI's DALL-E mdoel)
- The `Integrations` tab contains sample views that combine NLP tasks with API calls.
- The `Fun` tab contains views that may be less useful, but still fun! (like converting text to emoji, or having the AI chat with itself)
- The `Tools` tab contains views designed for testing prompts.

Most of these panels have configuration panels (at right) for adjusting settings. For the views under the `OpenAI API` tab, this can be used to select the model used for the API call. For most other views, the panel will use the app's default completion model and/or embedding model. These defaults can be selected from the top toolbar.

### API Key

PromptFx requires an OpenAI API key. To set the API key, you may click on the key in the toolbar. Alternately, you may save your key in an `apikey.txt` file, or in a system registry variable `OPENAI_API_KEY`.

## Document Q&A View

The `Document Q&A` tab is the default view. This lets you "ask questions" of a folder of local documents on your system, using OpenAI's Ada embedding model to retrieve relevant document chunks, and using context-augmented prompting to answer questions. In the view below, a question is being asked on a collection of research papers.

![image](https://github.com/aplpolaris/promptfx/assets/13057929/f5d6d17c-335f-4074-848a-87d0ea7c2aaa)

The view at the bottom left shows the "document snippets" that are used to find the most relevant matches in the documents in the folder. If you click on a snippet from a PDF document, a PDF viewer will show the page where the snippet comes from, as shown here:

![image](https://github.com/aplpolaris/promptfx/assets/13057929/7c289a7d-661b-4059-a21c-a6c1b4b90303)

To customize the documents used, select the `Open Folder` button and browse to a folder on your local system. Add documents (`.txt`, `.pdf`, `.doc`, or `.docx`) to this folder, and when you ask a question PromptFx will automatically pull text from the PDF/DOC files, chunk the text into sections, calculate embeddings, lookup relevant chunks based on your question, and use OpenAI's API to formulate a suitable response. The prompt is also designed to provide a reference to the source document. The configuration panel at right can be used to adjust the settings for chunking documents, as well as the prompting strategy.

In "full screen mode" (see button on toolbar), PromptFx provides a more elegant interactive mode, additionally rendering PDF thumbnails for source documents.

![image](https://github.com/aplpolaris/promptfx/assets/13057929/a063f5b3-59be-4b87-b0ef-76d5d22a9fa6)

## OpenAI API Views

The `OpenAI API` tab contains a number of views designed for exercising the OpenAI API, including `completions/`, `chat/`, `images/`, `embeddings/`, `audio/`, and more. These are similar to the API playground at https://platform.openai.com/playground, and should be self-explanatory.

The model view is shown below. Clicking `Run` will retrieve a list of models and is a good way to ensure the application is able to access the OpenAI API.

![image](https://github.com/aplpolaris/promptfx/assets/13057929/6f604ac2-c4b9-4e65-9441-5f50dbdbd4a4)

![image](https://github.com/aplpolaris/promptfx/assets/13057929/2ee11ade-03db-4a88-b457-b93e215c57d7)

PromptFx has limited support for OpenAI's `audio/` and `images/` endpoints.

<img src="https://github.com/aplpolaris/promptfx/assets/13057929/78057da5-8551-40c2-abba-4f8f06574663" width=700/>

## Text Views

The `Text` tab contains a number of use cases designed for different LLM applications or testing. Many of these are similar to the examples described at https://platform.openai.com/examples. These views are designed primarily for experimentation with LLMs, so may vary in terms of feature sets and quality.

![image](https://github.com/aplpolaris/promptfx/assets/13057929/8052d13f-7335-46e1-8e14-e3db00162e35)

## Audio/Vision Views

These tabs contain speech-to-text and speech-to-image tools, using OpenAI's Whisper and DALL-E models.

## Integration Views

The `Integrations` tab contains a few experimental demonstrations that integrate with external APIs. Many of these use a *prompt chaining* approach that may involve a series of steps to answer a question. Here is an example that uses an LLM to decide on a Wikipedia page relevant to your question, and then use the content of that page to answer the question.

<img src="https://github.com/aplpolaris/promptfx/assets/13057929/5511e45b-0764-4837-ae08-0d9ee1e8205a" width=800 height=600/>

## Fun Views

These are just for fun. Try out the `AI Conversations` to have the AI converse with itself.

<img src="https://github.com/aplpolaris/promptfx/assets/13057929/8d96cdaa-3d86-438b-9a63-a80fd7fb1545" height=600/>
<img src="https://github.com/aplpolaris/promptfx/assets/13057929/486f7f40-0081-4b66-b6d8-c5697fd3c420" height=600/>

## Tools Views

The `Prompt Template` view provides a utility for experimenting with prompts. Enter the template at top, using [mustache](https://mustache.github.io/) syntax, and it will provide a list of inputs. This can be used, e.g. to quickly try different models, tweak the prompt template, etc.

![image](https://github.com/aplpolaris/promptfx/assets/13057929/3a1d64b7-ecb8-49df-933b-458776351c36)

# PromptKt Library Features

The PromptKt library provides a number of features for working with LLM APIs, including:

- document chunking tools
- a local embedding (vector) database
- command-line chat tools
- support for AI pipelines (chaining prompts and/or APIs together)
- configurable prompt templates with [mustache](https://mustache.github.io/) support
- a basic tool chain execution service

Many of these features resemble features of [LangChain](https://python.langchain.com/).

# Building PromptKt and PromptFx

System requirements:
- Maven version 3.9.3+
- Java version 17+

OpenAI Client Library:
- PromptKt/PromptFx use the `openai-kotlin` API client library from https://github.com/aallam/openai-kotlin.

To build the project:
- Run `mvn clean install -DskipTests` from the `promptkt` directory.
- Run `mvn clean install -DskipTests` from the `promptfx` directory.

Note that you can run tests as part of the build, but many of these require an `apikey.txt` file and use the OpenAI API. We anticipate migrating these to a separate profile and making them available as optional integration tests.

See https://github.com/aplpolaris/promptfx/wiki for additional build/run troubleshooting.

## Running PromptFx

To run the project (after compilation):
- Run `java -jar target/promptfx-0.1.0-SNAPSHOT-jar-with-dependencies.jar` from the `promptfx` directory.
