# promptrt

Command-line AI tools for the [PromptFx](https://github.com/aplpolaris/promptfx) ecosystem.

Requires Java 17+. API keys are read from environment variables (e.g. `OPENAI_API_KEY`, `ANTHROPIC_API_KEY`, `GEMINI_API_KEY`).

---

## Building

From the `promptrt/` directory:

```
mvn package -DskipTests
```

This produces two artifacts in `promptrt-cli/target/`:
- `promptrt-cli-<version>-jar-with-dependencies.jar` — the executable fat jar (~113 MB)
- `promptrt` / `promptrt.bat` — launch scripts that reference the jar by relative path

---

## Running

**Without installing** (run directly from the build output):

```
# Unix
promptrt-cli/target/promptrt

# Windows
promptrt-cli\target\promptrt.bat

# Or invoke the jar directly
java -jar promptrt-cli/target/promptrt-cli-<version>-jar-with-dependencies.jar
```

**After installing** (see below):

```
promptrt
```

---

## Installing locally

To copy the jar and scripts to `~/.local/lib/promptrt/` and `~/.local/bin/`:

```
mvn package -Pinstall-local -DskipTests
```

Then add `~/.local/bin` to your PATH if it isn't already:

```
# ~/.bashrc or ~/.zshrc
export PATH="$HOME/.local/bin:$PATH"
```

On Windows, add `%USERPROFILE%\.local\bin` to your user PATH via System Settings.

---

## Usage

`promptrt` launches an interactive REPL by default:

```
promptrt [--mode <name>] [--config <file>]
```

Available subcommands for non-interactive use:

| Command | Description |
|---|---|
| `promptrt chat <message>` | Send a single message and exit (scriptable) |
| `promptrt models` | List all loaded chat and embedding models |
| `promptrt providers` | List all loaded provider plugins |
| `promptrt config` | Show resolved config file and active modes |
| `promptrt batch` | Run a batch job (stub — use `/batch` inside the REPL) |

### REPL commands

Once inside the REPL, type `/help` to see all commands. Key ones:

| Command | Description |
|---|---|
| `/mode <name>` | Switch mode (`plain`, `memory`, `rag`, `agent`) |
| `/model <id>` | Override model for this session |
| `/provider <name>` | Switch provider (auto-selects first available model) |
| `/memory on\|off` | Toggle persistent memory |
| `/rag <path>` | Enable RAG against a local document folder |
| `/tools on\|off` | Enable tool/function calling |
| `/temp <n>` | Set temperature |
| `/json on\|off` | Request JSON output |
| `/system <text>` | Set system prompt |
| `/batch <file>` | Run a batch job from a YAML file |
| `/status` | Show current session configuration |
| `/models` | List available models grouped by provider |
| `/providers` | List available providers |
| `/reset` | Restore default mode and clear overrides |
| `/quit` | Exit |

### Config file

Optional YAML config at `~/.promptrt/config.yaml`:

```yaml
default_mode: plain   # plain | memory | rag | agent | <custom>

modes:
  custom:
    model: claude-3-5-sonnet
    provider: Anthropic
    memory: true

providers:
  anthropic:
    api_key_env: ANTHROPIC_API_KEY
```

---

## Other CLIs

Two additional entry points are bundled in the same jar:

**`DocumentCli`** — document Q&A and embedding workflows:
```
java -cp promptrt-cli-<version>-jar-with-dependencies.jar tri.ai.cli.DocumentCli --help
```
Subcommands: `chat`, `qa`, `embeddings`, `chunk`

**`McpCli`** — MCP (Model Context Protocol) server client/server:
```
java -cp promptrt-cli-<version>-jar-with-dependencies.jar tri.ai.cli.McpCli --help
```
Subcommands: `prompts-list`, `prompts-get`, `prompts-execute`, `tools-list`, `tools-execute`, `resources-list`, `resources-read`, `start`
