# PromptKt MCP

This module provides MCP (Model Context Protocol) server and client implementations for Kotlin.
See the [Model Context Protocol](https://modelcontextprotocol.io/) specification for details on the protocol and methods.

## Features

- **MCP Server over HTTP**: Run an MCP server that can be accessed via HTTP POST requests
- **MCP Server over Stdio**: Run an MCP server that communicates via standard input/output
- **MCP Client**: Connect to remote MCP servers via HTTP or Stdio
- **Embedded Server**: Run an MCP server in-memory with custom prompts and tools

### Supported Methods

- `initialize` - Initialize the connection with the server
- `prompts/list` - List all available prompts
- `prompts/get` - Get a specific prompt with arguments
- `tools/list` - List all available tools
- `tools/call` - Call a specific tool with arguments
- `resources/list` - List available resources (if supported)
- `resources/read` - Get a specific resource (if supported)
- `resources/templates/list` - List available resource templates (if supported)

## Connecting to External MCP Servers

You can connect to external MCP servers using the `McpProviderHttp` and `McpProviderStdio` classes.

### Streamable HTTP MCP Servers

MCP servers over `Streamable HTTP` are persistent HTTP servers that support client/server communications via HTTP.
See https://modelcontextprotocol.io/specification/2025-11-25/basic/transports#streamable-http. Use `McpProviderHttp` to connect to these servers:

```kotlin
val provider = McpProviderHttp("http://mcp-server.example.com/mcp")
val capabilities = provider.getCapabilities()
val prompts = provider.listPrompts()
val tools = provider.listTools()
val result = provider.callTool("tool-name", mapOf("arg1" to "value1"))
val textResult = result.content.first() as McpContent.Text
println(textResult)
provider.close()
```

### Stdio MCP Servers

MCP servers over `stdio` typically function as a local process that you can launch and send/receive JSON-RPC MCP messages.
Use `McpProviderStdio` to launch these servers locally and use them in code:

```kotlin
val provider = McpProviderStdio(
    command = "path/to/executable",
    args = listOf("arg1", "arg2"),
    env = mapOf("var1" to "value1", "var2" to "value2")
)
val capabilities = provider.getCapabilities()
val prompts = provider.listPrompts()
val tools = provider.listTools()
val result = provider.callTool("tool-name", mapOf("arg1" to "value1"))
val textResult = result.content.first() as McpContent.Text
println(textResult)
provider.close()
```

## MCP Server Configuration

PromptKt MCP supports configuring multiple MCP servers via YAML or JSON configuration files. The configuration format is **compatible with Claude Desktop and OpenAI products**, supporting both explicit type declaration and type inference.

### Configuration Formats

Two formats are supported:

#### 1. Claude Desktop / OpenAI Format (Recommended)

Uses `mcpServers` as the top-level key and infers the server type from the fields present:

```json
{
  "mcpServers": {
    "filesystem": {
      "command": "npx",
      "args": ["-y", "@modelcontextprotocol/server-filesystem", "/tmp"]
    },
    "weather": {
      "command": "node",
      "args": ["/path/to/weather/server.js"],
      "env": {
        "API_KEY": "your-api-key"
      }
    },
    "brave-search": {
      "url": "http://localhost:8080/mcp"
    }
  }
}
```

YAML equivalent:

```yaml
mcpServers:
  filesystem:
    command: "npx"
    args: ["-y", "@modelcontextprotocol/server-filesystem", "/tmp"]
  
  weather:
    command: "node"
    args: ["/path/to/weather/server.js"]
    env:
      API_KEY: "your-api-key"
  
  brave-search:
    url: "http://localhost:8080/mcp"
```

#### 2. PromptFx Format (Also Supported)

Uses `servers` as the top-level key and includes an explicit `type` field:

```yaml
servers:
  filesystem:
    type: stdio
    command: "npx"
    args: ["-y", "@modelcontextprotocol/server-filesystem", "/tmp"]
  
  brave-search:
    type: http
    url: "http://localhost:8080/mcp"
  
  embedded:
    type: embedded
    description: "Embedded MCP server with default libraries"
```

### Type Inference

When using the Claude Desktop format (without explicit `type` field), the server type is automatically inferred:

- **Stdio server**: Has a `command` field
- **HTTP server**: Has a `url` field
- **Embedded/Test servers**: Must use explicit `type` field (PromptFx-specific)

### Loading Configuration

```kotlin
// Load from file (auto-detects format)
val registry = McpProviderRegistry.loadFromFile("config/mcp-servers.yaml")

// Get a provider
val provider = registry.getProvider("filesystem")
val prompts = provider?.listPrompts()
```

## Hosting MCP Servers

You can host your own MCP servers using the provided HTTP and Stdio server implementations.
The examples here are for testing only and not intended for production use.

The default server configuration:
- Prompts: Includes research-related prompts from the PromptLibrary
- Tools: Includes tools from StarterToolLibrary
- Resources: none

To customize prompts, tools, and resources, modify the `McpServerHttpMain.kt` or `McpServerStdioMain.kt` file or create your own main class.

### Running the MCP HTTP Server

The MCP HTTP server supports POST requests only. It has the following configuration:
- Port: 8080 (can be customized via command-line argument)

To start an MCP HTTP server:

```bash
# Build the project first
mvn clean install

# Run the HTTP server (default port 8080)
mvn exec:java -Dexec.mainClass="tri.ai.mcp.http.McpServerHttpMainKt"

# Or specify a custom port
mvn exec:java -Dexec.mainClass="tri.ai.mcp.http.McpServerHttpMainKt" -Dexec.args="9000"
```

The server will start and display:
- Server URL
- MCP endpoint path
- Health check endpoint
- Number of available prompts, tools, and resources

#### Testing the MCP HTTP Server

Check if the server is running (expect response `OK`):
```bash
curl http://localhost:8080/health
```

Send an initialize request to the MCP server:
```bash
curl -X POST http://localhost:8080/mcp \
  -H "Content-Type: application/json" \
  -d '{
    "jsonrpc": "2.0",
    "id": 1,
    "method": "initialize",
    "params": {
      "protocolVersion": "2025-06-18",
      "capabilities": {},
      "clientInfo": {
        "name": "test-client",
        "version": "1.0.0"
      }
    }
  }'
```

List available prompts:
```bash
curl -X POST http://localhost:8080/mcp \
  -H "Content-Type: application/json" \
  -d '{
    "jsonrpc": "2.0",
    "id": 2,
    "method": "prompts/list"
  }'
```

Get a prompt:
```bash
curl -X POST http://localhost:8080/mcp \
  -H "Content-Type: application/json" \
  -d '{
    "jsonrpc": "2.0",
    "id": 3,
    "method": "prompts/get",
    "params": {
      "name": "<prompt-name>",
      "arguments": {}
    }
  }'
```

List available tools:
```bash
curl -X POST http://localhost:8080/mcp \
  -H "Content-Type: application/json" \
  -d '{
    "jsonrpc": "2.0",
    "id": 4,
    "method": "tools/list"
  }'
```

Call a tool:
```bash
curl -X POST http://localhost:8080/mcp \
  -H "Content-Type: application/json" \
  -d '{
    "jsonrpc": "2.0",
    "id": 5,
    "method": "tools/call",
    "params": {
      "name": "<tool-name>",
      "arguments": {
        "arg1": "value1"
      }
    }
  }'
```

### Running the MCP Stdio Server

To start the MCP Stdio server (with sample prompts and tools):

```bash
# Build the project first
mvn clean install

# Run the Stdio server
mvn exec:java -Dexec.mainClass="tri.ai.mcp.stdio.McpServerStdioMainKt"
```

The server will:
- Read JSON-RPC 2.0 requests from standard input (stdin)
- Write JSON-RPC 2.0 responses to standard output (stdout)
- Write server status messages to standard error (stderr)
- Display number of available prompts, tools, and resources on startup

## Testing the MCP Stdio Server

The Stdio server can be tested by sending JSON-RPC 2.0 requests to its standard input. Here are some sample messages that can be copied directly into the input:

Initialize connection:
```json
{"jsonrpc":"2.0","id":1,"method":"initialize","params":{"protocolVersion":"2025-06-18","capabilities":{},"clientInfo":{"name":"test-client","version":"1.0.0"}}}
```

List Available Prompts:
```json
{"jsonrpc":"2.0","id":2,"method":"prompts/list"}
```

List Available Tools:
```json
{"jsonrpc":"2.0","id":3,"method":"tools/list"}
```

## Additional Notes

**MCP Inspector** is useful for testing MCP servers: https://modelcontextprotocol.io/docs/tools/inspector. Run with
```bash
npx @modelcontextprotocol/inspector <command>`
```

Run a test MCP server over `stdio` with:
```bash
npx @modelcontextprotocol/server-everything
```