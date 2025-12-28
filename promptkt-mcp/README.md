# PromptKt MCP

This module provides MCP (Model Context Protocol) server and client implementations for Kotlin.

## Features

- **MCP Server over HTTP**: Run an MCP server that can be accessed via HTTP POST requests
- **MCP Server over Stdio**: Run an MCP server that communicates via standard input/output
- **MCP Client**: Connect to remote MCP servers via HTTP or Stdio
- **Embedded Server**: Run an MCP server in-memory with custom prompts and tools

## Running the MCP HTTP Server

To start the MCP HTTP server:

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
- Number of available prompts and tools

## Running the MCP Stdio Server

To start the MCP Stdio server:

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
- Display number of available prompts and tools on startup

The Stdio server is ideal for:
- Integration with MCP clients that use stdio transport
- Running as a subprocess in other applications
- Testing with command-line tools

## Testing the MCP HTTP Server

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

## Testing the MCP Stdio Server

The Stdio server can be tested by sending JSON-RPC 2.0 requests to its standard input. Here are some examples:

### Initialize Connection

```bash
echo '{"jsonrpc":"2.0","id":1,"method":"initialize","params":{"protocolVersion":"2025-06-18","clientInfo":{"name":"test-client","version":"1.0.0"}}}' | mvn exec:java -Dexec.mainClass="tri.ai.mcp.stdio.McpServerStdioMainKt" 2>/dev/null
```

### List Available Prompts

```bash
echo '{"jsonrpc":"2.0","id":2,"method":"prompts/list"}' | mvn exec:java -Dexec.mainClass="tri.ai.mcp.stdio.McpServerStdioMainKt" 2>/dev/null
```

### List Available Tools

```bash
echo '{"jsonrpc":"2.0","id":3,"method":"tools/list"}' | mvn exec:java -Dexec.mainClass="tri.ai.mcp.stdio.McpServerStdioMainKt" 2>/dev/null
```

Note: The `2>/dev/null` redirects stderr (server status messages) so you only see the JSON-RPC responses on stdout.

## Testing with a Shell Script

See test script `test-mcp-server.sh` for a sample test script.

## MCP Protocol

The server implements the [Model Context Protocol](https://modelcontextprotocol.io/) specification, which uses JSON-RPC 2.0 over HTTP.

### Supported Methods

- `initialize` - Initialize the connection with the server
- `prompts/list` - List all available prompts
- `prompts/get` - Get a specific prompt with arguments
- `tools/list` - List all available tools
- `tools/call` - Call a specific tool with arguments

## Configuration

The default server configuration:
- Port: 8080 (can be customized via command-line argument)
- Prompts: Includes research-related prompts from the PromptLibrary
- Tools: Includes tools from StarterToolLibrary

To customize prompts and tools, modify the `McpServerHttpMain.kt` file or create your own main class.
