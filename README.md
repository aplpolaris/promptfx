# PromptFx

This project provides a prototype Kotlin module for AI prompt chaining (`promptkt`) and an associated JavaFx demonstration UI (`promptfx`).
It is intended primarily for demonstration and exploration purposes, and is designed to be primarily used with the [OpenAI API](https://platform.openai.com/docs/api-reference) or a compatible LLM API.

See [below](https://github.com/aplpolaris/promptfx/tree/main#building-promptkt-and-promptfx) and [the wiki](https://github.com/aplpolaris/promptfx/wiki) for instructions to build and run PromptFx and troubleshooting.

## Modules

- **promptkt** [![Maven Central](https://maven-badges.herokuapp.com/maven-central/com.googlecode.blaisemath/promptkt/badge.svg)](https://maven-badges.herokuapp.com/maven-central/com.googlecode.blaisemath/promptkt) *utilities for working with AI APIs*
- **promptfx** [![Maven Central](https://maven-badges.herokuapp.com/maven-central/com.googlecode.blaisemath/promptfx/badge.svg)](https://maven-badges.herokuapp.com/maven-central/com.googlecode.blaisemath/promptfx) *JavaFx GUI*

## API Key

PromptFx currently supports OpenAI and Google Gemini models, though more can be added as plugins.

- To set the OpenAI API key, click on the key button in the toolbar. Alternately, save your key in an `apikey.txt` file in the same folder you use to run PromptFx, or in a system registry variable `OPENAI_API_KEY`.
- To set the Gemini API key, save your key in a `apikey-gemini.txt` file in the same folder you use to run PromptFx.

## PromptFx UI Features

PromptFx has views for testing AI/ML models and for a number of basic applications. These views are organized into tabs on the left side of the UI. Briefly:

- The [API tab](https://github.com/aplpolaris/promptfx/wiki/API-Tab) contains views for browsing models and testing API functionality.
- The [Tools tab](https://github.com/aplpolaris/promptfx/wiki/Tools) contains views for testing prompts, scripting prompts, working with text collections, and more.
- The [Documents tab](https://github.com/aplpolaris/promptfx/wiki/Documents) contains views for working with documents, including document Q&A and other tasks.
- The [Text tab](https://github.com/aplpolaris/promptfx/wiki/Text) contains views for natural language processing (NLP) and basic text processing (e.g. summarization, translation, sentiment analysis, etc.)
- The [Fun tab (beta)](https://github.com/aplpolaris/promptfx/wiki/Fun) contains views that may be less useful, but still fun! (like converting text to emoji, or having the AI chat with itself)
- The [Audio tab (beta)](https://github.com/aplpolaris/promptfx/wiki/Audio) contains views for working with speech-to-text and text-to-speech models
- The [Vision tab (beta)](https://github.com/aplpolaris/promptfx/wiki/Vision) contains views for generating and describing images (using multimodal models)
- The [Integrations tab (beta)](https://github.com/aplpolaris/promptfx/wiki/Integrations) contains experimental views that combine NLP tasks with API calls.

Most of these panels have configuration panels (at right) for adjusting settings. For the views under the `API` tab, this can be used to select the model used for the API call. For most other views, the panel will use the app's default completion model and/or embedding model. These defaults can be selected from the top toolbar.

## Document Q&A View

The `Document Q&A` tab is the default view. This lets you "ask questions" of a folder of local documents on your system, using OpenAI's Ada embedding model to retrieve relevant document chunks, and using [retrieval-augmented generation (RAG)](https://en.wikipedia.org/wiki/Retrieval-augmented_generation) to answer questions. In the view below, a question is being asked on a collection of research papers.

![image](https://github.com/aplpolaris/promptfx/assets/13057929/f5d6d17c-335f-4074-848a-87d0ea7c2aaa)

The view at the bottom left shows the "document snippets" that are used to find the most relevant matches in the documents in the folder. If you click on a snippet from a PDF document, a PDF viewer will show the page where the snippet comes from, as shown here:

![image](https://github.com/aplpolaris/promptfx/assets/13057929/7c289a7d-661b-4059-a21c-a6c1b4b90303)

To customize the documents used, select the `Open Folder` button and browse to a folder on your local system. Add documents (`.txt`, `.pdf`, `.doc`, or `.docx`) to this folder, and when you ask a question PromptFx will automatically pull text from the PDF/DOC files, chunk the text into sections, calculate embeddings, lookup relevant chunks based on your question, and use OpenAI's API to formulate a suitable response. The prompt is also designed to provide a reference to the source document. The configuration panel at right can be used to adjust the settings for chunking documents, as well as the prompting strategy.

In "full screen mode" (see button on toolbar), PromptFx provides a more elegant interactive mode, additionally rendering PDF thumbnails for source documents.

![image](https://github.com/aplpolaris/promptfx/assets/13057929/a063f5b3-59be-4b87-b0ef-76d5d22a9fa6)

This tab also contains a `Text Manager` view for creating and working with collections of text.

See [Documents](https://github.com/aplpolaris/promptfx/wiki/Documents) for more information.

## API Views

The `API` tab contains a number of views designed for exercising the OpenAI API (or a compatible API), including `completions/`, `chat/`, `images/`, `embeddings/`, `audio/`, and more. These are similar to the API playground at https://platform.openai.com/playground, and should be self-explanatory.

See [API-Tab](https://github.com/aplpolaris/promptfx/wiki/API-Tab) for more information.

## Tools Views

Under `Tools`, PromptFx provides:

* a `Prompt Library` view for viewing built-in and custom prompts
* a `Prompt Template` view for experimenting with prompt templates using [mustache](https://mustache.github.io/) syntax

![image](https://github.com/aplpolaris/promptfx/assets/13057929/3a1d64b7-ecb8-49df-933b-458776351c36)

* a `Prompt Scripting` view for executing batches of scripts
* a `Prompt Validator` view for experimenting with automatic validation methods

See [Tools](https://github.com/aplpolaris/promptfx/wiki/Tools) for more information.

## Text Views

The `Text` tab contains a number of use cases designed for natural language tasks, including translation, summarization, entity extraction, and sentiment analysis.
Many of these are similar to the examples described at https://platform.openai.com/examples. These views are designed primarily for experimentation with LLMs, so may vary in terms of feature sets and quality.

![image](https://github.com/aplpolaris/promptfx/assets/13057929/8052d13f-7335-46e1-8e14-e3db00162e35)

See [Text](https://github.com/aplpolaris/promptfx/wiki/Text) for more information.

## Beta Views

The `Fun` and `Integrations` tabs are experimental with some fun applications of large language models.
See [Fun](https://github.com/aplpolaris/promptfx/wiki/Fun) and [Integrations](https://github.com/aplpolaris/promptfx/wiki/Integrations) for more details.

The `Audio` and `Vision` tabs contain speech-to-text and speech-to-image tools, using OpenAI's Whisper and DALL-E models.
See [Audio](https://github.com/aplpolaris/promptfx/wiki/Audio) and [Vision](https://github.com/aplpolaris/promptfx/wiki/Vision) for more details.

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
- Run `mvn clean install` from the `promptkt` directory.
- Run `mvn clean install` from the `promptfx` directory.

The libraries also contain separate integration tests requiring `OpenAI` or `Gemini` API keys. See [the wiki](https://github.com/aplpolaris/promptfx/wiki) for how to execute those tests and additional build/run troubleshooting.

## Running PromptFx

To run the project (after compilation):
- Run `java -jar target/promptfx-0.1.0-SNAPSHOT-jar-with-dependencies.jar` from the `promptfx` directory.
