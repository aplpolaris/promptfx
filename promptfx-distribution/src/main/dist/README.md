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
├── run-[platform].*               # Launcher script (e.g. .bat, .sh, .command)
├── promptfx-[version].jar         # Main application JAR (includes all dependencies)
├── apikey.txt                     # (optional) OpenAI API key
├── apikey-gemini.txt              # (optional) Gemini API key
└── config/                        # YAML configuration files
    ├── openai-models.yaml
    ├── gemini-models.yaml
    ├── prompts.yaml
    ├── views.yaml
    ├── views-links.yaml
    ├── modes.yaml
    └── starship.yaml
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

| File | Purpose                                                    |
|------|------------------------------------------------------------|
| `openai-models.yaml` | Available OpenAI models                                    |
| `gemini-models.yaml` | Available Google Gemini models                             |
| `prompts.yaml` | Prompt templates                                           |
| `modes.yaml` | Lists of options (used in prompt templates and some views) |
| `views.yaml` | Configurations of custom views                             |
| `views-links.yaml` | Documentation links displayed in navigation panes          |
| `starship.yaml` | Configuration of the "starship" demo mode                  |

These files are located in the `config/` folder and loaded at runtime.

#### Configuring Documentation Links

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

Additional information on runtime configuration can be found at https://github.com/aplpolaris/promptfx/wiki/PromptFx#configuring-views-at-runtime.

---

## 🔌 Plugins

PromptFX supports two types of plugins that can be used to extend its functionality:

### UI View Plugins (NavigableWorkspaceView)

UI view plugins allow you to add custom views and tabs to the PromptFX interface. These plugins:
- Implement the `NavigableWorkspaceView` interface (typically by extending `NavigableWorkspaceViewImpl`)
- Can add new functionality to any tab category (API, Tools, Documents, Text, etc.)
- Are automatically discovered using Java's ServiceLoader mechanism
- Can be packaged as standalone JAR files

#### Creating a View Plugin

See the [promptfx-sample-plugin](https://github.com/aplpolaris/promptfx/tree/main/promptfx-sample-plugin) for a complete example. The key steps are:

1. **Implement NavigableWorkspaceView**: Create a class that extends `NavigableWorkspaceViewImpl`
   ```kotlin
   class MyPlugin : NavigableWorkspaceViewImpl<MyView>(
       "Category",        // Tab category
       "View Name",       // View name
       WorkspaceViewAffordance.INPUT_ONLY,
       MyView::class
   )
   ```

2. **Create the View**: Implement your UI using TornadoFX's `View` class
   ```kotlin
   class MyView : View("My Custom View") {
       override val root = vbox {
           // UI components
       }
   }
   ```

3. **Register the Plugin**: Create a service registration file containing your plugin's fully qualified class name:
   ```
   META-INF/services/tri.util.ui.NavigableWorkspaceView
   ```

4. **Build and Deploy**: Package as a JAR and place in the `config/plugins/` directory

#### Installing View Plugins

1. Build your plugin JAR file
2. Copy the JAR to the `config/plugins/` directory (create it if it doesn't exist)
3. Restart PromptFX - the plugin will be automatically discovered and loaded

### Model Integration Plugins (TextPlugin)

Model integration plugins allow you to add support for additional AI/ML model providers beyond OpenAI and Gemini. These plugins:
- Implement the `TextPlugin` interface
- Can provide chat models, embeddings, vision models, image generators, etc.
- Are loaded from the `config/modules/` directory
- Allow users to switch between different AI providers

#### Creating a Model Plugin

1. **Implement TextPlugin**: Create a class that implements the `TextPlugin` interface
2. **Provide Models**: Implement methods to return your model implementations (chat models, embedding models, etc.)
3. **Register the Plugin**: Create a service registration file containing your plugin's fully qualified class name:
   ```
   META-INF/services/tri.ai.core.TextPlugin
   ```
4. **Build and Deploy**: Package as a JAR and place in the `config/modules/` directory

See the [promptkt-openai](https://github.com/aplpolaris/promptfx/tree/main/promptkt-openai) and [promptkt-gemini](https://github.com/aplpolaris/promptfx/tree/main/promptkt-gemini) modules for reference implementations.

---

## 🌐 Configuring MCP Servers

The Model Context Protocol (MCP) allows PromptFX to connect to external servers that provide prompts, tools, and resources. The `mcp-servers.yaml` file configures which MCP servers are available.

### File Structure

```yaml
servers:
  # Embedded server with default libraries
  embedded:
    type: embedded
    description: "Embedded MCP server with default prompt and tool libraries"
  
  # Embedded server with custom prompts
  custom-embedded:
    type: embedded
    description: "Embedded MCP server with custom prompts"
    promptLibraryPath: "/path/to/custom/prompts.yaml"
  
  # Test server for development
  test:
    type: test
    description: "Test server with sample prompts and tools"
    includeDefaultPrompts: true
    includeDefaultTools: true
  
  # Remote MCP server via HTTP
  remote-http:
    type: http
    description: "Remote MCP server via HTTP"
    url: "http://localhost:8080/mcp"
  
  # Remote MCP server via stdio (process-based)
  remote-stdio:
    type: stdio
    description: "MCP server via stdio"
    command: "npx"
    args: ["-y", "@modelcontextprotocol/server-filesystem", "/tmp"]
    env:
      NODE_ENV: "production"
```

### Server Types

- **embedded**: Runs an MCP server in-memory with PromptFX
  - Optional: `promptLibraryPath` - Path to custom prompt library YAML file
  
- **test**: Test server with built-in sample prompts and tools
  - Optional: `includeDefaultPrompts` - Include default prompt library (true/false)
  - Optional: `includeDefaultTools` - Include default tool library (true/false)
  
- **http**: Connect to a remote MCP server over HTTP
  - Required: `url` - HTTP endpoint URL
  
- **stdio**: Connect to an MCP server via standard input/output
  - Required: `command` - Command to execute
  - Optional: `args` - Command-line arguments (array)
  - Optional: `env` - Environment variables (map)

### Configuration

- **File Location**: Place the file at `config/mcp-servers.yaml` in the working directory, or customize the built-in servers by editing the default configuration in the PromptFX resources
- **Server Names**: Each server must have a unique name (used as the key in the YAML)
- **Description**: Human-readable description shown in the UI

For more details on MCP servers, see the [promptkt-mcp module documentation](https://github.com/aplpolaris/promptfx/tree/main/promptkt-mcp).

---

## 📚 Resources

- **Main Repository**: [PromptFX on GitHub](https://github.com/aplpolaris/promptfx)
- **📖 Wiki & Docs**: [PromptFX Wiki](https://github.com/aplpolaris/promptfx/wiki)
- **📦 Releases**: [Download Latest Release](https://github.com/aplpolaris/promptfx/releases)
