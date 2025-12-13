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

## Testing with a Shell Script

Create a test script `test-mcp-server.sh`:

```bash
#!/bin/bash

SERVER_URL="http://localhost:8080"

echo "Testing MCP Server at $SERVER_URL"
echo "=================================="

# Health check
echo -e "\n1. Health Check:"
curl -s $SERVER_URL/health
echo

# Initialize
echo -e "\n2. Initialize:"
curl -s -X POST $SERVER_URL/mcp \
  -H "Content-Type: application/json" \
  -d '{
    "jsonrpc": "2.0",
    "id": 1,
    "method": "initialize",
    "params": {
      "protocolVersion": "2025-06-18",
      "clientInfo": {"name": "test-client", "version": "1.0.0"}
    }
  }' | jq .

# List prompts
echo -e "\n3. List Prompts:"
curl -s -X POST $SERVER_URL/mcp \
  -H "Content-Type: application/json" \
  -d '{
    "jsonrpc": "2.0",
    "id": 2,
    "method": "prompts/list"
  }' | jq .

# List tools
echo -e "\n4. List Tools:"
curl -s -X POST $SERVER_URL/mcp \
  -H "Content-Type: application/json" \
  -d '{
    "jsonrpc": "2.0",
    "id": 3,
    "method": "tools/list"
  }' | jq .

echo -e "\n=================================="
echo "Testing complete!"
```

Make it executable and run:

```bash
chmod +x test-mcp-server.sh
./test-mcp-server.sh
```

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
