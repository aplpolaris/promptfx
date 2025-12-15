#!/bin/bash
#
# Test script for MCP HTTP Server
#

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
  }'

# List prompts
echo -e "\n3. List Prompts:"
curl -s -X POST $SERVER_URL/mcp \
  -H "Content-Type: application/json" \
  -d '{
    "jsonrpc": "2.0",
    "id": 2,
    "method": "prompts/list"
  }'

# List tools
echo -e "\n4. List Tools:"
curl -s -X POST $SERVER_URL/mcp \
  -H "Content-Type: application/json" \
  -d '{
    "jsonrpc": "2.0",
    "id": 3,
    "method": "tools/list"
  }'

echo -e "\n=================================="
echo "Testing complete!"
