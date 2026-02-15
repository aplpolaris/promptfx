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
├── config/                        # YAML configuration files
│   ├── openai-models.yaml
│   ├── openai-api-config.yaml
│   ├── gemini-models.yaml
│   ├── ollama-models.yaml
│   ├── views.yaml
│   ├── modes.yaml
│   ├── starship.yaml
│   └── plugins/                   # Plugin configuration
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

| File | Purpose                                                    |
|------|------------------------------------------------------------|
| `openai-models.yaml` | Available OpenAI models                                    |
| `openai-api-config.yaml` | OpenAI API configuration settings                         |
| `gemini-models.yaml` | Available Google Gemini models                             |
| `ollama-models.yaml` | Available Ollama models (local model support)              |
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

### 📝 Custom Prompts

Custom prompt templates can be added or modified in the `prompts/` folder:

| File | Purpose                                                    |
|------|------------------------------------------------------------|
| `custom-prompts.yaml` | Custom prompt templates that override or extend defaults   |

The `prompts/` folder is scanned recursively, so you can organize your prompts into subdirectories if desired.

Additional information on runtime configuration can be found at https://github.com/aplpolaris/promptfx/wiki/PromptFx#configuring-views-at-runtime.

---

## 📚 Resources

- **Main Repository**: [PromptFX on GitHub](https://github.com/aplpolaris/promptfx)
- **📖 Wiki & Docs**: [PromptFX Wiki](https://github.com/aplpolaris/promptfx/wiki)
- **📦 Releases**: [Download Latest Release](https://github.com/aplpolaris/promptfx/releases)
