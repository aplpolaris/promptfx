# User Interface Organization

The PromptFx User Interface is divided into:

- A navigation bar at the top, allowing you to move forward/backward between different views, enter a "full screen" mode, and manage the default models used for text completion and embeddings.
- A navigation bar at the left, where you can navigate to different views.
- A main view, which is generally divided into inputs and outputs.
- A parameters panel at the right, where you can adjust model settings, prompt settings, etc. for the view.
- A "Run" button and status bar, for executing tasks using remote APIs.

PromptFx’s interface is modular: most tabs and views (including custom prompt apps) can be extended or overridden at runtime. Place your own `views.yaml` in `config/` to add or replace UI panels. See [Configuring Views](https://github.com/aplpolaris/promptfx/wiki/PromptFx#configuring-views-at-runtime) for details.

# Views

PromptFx has views for testing AI/ML models and for a number of basic applications. These views are organized into tabs on the left side of the UI:

- [[API Tab]] - Access to model playgrounds and API-based tools (completions, chat, embeddings, images, audio, etc.).
- [[Prompts]] - Tools for prompt library management, template editing, batch prompt scripting, and prompt validation.
- [[Text]] - Natural language processing tasks, including summarization, translation, entity extraction, sentiment analysis, and more.
- [[Documents]] - Document Q&A, document search, clustering, text manager, and PDF/snippet preview tools.
- [[Multimodal]] - Image and audio features, such as speech-to-text, text-to-speech, image generation, and description (Vision/Audio tabs).
- [[MCP]] - Support for Model Context Protocol (MCP) servers and prompts/resources/tools. (See https://modelcontextprotocol.io/.)
- [[Agents]] - Agentic workflows, tool chaining, and integrations with external APIs or agent plugins.
- [[Settings]] - Application and session configuration, system info, and runtime diagnostics.
- [Custom](https://github.com/aplpolaris/promptfx/wiki/PromptFx#configuring-views-at-runtime) - Uncategorized and user-configured views.