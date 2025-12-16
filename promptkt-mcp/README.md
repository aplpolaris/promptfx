# PromptKt MCP

This module provides MCP (Model Context Protocol) server and client implementations for Kotlin.

## Features

- **MCP Server over HTTP**: Run an MCP server that can be accessed via HTTP POST requests
- **MCP Server over Stdio**: Run an MCP server that communicates via standard input/output
- **MCP Client**: Connect to remote MCP servers via HTTP or Stdio
- **Embedded Server**: Run an MCP server in-memory with custom prompts and tools
- **Resources**: Expose and access files, data, and other resources via MCP
- **Server Registry**: Configure and manage multiple MCP servers from YAML/JSON files

## Running the MCP HTTP Server

To start the MCP HTTP server:

```bash
# Build the project first
mvn clean install

# Run the HTTP server (default port 8080)
mvn exec:java -Dexec.mainClass="tri.ai.mcp.McpServerHttpMainKt"

# Or specify a custom port
mvn exec:java -Dexec.mainClass="tri.ai.mcp.McpServerHttpMainKt" -Dexec.args="9000"
```

The server will start and display:
- Server URL
- MCP endpoint path
- Health check endpoint
- Number of available prompts and tools

## Testing the MCP Server

### Health Check

Check if the server is running:

```bash
curl http://localhost:8080/health
```

Expected response: `OK`

### Initialize Connection

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
      "clientInfo": {
        "name": "test-client",
        "version": "1.0.0"
      }
    }
  }'
```

Expected response includes server info and capabilities.

### List Available Prompts

```bash
curl -X POST http://localhost:8080/mcp \
  -H "Content-Type: application/json" \
  -d '{
    "jsonrpc": "2.0",
    "id": 2,
    "method": "prompts/list"
  }'
```

Expected response contains a list of available prompts.

### Get a Specific Prompt

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

Replace `<prompt-name>` with an actual prompt name from the list.

### List Available Tools

```bash
curl -X POST http://localhost:8080/mcp \
  -H "Content-Type: application/json" \
  -d '{
    "jsonrpc": "2.0",
    "id": 4,
    "method": "tools/list"
  }'
```

Expected response contains a list of available tools.

### Call a Tool

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

Replace `<tool-name>` and arguments with actual tool name and required parameters.

## Testing with Shell Scripts

- **HTTP Server**: See `test-mcp-http-server.sh` for testing the HTTP server
- **Stdio Server**: See `test-mcp-stdio-server.sh` for testing the stdio server

## Running the MCP Stdio Server

The stdio server communicates via standard input and output streams, making it ideal for:
- Integration with command-line tools
- Process-to-process communication
- Editors and IDEs that support MCP via stdio

### Creating a Stdio Server

```kotlin
import tri.ai.mcp.*
import tri.ai.mcp.tool.StarterToolLibrary
import tri.ai.prompt.PromptLibrary

// Create embedded server with prompts and tools
val prompts = PromptLibrary()
val tools = StarterToolLibrary()
val adapter = McpServerEmbedded(prompts, tools)

// Create and start stdio server
val server = McpServerStdio(adapter)
server.startServer(System.`in`, System.out)
```

### Testing the Stdio Server

Use the test script to send JSON-RPC messages to a stdio server:

```bash
./test-mcp-stdio-server.sh | java -jar your-mcp-stdio-server.jar
```

Or test individual requests using echo and pipes:

```bash
echo '{"jsonrpc":"2.0","id":1,"method":"initialize","params":{}}' | java -jar server.jar
```

## Using MCP Clients

The MCP client adapters allow you to connect to remote MCP servers, either via HTTP or stdio.

### HTTP Client

Connect to a remote MCP server over HTTP:

```kotlin
import tri.ai.mcp.*

// Connect to HTTP server
val client = McpServerAdapterHttp("http://localhost:8080/mcp")

// List available prompts
val prompts = client.listPrompts()
prompts.forEach { println("Prompt: ${it.name}") }

// Get a specific prompt
val response = client.getPrompt("prompt-name", mapOf("arg1" to "value1"))

// List tools
val tools = client.listTools()

// Call a tool
val result = client.callTool("tool-name", mapOf("param" to "value"))

// Clean up
client.close()
```

### Stdio Client

Connect to a remote MCP server via stdio (launching an external process):

```kotlin
import tri.ai.mcp.*

// Launch external MCP server process
val client = McpServerAdapterStdio(
    command = "npx",
    args = listOf("-y", "@modelcontextprotocol/server-filesystem", "/tmp"),
    env = mapOf("NODE_ENV" to "production")
)

// Use the client (same API as HTTP client)
val prompts = client.listPrompts()
val tools = client.listTools()
val resources = client.listResources()

// Clean up (terminates the external process)
client.close()
```

## Using the Embedded Server

The embedded server runs in-memory and can be customized with your own prompts, tools, and resources.

### Basic Usage

```kotlin
import tri.ai.mcp.*
import tri.ai.mcp.tool.StarterToolLibrary
import tri.ai.prompt.PromptLibrary

// Create a prompt library
val prompts = PromptLibrary().apply {
    PromptLibrary.INSTANCE
        .list { it.category?.startsWith("research") == true }
        .forEach { addPrompt(it) }
}

// Create a tool library
val tools = StarterToolLibrary()

// Create embedded server
val server = McpServerEmbedded(prompts, tools)

// Use the server directly
val promptList = server.listPrompts()
val promptResponse = server.getPrompt("prompt-name", mapOf())
```

### With Resources

Resources allow you to expose files, data, or other content through the MCP protocol:

```kotlin
import tri.ai.mcp.*

// Define resources
val resources = listOf(
    McpResource(
        uri = "file:///data/config.json",
        name = "Configuration File",
        description = "Application configuration",
        mimeType = "application/json"
    ),
    McpResource(
        uri = "data://users/summary",
        name = "User Summary",
        description = "Summary of user statistics",
        mimeType = "text/plain"
    )
)

// Create embedded server with resources
val server = McpServerEmbedded(prompts, tools, resources)

// List available resources
val resourceList = server.listResources()

// Read a resource
val content = server.readResource("file:///data/config.json")
```

### Wrapping with HTTP or Stdio

You can wrap an embedded server with HTTP or stdio transport:

```kotlin
// Wrap with HTTP server
val httpServer = McpServerHttp(server, port = 8080)
httpServer.startServer()

// Or wrap with stdio server
val stdioServer = McpServerStdio(server)
stdioServer.startServer(System.`in`, System.out)
```

## MCP Resources

Resources in MCP allow servers to expose files, data, or other content that clients can access.

### Listing Resources

```bash
curl -X POST http://localhost:8080/mcp \
  -H "Content-Type: application/json" \
  -d '{
    "jsonrpc": "2.0",
    "id": 1,
    "method": "resources/list"
  }'
```

Expected response includes a list of available resources with their URIs, names, and metadata.

### Reading a Resource

```bash
curl -X POST http://localhost:8080/mcp \
  -H "Content-Type: application/json" \
  -d '{
    "jsonrpc": "2.0",
    "id": 2,
    "method": "resources/read",
    "params": {
      "uri": "file:///data/config.json"
    }
  }'
```

Expected response contains the resource contents (either as text or base64-encoded blob).

### Resource Templates

Resource templates allow dynamic resource URIs with parameters:

```kotlin
val template = McpResourceTemplate(
    uriTemplate = "file:///{path}",
    name = "File Access",
    description = "Access files by path"
)
```

## Server Registration

The `McpServerRegistry` provides a centralized way to configure and manage multiple MCP servers.

### Configuration File Format

Create a YAML or JSON file to define your servers:

```yaml
# mcp-servers.yaml
servers:
  # Embedded server with default libraries
  embedded:
    type: embedded
    description: "Embedded MCP server with default prompt and tool libraries"
  
  # Embedded server with custom prompt library
  custom:
    type: embedded
    description: "Embedded MCP server with custom prompts"
    promptLibraryPath: "/path/to/prompts.yaml"
  
  # Remote HTTP server
  remote-http:
    type: http
    description: "Remote MCP server via HTTP"
    url: "http://localhost:8080/mcp"
  
  # Remote stdio server (external process)
  filesystem:
    type: stdio
    description: "Filesystem MCP server"
    command: "npx"
    args: ["-y", "@modelcontextprotocol/server-filesystem", "/tmp"]
    env:
      NODE_ENV: "production"
  
  # Test server with samples
  test:
    type: test
    description: "Test server with sample prompts and tools"
    includeDefaultPrompts: true
    includeDefaultTools: true
    includeDefaultResources: true
```

### Using the Registry

```kotlin
import tri.ai.mcp.McpServerRegistry

// Load registry from file
val registry = McpServerRegistry.loadFromFile("mcp-servers.yaml")

// List available servers
val serverNames = registry.listServerNames()
println("Available servers: $serverNames")

// Get a specific server
val server = registry.getServer("embedded")
if (server != null) {
    val prompts = server.listPrompts()
    println("Prompts: ${prompts.size}")
}

// Use default registry
val defaultRegistry = McpServerRegistry.default()
```

### Configuration Types

1. **Embedded**: In-process server with custom prompt/tool libraries
   - `promptLibraryPath`: Optional path to custom prompt library

2. **HTTP**: Connect to a remote HTTP MCP server
   - `url`: The HTTP endpoint URL

3. **Stdio**: Launch and connect to an external process via stdio
   - `command`: Command to execute
   - `args`: Command arguments (optional)
   - `env`: Environment variables (optional)

4. **Test**: Pre-configured test server with samples
   - `includeDefaultPrompts`: Include sample prompts
   - `includeDefaultTools`: Include sample tools
   - `includeDefaultResources`: Include sample resources

## MCP Protocol

The server implements the [Model Context Protocol](https://modelcontextprotocol.io/) specification, which uses JSON-RPC 2.0 over HTTP.

### Supported Methods

- `initialize` - Initialize the connection with the server
- `prompts/list` - List all available prompts
- `prompts/get` - Get a specific prompt with arguments
- `tools/list` - List all available tools
- `tools/call` - Call a specific tool with arguments
- `resources/list` - List all available resources
- `resources/read` - Read the contents of a specific resource
- `resources/templates/list` - List resource URI templates

## Configuration

The default server configuration:
- Port: 8080 (can be customized via command-line argument)
- Prompts: Includes research-related prompts from the PromptLibrary
- Tools: Includes tools from StarterToolLibrary

To customize prompts and tools, modify the `McpServerHttpMain.kt` file or create your own main class.
