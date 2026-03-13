The `API` tab contains a number of views designed for exercising OpenAI, Gemini, and other LLM/GAI service APIs. These are similar to the API playground at https://platform.openai.com/playground, and should be self-explanatory.

# Models view

The **Models** view is shown below. The view at left shows a full list of models supported by API plugins, and the view at right shows details about a selected model. There are also a few options for sorting and filtering the list by source and type. See https://platform.openai.com/docs/api-reference/models for API details.

![image](https://github.com/aplpolaris/promptfx/assets/13057929/e1a14c6b-e546-42db-902a-fa2a19109076)

# Chat API views

The **Chat** view shows a basic chat with support for a system message, user messages, and assistant messages. This exposes many API options for sampling, input, and output, but the exact set of supported parameters may vary by model. See https://platform.openai.com/docs/api-reference/chat for API details.

![image](https://github.com/aplpolaris/promptfx/assets/13057929/54d60f95-6aae-4bbe-b238-c9c86f919972)

The **Chat (Advanced)** view has a similar UI but with additional support for use of the OpenAI *tools* feature. This can be used, e.g. to generate structured JSON messages that can be sent to an API from a user's message. See https://platform.openai.com/docs/api-reference/chat/create#chat-create-tools for API details.

![image](https://github.com/aplpolaris/promptfx/assets/13057929/df8175e3-713f-43fc-9d14-377ed6c01a32)

The **Completions** view allows you to perform basic text completions using text chat and text completion models.

![image](https://github.com/aplpolaris/promptfx/assets/13057929/5e87146a-cd29-4c95-bcf3-f3caf4af30fb)

# Multimodal API views

PromptFx has limited support for OpenAI's audio, speech, and images endpoints.

The **Audio** view performs text transcription of recorded audio, or audio files, using OpenAI's *whisper* model. See https://platform.openai.com/docs/api-reference/audio/createTranscription for API details.

![image](https://github.com/aplpolaris/promptfx/assets/13057929/e9f44c09-981e-4c34-9f97-87167476e600)

The **Speech** view synthesizes speech from text, using OpenAI's text-to-speech (*tts*) models and selected voices. See https://platform.openai.com/docs/api-reference/audio/createSpeech for API details.

![image](https://github.com/aplpolaris/promptfx/assets/13057929/289bd70a-06bf-4867-b04a-de01391e6044)

The **Images** view generates an image from text, using OpenAI's DALLE2 or DALLE3 model. See https://platform.openai.com/docs/api-reference/images for API details.

![image](https://github.com/aplpolaris/promptfx/assets/13057929/00c52a14-0871-4a01-8548-46273388f937)

# Advanced API views

PromptFx supports OpenAI and Gemini `embeddings/` and OpenAI `moderations/` endpoints. Additional endpoints may be supported in the future.

For embeddings, enter text at left to see embeddings calculated (separately for each line) at right. The view allows you to set a custom output dimensionality to get smaller vectors. See https://platform.openai.com/docs/api-reference/embeddings for API details.

For moderations, enter text at left to see resulting moderation model flags from OpenAI. This is not currently integrated with any other views in PromptFx. See https://platform.openai.com/docs/api-reference/moderations for API details.

# Documentation/Links

Links are provided to various OpenAI webpages, opening in an external browser.