#!/bin/bash
#
# Test script for MCP Stdio Server
# This script sends JSON-RPC messages to stdin and reads responses from stdout
#

MAIN_CLASS="tri.ai.mcp.McpServerStdioMainKt"

echo "Testing MCP Server over Stdio"
echo "=================================="
echo "Note: This requires a running MCP server process that communicates via stdio"
echo ""

# Function to send a JSON-RPC request and read response
send_request() {
  local request="$1"
  echo "Request: $request" >&2
  echo "$request"
}

# Test via process substitution with a command that would start an MCP stdio server
# For example: mvn exec:java -Dexec.mainClass="$MAIN_CLASS"

echo "To test stdio server, you need to:"
echo "1. Start the MCP stdio server in one terminal:"
echo "   mvn exec:java -Dexec.mainClass=\"$MAIN_CLASS\""
echo ""
echo "2. Pipe this script to it in another terminal:"
echo "   ./test-mcp-stdio-server.sh | mvn exec:java -Dexec.mainClass=\"$MAIN_CLASS\""
echo ""
echo "Sending test requests to stdout (pipe to an MCP stdio server):"
echo ""

# Initialize
send_request '{"jsonrpc":"2.0","id":1,"method":"initialize","params":{"protocolVersion":"2025-06-18","clientInfo":{"name":"test-client","version":"1.0.0"}}}'

# List prompts
send_request '{"jsonrpc":"2.0","id":2,"method":"prompts/list"}'

# List tools
send_request '{"jsonrpc":"2.0","id":3,"method":"tools/list"}'

# List resources
send_request '{"jsonrpc":"2.0","id":4,"method":"resources/list"}'

echo ""
echo "=================================="
echo "Testing complete! Responses will appear in the server output."
