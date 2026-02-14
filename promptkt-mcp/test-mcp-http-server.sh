#!/bin/bash
# Test script for MCP HTTP Server
# This script tests the MCP HTTP server by sending various requests

set -e

# Configuration
HOST="localhost"
PORT="${1:-8080}"
BASE_URL="http://${HOST}:${PORT}"
MCP_URL="${BASE_URL}/mcp"
HEALTH_URL="${BASE_URL}/health"

echo "================================================"
echo "Testing MCP HTTP Server"
echo "================================================"
echo "Server URL: ${BASE_URL}"
echo "MCP Endpoint: ${MCP_URL}"
echo ""

# Color codes for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# Function to test an endpoint
test_request() {
    local description=$1
    local data=$2
    local expected_key=$3
    
    echo -e "${YELLOW}Testing: ${description}${NC}"
    
    response=$(curl -s -X POST "${MCP_URL}" \
        -H "Content-Type: application/json" \
        -d "${data}")
    
    echo "Response: ${response}"
    
    if echo "${response}" | grep -q "${expected_key}"; then
        echo -e "${GREEN}✓ PASSED${NC}"
    else
        echo -e "${RED}✗ FAILED${NC}"
        return 1
    fi
    echo ""
}

# Test 1: Health check
echo -e "${YELLOW}Testing: Health Check${NC}"
health_response=$(curl -s "${HEALTH_URL}")
echo "Response: ${health_response}"
if [ "${health_response}" = "OK" ]; then
    echo -e "${GREEN}✓ PASSED${NC}"
else
    echo -e "${RED}✗ FAILED${NC}"
    exit 1
fi
echo ""

# Test 2: Initialize
test_request "Initialize Connection" \
    '{"jsonrpc":"2.0","id":1,"method":"initialize","params":{"protocolVersion":"2025-06-18","capabilities":{},"clientInfo":{"name":"test-client","version":"1.0.0"}}}' \
    '"result"'

# Test 3: List Prompts
test_request "List Prompts" \
    '{"jsonrpc":"2.0","id":2,"method":"prompts/list"}' \
    '"prompts"'

# Test 4: List Tools
test_request "List Tools" \
    '{"jsonrpc":"2.0","id":3,"method":"tools/list"}' \
    '"tools"'

# Test 5: List Resources
test_request "List Resources" \
    '{"jsonrpc":"2.0","id":4,"method":"resources/list"}' \
    '"result"'

# Test 6: List Resource Templates
test_request "List Resource Templates" \
    '{"jsonrpc":"2.0","id":5,"method":"resources/templates/list"}' \
    '"result"'

echo "================================================"
echo -e "${GREEN}All tests completed!${NC}"
echo "================================================"
