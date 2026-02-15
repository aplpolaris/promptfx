# PromptKt MCP

This module provides MCP (Model Context Protocol) server and client implementations for Kotlin.
See the [Model Context Protocol](https://modelcontextprotocol.io/) specification for details on the protocol and methods.

## Features

- **Connect to External MCP Servers**: Connect to any MCP server over HTTP or stdio using the provided client implementations. Supports both synchronous and asynchronous communication.
- **Host Your Own MCP Server**: Easily create and host your own MCP servers over HTTP or stdio with customizable prompts, tools, and resources. The provided server implementations are intended for testing and demonstration purposes and can be extended for production use.
- **Embedded Servers**: The server implementations can be embedded within your own applications, allowing you to expose MCP functionality as part of a larger system.
- **MCP Server Registration**: Register multiple MCP servers within the same application, each with its own configuration and capabilities.

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
// With SSE (Server-Sent Events) support enabled by default
val provider = McpProviderHttp("http://mcp-server.example.com/mcp")
val capabilities = provider.getCapabilities()
val prompts = provider.listPrompts()
val tools = provider.listTools()
val result = provider.callTool("tool-name", mapOf("arg1" to "value1"))
val textResult = result.content.first() as McpContent.Text
println(textResult)
provider.close()

// Disable SSE if server doesn't support it
val providerNoSse = McpProviderHttp("http://mcp-server.example.com/mcp", enableSse = false)
```

**SSE Support**: The `McpProviderHttp` client now supports the full MCP streamable HTTP specification, including:
- Session ID management via `Mcp-Session-Id` header
- Server-Sent Events (SSE) channel for receiving asynchronous server messages (via GET to `/mcp`)
- POST requests for sending commands (existing behavior)
- Handling both synchronous JSON-RPC responses and asynchronous SSE messages

The SSE connection is automatically established during initialization when a session ID is received. Set `enableSse = false` to disable SSE support for servers that only support simple POST-based communication.

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

#### Testing with "Server Everything"

You can use "Server Everything" to quickly test the `McpProviderStdio` client. This requires Node.js and npx to be installed.

Run a test MCP server over `stdio` with:
```bash
npx @modelcontextprotocol/server-everything
```

Test it in code:
```kotlin
val provider = McpProviderStdio(
    command = "npx", // may need to provide full path
    args = listOf("@modelcontextprotocol/server-everything")
)
val capabilities = provider.getCapabilities()
val tools = provider.listTools()
provider.close()
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

#### Testing the MCP HTTP Server with Curl

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

#### Testing the MCP Stdio Server

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

### Testing MCP Servers with MCP Inspector

**MCP Inspector** is useful for testing both HTTP and stdio MCP servers: https://modelcontextprotocol.io/docs/tools/inspector. 

Requires Node.js and npx. Run with a stdio server command:
```bash
npx @modelcontextprotocol/inspector
```
This will launch MCP inspector on `localhost` and provide a webpage for connecting to MCP servers and testing.

## Using Embedded MCP Servers

Embedded servers allow you to create MCP servers directly in your Kotlin code without launching external processes. This is useful for:
- Creating custom in-process servers with your own prompts and tools
- Testing MCP functionality without network communication
- Embedding MCP capabilities into larger applications
- Using as a base for HTTP or stdio server implementations

### Creating an Embedded Server

Use `McpProviderEmbedded` to create a server with custom prompts and tools:

```kotlin
import tri.ai.mcp.McpProviderEmbedded
import tri.ai.mcp.tool.McpToolLibraryStarter
import tri.ai.prompt.PromptLibrary

// Create with default libraries
val server = McpProviderEmbedded(
    prompts = PromptLibrary.INSTANCE,
    tools = McpToolLibraryStarter()
)

// Use the server
val capabilities = server.getCapabilities()
val prompts = server.listPrompts()
val tools = server.listTools()
val result = server.callTool("test_sentiment_analysis", mapOf("input_text" to "I love Kotlin!"))
server.close()
```

### Custom Prompts and Tools

You can create a server with your own prompt library and custom tools:

```kotlin
import tri.ai.prompt.PromptLibrary
import tri.ai.prompt.PromptDef
import tri.ai.mcp.tool.McpToolLibrary
import tri.ai.mcp.tool.McpToolMetadata
import tri.ai.mcp.tool.McpToolResponse

// Create custom prompt library
val customPrompts = PromptLibrary().apply {
    addPrompt(PromptDef(
        id = "my-prompt@1.0",
        name = "my-prompt",
        title = "My Custom Prompt",
        description = "A custom prompt template",
        template = "Process {{input}} and return results."
    ))
}

// Create custom tool library
val customTools = object : McpToolLibrary {
    override suspend fun listTools() = listOf(
        McpToolMetadata(
            name = "my-tool",
            description = "My custom tool",
            inputSchema = mapOf("type" to "object")
        )
    )
    
    override suspend fun getTool(name: String) = 
        listTools().find { it.name == name }
    
    override suspend fun callTool(name: String, args: Map<String, Any?>) =
        McpToolResponse.text("Tool result for: $args")
}

// Create embedded server with custom libraries
val customServer = McpProviderEmbedded(
    prompts = customPrompts,
    tools = customTools
)
```

### Using Embedded Servers with HTTP or Stdio

Embedded servers can be used as the backend for HTTP or stdio servers:

```kotlin
import tri.ai.mcp.http.McpServerHttp
import tri.ai.mcp.stdio.McpServerStdio

// Create embedded server
val embeddedServer = McpProviderEmbedded(
    prompts = PromptLibrary.INSTANCE,
    tools = McpToolLibraryStarter()
)

// Expose via HTTP
val httpServer = McpServerHttp(embeddedServer, port = 8080)
httpServer.startServer()

// Or expose via stdio
val stdioServer = McpServerStdio(embeddedServer)
stdioServer.startServer(System.`in`, System.out)
```

## Managing Multiple MCP Servers

The `McpProviderRegistry` allows you to configure and manage multiple MCP servers from a single configuration file. This is useful for:
- Managing multiple server connections in one place
- Switching between different server configurations
- Loading server configurations from JSON or YAML files
- Dynamically creating servers based on configuration

### Using the Default Registry

The default registry includes built-in "embedded" and "test" servers:

```kotlin
import tri.ai.mcp.McpProviderRegistry

// Load default registry
val registry = McpProviderRegistry.default()

// List available servers
val serverNames = registry.listProviderNames() // ["embedded", "test"]

// Get a specific server
val server = registry.getProvider("embedded")
val prompts = server?.listPrompts()
server?.close()
```

### Loading from Configuration Files

You can define server configurations in YAML or JSON:

**Example: mcp-servers.yaml**
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
  
  # Remote stdio server
  remote-stdio:
    type: stdio
    description: "Remote MCP server via stdio"
    command: "npx"
    args: ["-y", "@modelcontextprotocol/server-filesystem", "/tmp"]
    env:
      NODE_ENV: "production"
```

Load and use the registry:

```kotlin
// Load from file (auto-detects JSON or YAML)
val registry = McpProviderRegistry.loadFromFile("mcp-servers.yaml")

// Or explicitly load YAML or JSON
val yamlRegistry = McpProviderRegistry.loadFromYaml(File("mcp-servers.yaml"))
val jsonRegistry = McpProviderRegistry.loadFromJson(File("mcp-servers.json"))

// Use servers from registry
val server = registry.getProvider("remote-http")
val capabilities = server?.getCapabilities()
server?.close()
```

### Configuration Types

The registry supports four configuration types:

#### Embedded Server
```yaml
my-embedded:
  type: embedded
  description: "Embedded server description"
  promptLibraryPath: "/optional/path/to/prompts.yaml"  # Optional
```

#### HTTP Server
```yaml
my-http:
  type: http
  description: "HTTP server description"
  url: "http://example.com/mcp"
```

#### Stdio Server
```yaml
my-stdio:
  type: stdio
  description: "Stdio server description"
  command: "path/to/executable"
  args: ["arg1", "arg2"]  # Optional
  env:  # Optional
    VAR1: "value1"
```

#### Test Server
```yaml
my-test:
  type: test
  description: "Test server with samples"
  includeDefaultPrompts: true   # Optional, default: true
  includeDefaultTools: true     # Optional, default: true
  includeDefaultResources: true # Optional, default: true
```

### Example: Using Multiple Servers

```kotlin
val registry = McpProviderRegistry.loadFromFile("mcp-servers.yaml")

// Work with multiple servers
val embeddedServer = registry.getProvider("embedded")
val httpServer = registry.getProvider("remote-http")
val stdioServer = registry.getProvider("remote-stdio")

// List prompts from embedded server
val embeddedPrompts = embeddedServer?.listPrompts()

// List tools from HTTP server
val httpTools = httpServer?.listTools()

// Call tool on stdio server
val result = stdioServer?.callTool("test_aircraft_type_lookup", mapOf("type_code" to "B738"))

// Clean up
embeddedServer?.close()
httpServer?.close()
stdioServer?.close()
```