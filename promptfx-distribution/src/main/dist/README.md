# PromptFX

**PromptFX** is a cross-platform desktop application for exploring and experimenting with AI prompts
using OpenAI and Google Gemini models, or models provided via another endpoint.
It provides a graphical user interface to ask questions of local documents (Q&A),
test large language models (LLMs), organize prompts, switch between runtime modes, 
and interact with results — all in one clean, tabbed UI. It supports chat models, vision language models,
audio models, and more.

PromptFX is ideal for experimentation, prototyping, and learning about prompt workflows using real APIs.

---

## 🧰 Requirements

- Java 17+ (OpenJDK 21+ recommended for Apple Silicon/macOS users)
- macOS, Windows, or Linux desktop environment

> If using macOS on an ARM/Apple Silicon machine, see notes under “How to Run”

---

## 🚀 How to Run

1. **Download a release** from the [📦 Releases page](https://github.com/aplpolaris/promptfx/releases).
2. **Extract the ZIP** for your platform:
    - `promptfx-<version>-windows.zip`
    - `promptfx-<version>-macos.zip`
    - `promptfx-<version>-linux.zip`

3. **Run the launcher script**:
    - **Windows**: Double-click `run-windows.bat`
    - **macOS**: Right-click `run-macos.command` → Open (you may need to approve it in Security & Privacy)
    - **Linux**: Run `./run-linux.sh` in the terminal (you may need to `chmod +x` first)

Once launched, the PromptFX interface provides multiple tabs for different types of AI interactions, including direct prompting, chaining, view-based templates, and more.

> **EXECUTABLE CONFIGURATION**
> 
> You may need to approve the app in your system's security settings if it doesn't open right away.
> 
> **For Apple Macs (Intel or ARM)**:
> Right-click the unzipped folder and select `New Terminal At Folder`.
> Paste the following command into the terminal. This is a one-time operation that removes the
> quarantine flag allowing you to run downloaded software:
> ```bash
> xattr -rd com.apple.quarantine .
> ```
> Then, run the app using the `run-macos.command` file.
 
> **JAVA INSTALLATION**
> 
> You’ll need Java 17+ installed unless you’re using a bundled version (coming soon).
> 
> **For Apple Macs (Intel or ARM)**: PromptFX was built using OpenJDK 21. If you don't have Java 21 installed, run:
> ```bash
> brew install openjdk@21
> ```
> To ensure it runs correctly, add it to your `PATH`:
> ```bash
> echo 'export PATH="/opt/homebrew/opt/openjdk@21/bin:$PATH"' >> ~/.zshrc
> source ~/.zshrc
> ```
> You can check your current Java version with:
> ```bash
> java -version
> ```

---

## ⚙️ Configuration Overview

When running PromptFX, your application folder should be structured like this:
```
promptfx/
│
├── run-[platform].*               # Launcher script (e.g. .bat, .sh, .command)
├── promptfx-[version].jar         # Main application JAR (includes all dependencies)
├── apikey.txt                     # (optional) OpenAI API key
├── apikey-gemini.txt              # (optional) Gemini API key
│
├── config/                        # YAML configuration files
│   │
│   ├── openai-api-config.yaml     # API and Model Configurations
│   ├── gemini-models.yaml
│   ├── ollama-models.yaml
│   ├── openai-models.yaml
│   │
│   ├── mcp-servers.yaml          # MCP server configurations
│   │
│   ├── modes.yaml                # View customization
│   ├── starship.yaml
│   ├── views.yaml
│   ├── views-links.yaml
│   │
│   └── plugins/                   # Plugin configuration
│
└── prompts/                       # Custom prompt templates
    └── custom-prompts.yaml
```

### 🔐 API Key Setup

PromptFX works with **OpenAI** and **Google Gemini** APIs. You can enter your API key in-app or store it ahead of time:

#### OpenAI:
- Click the **key icon** in the toolbar to enter it in the UI, **or**
- Create a file named `apikey.txt` in the same folder where you run the app
- Or, set a system environment variable:  
  `OPENAI_API_KEY=your-key`

#### Gemini:
- Save your key in a file named `apikey-gemini.txt` in the app folder

### ⚙️ Runtime Options

PromptFX supports optional runtime flags that modify behavior or enable experimental features.

| Flag         | Description                                                                 |
|--------------|-----------------------------------------------------------------------------|
| `starship` | Enables the **Starship demo** button in the UI, allowing access to features defined in `config/starship.yaml`. |

### 🛠️ Runtime Configuration Files

PromptFX uses YAML files to configure models, views, prompts, and runtime behavior. You can customize these to suit your needs.
These files are located in the `config/` folder and loaded at runtime.
Many of these files have default versions included in the JAR, but you can create your own in the `config/` folder to override or extend functionality without modifying the JAR.

Many of these settings and customizations can be viewed when PromptFx is launched in the `Settings` view.

Additional information on runtime configuration can be found at https://github.com/aplpolaris/promptfx/wiki/PromptFx#configuring-views-at-runtime.

#### API Configuration and Models

| File | Purpose                                       |
|------|-----------------------------------------------|
| `openai-api-config.yaml` | OpenAI-compatiable API settings              |
| `gemini-models.yaml` | Available Google Gemini models                |
| `ollama-models.yaml` | Available Ollama models (local model support) |
| `openai-models.yaml` | Available OpenAI models                       |

The `-models.yaml` files are used to restrict which models are available in the UI for a given API endpoint.

The `openai-api-config.yaml` file is used to configure alternate (non-standard) API endpoints compatible with the OpenAI API specification, such as *Ollama*.
You may customize the base API URL and some basic API parameters, and reference a separate model file (e.g. `ollama-models.yaml`) to limit the models available in the UI.

#### MCP Server Configuration

| File | Purpose                                                    |
|------|------------------------------------------------------------|
| `mcp-servers.yaml` | Configurations for Model Context Protocol (MCP) servers |

The `mcp-servers.yaml` file is used to configure connections to Model Context Protocol (MCP) servers. MCP servers provide additional tools, prompts, and resources that can be accessed from within PromptFX. You can connect to multiple servers simultaneously, each with its own configuration.

**Supported Server Types:**

- **embedded**: An in-process server using default or custom prompt libraries
- **test**: A test server with sample prompts and tools for development
- **http**: A remote MCP server accessible via HTTP endpoint
- **stdio**: A local MCP server process that communicates via standard input/output

**Example configuration:**
```yaml
servers:
  # Embedded server with default libraries
  embedded:
    type: embedded
    description: "Embedded MCP server with default prompt and tool libraries"
  
  # Test server with samples
  test:
    type: test
    description: "Test server with sample prompts and tools"
    includeDefaultPrompts: true
    includeDefaultTools: true
    includeDefaultResources: true
  
  # Remote HTTP server
  remote-http:
    type: http
    description: "Remote MCP server via HTTP"
    url: "http://localhost:8080/mcp"
  
  # Remote stdio server (e.g., filesystem server)
  filesystem:
    type: stdio
    description: "MCP filesystem server"
    command: "npx"
    args: ["-y", "@modelcontextprotocol/server-filesystem", "/path/to/directory"]
    env:
      NODE_ENV: "production"
```

Once configured, MCP servers can be accessed from the **MCP** tab in the PromptFX UI, where you can browse available prompts, tools, and resources, and execute them interactively.

For more information on MCP servers and available implementations, see the [Model Context Protocol documentation](https://modelcontextprotocol.io/).

#### View Customization

| File | Purpose                                                    |
|------|------------------------------------------------------------|
| `modes.yaml` | Lists of options (used in prompt templates and some views) |
| `starship.yaml` | Configuration of the "starship" demo mode                  |
| `views.yaml` | Configurations of custom views                             |
| `views-links.yaml` | Documentation links displayed in navigation panes          |

The `modes.yaml` file is used to configure certain drop-down lists of options in the UI.

The `starship.yaml` file is used to configure the "Starship demo" mode, which can be enabled with the `starship` runtime flag. This allows you to create a custom demo experience that can be accessed via a button in the UI. This feature is under development and may not be fully functional.

The `views.yaml` file is especially powerful and can be used to create custom views built around prompt templates.
Prompt templates may be defined directly, or may be defined in a custom prompts file under `prompts/`.

The `views-links.yaml` file allows you to customize documentation links that appear at the bottom of navigation panes in the UI. Links are organized by category (e.g., API, MCP) and grouped by topic.

**Example configuration:**
```yaml
API:
  - group: "OpenAI"
    links:
      - label: "OpenAI API Reference"
        url: "https://platform.openai.com/docs/api-reference"
      - label: "OpenAI Pricing"
        url: "https://openai.com/pricing"
MCP:
  - group: "Documentation"
    links:
      - label: "MCP Getting Started"
        url: "https://modelcontextprotocol.io/docs/getting-started/intro"
```

You can add links for any tab category (API, MCP, Prompts, Text, Documents, etc.) by creating groups with labels and URLs. The links will automatically appear at the bottom of the corresponding navigation pane under "Documentation/Links".

### 📝 Custom Prompts

Custom prompt templates can be added or modified in the `prompts/` folder:

| File | Purpose                                                    |
|------|------------------------------------------------------------|
| `custom-prompts.yaml` | Custom prompt templates that override or extend defaults   |

The `prompts/` folder is scanned recursively, so you can organize your prompts into subdirectories if desired.
Prompts are accessible within the `Prompt Library` view in the UI, and can be used in custom views.

**Example prompt template:**
```yaml
prompts:
  - id: text-extract/author@1.0.0
    title: Extract Author
    description: Extracts the author name from input text
    template: |
      Input: {{{input}}}
      Author:
  
  - id: text-summarize/simplify-audience@1.0.0
    title: Simplify Text for Audience
    template: |
      Simplify the following text into 1-2 short sentences explaining the main idea.
      Write it for {{audience}}.
      ```
      {{{input}}}
      ```
```

Prompts use Mustache templating syntax with `{{{variable}}}` for unescaped content and `{{variable}}` for escaped content.
See the `prompts/README.md` file for more details on prompt structure and usage.

---

### 🧩 Plugins

PromptFX supports two types of plugins that extend its functionality:

- **API Plugins (TextPlugin)**: Add support for new AI model providers (chat, completion, embedding, vision, audio models)
- **View Plugins (NavigableWorkspaceView)**: Add custom UI views to the PromptFX workspace

Plugins are installed by copying JAR files to the `config/plugins/` directory and restarting PromptFX. They are automatically discovered using Java's ServiceLoader mechanism.

See `config/plugins/README.md` for detailed information on plugin types, installation, and development. Sample plugins are available in the repository:
- `promptfx-sample-api-plugin/` - API plugin example
- `promptfx-sample-view-plugin/` - View plugin example

---

## 📚 Resources

- **Main Repository**: [PromptFX on GitHub](https://github.com/aplpolaris/promptfx)
- **📖 Wiki & Docs**: [PromptFX Wiki](https://github.com/aplpolaris/promptfx/wiki)
- **📦 Releases**: [Download Latest Release](https://github.com/aplpolaris/promptfx/releases)
