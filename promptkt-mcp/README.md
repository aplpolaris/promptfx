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

### Working with Resources

MCP resources provide access to data sources like files, databases, or other external content. Resources can be listed, read, and accessed via templates.

#### Listing Resources

```kotlin
val provider = McpProviderHttp("http://mcp-server.example.com/mcp")
val resources = provider.listResources()
resources.forEach { resource ->
    println("Resource: ${resource.name}")
    println("  URI: ${resource.uri}")
    println("  Description: ${resource.description}")
    println("  MIME Type: ${resource.mimeType}")
}
```

#### Reading Resource Content

```kotlin
val resourceContent = provider.readResource("file:///sample-data.txt")
resourceContent.contents.forEach { content ->
    println("URI: ${content.uri}")
    println("MIME Type: ${content.mimeType}")
    if (content.text != null) {
        println("Text content: ${content.text}")
    } else if (content.blob != null) {
        println("Binary content (base64): ${content.blob}")
    }
}
```

#### Resource Templates

Some MCP servers support resource templates, which allow dynamic resource URIs:

```kotlin
val templates = provider.listResourceTemplates()
templates.forEach { template ->
    println("Template: ${template.name}")
    println("  URI Template: ${template.uriTemplate}")
    println("  Description: ${template.description}")
}
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

You can use the provided test script to verify the server is working correctly:

```bash
# Run the test script (assumes server is running on localhost:8080)
./test-mcp-http-server.sh

# Or test with a custom port
./test-mcp-http-server.sh 9000
```

Alternatively, you can manually test individual endpoints:

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

You can use the provided test script to verify the server is working correctly:

```bash
# Run the test script with default mvn command
./test-mcp-stdio-server.sh

# Or specify a custom server command
./test-mcp-stdio-server.sh node path/to/mcp-server.js
```

Alternatively, you can manually test the server:

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

### Running an Embedded MCP Server

The embedded MCP server runs in-memory within your application. This is useful for:
- Testing MCP functionality without external dependencies
- Providing MCP capabilities within your Kotlin application
- Building custom MCP servers with your own prompts, tools, and resources

#### Basic Embedded Server

```kotlin
import tri.ai.mcp.McpProviderEmbedded
import tri.ai.mcp.tool.McpToolLibraryStarter
import tri.ai.prompt.PromptLibrary

// Create an embedded server with default prompts and tools
val prompts = PromptLibrary()
val tools = McpToolLibraryStarter()
val provider = McpProviderEmbedded(prompts, tools)

// Use it like any other MCP provider
val capabilities = provider.getCapabilities()
val promptList = provider.listPrompts()
val toolList = provider.listTools()
```

#### Embedded Server with Resources

```kotlin
import tri.ai.mcp.McpResource

// Define custom resources
val resources = listOf(
    McpResource(
        uri = "file:///data/config.json",
        name = "Configuration",
        description = "Application configuration file",
        mimeType = "application/json"
    ),
    McpResource(
        uri = "file:///data/readme.txt",
        name = "README",
        description = "Project documentation",
        mimeType = "text/plain"
    )
)

// Create embedded server with resources
val provider = McpProviderEmbedded(prompts, tools, resources)

// List and read resources
val resourceList = provider.listResources()
val content = provider.readResource("file:///data/config.json")
```

#### Custom Prompts and Tools

```kotlin
import tri.ai.prompt.trace.Prompt

// Add custom prompts
val customPrompt = Prompt(
    id = "custom-prompt",
    template = "Analyze the following data: {{data}}",
    promptInfo = mapOf(
        "category" to "analysis",
        "description" to "Custom data analysis prompt"
    )
)
prompts.addPrompt(customPrompt)

// Create embedded server with custom prompts
val provider = McpProviderEmbedded(prompts, tools)
```

### MCP Client Usage

The MCP client functionality is provided through the `McpProvider` interface. All provider implementations (`McpProviderHttp`, `McpProviderStdio`, `McpProviderEmbedded`) implement this interface, allowing you to:

1. **Connect to any MCP server** using the appropriate provider type
2. **Use the same API** regardless of the transport mechanism
3. **Switch between providers** without changing your code

```kotlin
// Example: Using different providers with the same API
fun demonstrateClient(provider: McpProvider) {
    // Initialize the connection
    provider.initialize()
    
    // Get server capabilities
    val capabilities = provider.getCapabilities()
    println("Server capabilities: $capabilities")
    
    // List and use prompts
    val prompts = provider.listPrompts()
    if (prompts.isNotEmpty()) {
        val promptResult = provider.getPrompt(
            prompts.first().name,
            mapOf("arg1" to "value1")
        )
        println("Prompt result: $promptResult")
    }
    
    // List and call tools
    val tools = provider.listTools()
    if (tools.isNotEmpty()) {
        val toolResult = provider.callTool(
            tools.first().name,
            mapOf("param1" to "value1")
        )
        println("Tool result: $toolResult")
    }
    
    // List and read resources
    val resources = provider.listResources()
    if (resources.isNotEmpty()) {
        val resourceContent = provider.readResource(resources.first().uri)
        println("Resource content: $resourceContent")
    }
    
    provider.close()
}

// Use with different provider types
demonstrateClient(McpProviderHttp("http://localhost:8080/mcp"))
demonstrateClient(McpProviderStdio("./mcp-server", listOf("arg1")))
demonstrateClient(McpProviderEmbedded(PromptLibrary(), McpToolLibraryStarter()))
```

### Server Registration with McpProviderRegistry

The `McpProviderRegistry` provides a centralized way to configure and manage multiple MCP providers. This is particularly useful for applications that need to connect to multiple MCP servers.

#### Configuration File Format

Create a configuration file (JSON or YAML) to define your MCP providers:

**Example: mcp-config.json**
```json
{
  "servers": {
    "local-embedded": {
      "type": "embedded",
      "description": "Embedded server with default libraries"
    },
    "research-server": {
      "type": "http",
      "description": "Research tools MCP server",
      "url": "http://localhost:8081/mcp"
    },
    "code-analysis": {
      "type": "stdio",
      "description": "Code analysis MCP server",
      "command": "/usr/local/bin/code-analyzer",
      "args": ["--mode", "mcp"],
      "env": {
        "ANALYSIS_LEVEL": "detailed"
      }
    },
    "test-server": {
      "type": "test",
      "description": "Test server with sample data",
      "includeDefaultPrompts": true,
      "includeDefaultTools": true,
      "includeDefaultResources": true
    }
  }
}
```

**Example: mcp-config.yaml**
```yaml
servers:
  local-embedded:
    type: embedded
    description: Embedded server with default libraries
    
  research-server:
    type: http
    description: Research tools MCP server
    url: http://localhost:8081/mcp
    
  code-analysis:
    type: stdio
    description: Code analysis MCP server
    command: /usr/local/bin/code-analyzer
    args:
      - --mode
      - mcp
    env:
      ANALYSIS_LEVEL: detailed
      
  test-server:
    type: test
    description: Test server with sample data
    includeDefaultPrompts: true
    includeDefaultTools: true
    includeDefaultResources: true
```

#### Using the Registry

```kotlin
import tri.ai.mcp.McpProviderRegistry

// Load registry from a configuration file
val registry = McpProviderRegistry.loadFromFile("mcp-config.json")
// or: McpProviderRegistry.loadFromYaml(File("mcp-config.yaml"))

// List available providers
val providerNames = registry.listProviderNames()
println("Available providers: $providerNames")

// Get and use a specific provider
val provider = registry.getProvider("research-server")
if (provider != null) {
    provider.initialize()
    val tools = provider.listTools()
    println("Available tools: ${tools.map { it.name }}")
    provider.close()
}

// Use default registry (includes embedded and test providers)
val defaultRegistry = McpProviderRegistry.default()
val testProvider = defaultRegistry.getProvider("test")
```

#### Provider Configuration Types

1. **Embedded Provider** (`type: embedded`):
   - Runs in-memory within your application
   - Optional `promptLibraryPath` to load prompts from a specific location

2. **HTTP Provider** (`type: http`):
   - Connects to a remote MCP server via HTTP
   - Requires `url` parameter

3. **Stdio Provider** (`type: stdio`):
   - Launches an external process that communicates via stdin/stdout
   - Requires `command` parameter
   - Optional `args` and `env` parameters

4. **Test Provider** (`type: test`):
   - Special provider for testing with sample data
   - Optional flags: `includeDefaultPrompts`, `includeDefaultTools`, `includeDefaultResources`

## Additional Notes

**MCP Inspector** is useful for testing MCP servers: https://modelcontextprotocol.io/docs/tools/inspector. Run with
```bash
npx @modelcontextprotocol/inspector <command>`
```

Run a test MCP server over `stdio` with:
```bash
npx @modelcontextprotocol/server-everything
```