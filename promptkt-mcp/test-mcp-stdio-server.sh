#!/bin/bash
# Test script for MCP Stdio Server
# This script tests the MCP Stdio server by sending JSON-RPC requests via stdin

set -e

echo "================================================"
echo "Testing MCP Stdio Server"
echo "================================================"
echo ""

# Color codes for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Determine the command to run the server
if [ -n "$1" ]; then
    # Use provided command
    SERVER_CMD="$@"
else
    # Default: use mvn exec:java
    SERVER_CMD="mvn -q exec:java -Dexec.mainClass=tri.ai.mcp.stdio.McpServerStdioMainKt"
fi

echo -e "${BLUE}Server command: ${SERVER_CMD}${NC}"
echo ""

# Create a temporary directory for test files
TEST_DIR=$(mktemp -d)
INPUT_FILE="${TEST_DIR}/input.json"
OUTPUT_FILE="${TEST_DIR}/output.json"
ERROR_FILE="${TEST_DIR}/error.log"

cleanup() {
    echo ""
    echo -e "${YELLOW}Cleaning up...${NC}"
    # Kill the server process if still running
    if [ -n "${SERVER_PID}" ]; then
        kill ${SERVER_PID} 2>/dev/null || true
    fi
    rm -rf "${TEST_DIR}"
}

trap cleanup EXIT

# Function to send a request and check response
test_stdio_request() {
    local description=$1
    local request=$2
    local expected_pattern=$3
    
    echo -e "${YELLOW}Testing: ${description}${NC}"
    echo "Request: ${request}"
    
    # Send request to stdin and capture stdout
    echo "${request}" > "${INPUT_FILE}"
    
    # Read one line of JSON response
    response=$(cat "${INPUT_FILE}" | ${SERVER_CMD} 2>"${ERROR_FILE}" | head -n 1)
    
    echo "Response: ${response}"
    
    if echo "${response}" | grep -q "${expected_pattern}"; then
        echo -e "${GREEN}✓ PASSED${NC}"
        return 0
    else
        echo -e "${RED}✗ FAILED${NC}"
        echo "Expected pattern: ${expected_pattern}"
        if [ -s "${ERROR_FILE}" ]; then
            echo "Server errors:"
            cat "${ERROR_FILE}"
        fi
        return 1
    fi
    echo ""
}

# Alternative approach: Start server once and send multiple requests
echo -e "${BLUE}Starting MCP Stdio server...${NC}"
echo ""

# Start the server in background
${SERVER_CMD} > "${OUTPUT_FILE}" 2>"${ERROR_FILE}" &
SERVER_PID=$!

# Wait a moment for server to start
sleep 2

# Check if server is still running
if ! kill -0 ${SERVER_PID} 2>/dev/null; then
    echo -e "${RED}Failed to start server${NC}"
    if [ -s "${ERROR_FILE}" ]; then
        echo "Server errors:"
        cat "${ERROR_FILE}"
    fi
    exit 1
fi

echo -e "${GREEN}Server started (PID: ${SERVER_PID})${NC}"
echo ""

# Function to send requests to running server
send_request() {
    local description=$1
    local request=$2
    local expected_pattern=$3
    
    echo -e "${YELLOW}Testing: ${description}${NC}"
    echo "Request: ${request}"
    
    # Send request to the server's stdin
    echo "${request}" >&${SERVER_INPUT_FD}
    
    # Wait briefly for response
    sleep 1
    
    # Check output file for response
    if tail -n 1 "${OUTPUT_FILE}" | grep -q "${expected_pattern}"; then
        echo -e "${GREEN}✓ PASSED${NC}"
        return 0
    else
        echo -e "${RED}✗ FAILED (pattern not found)${NC}"
        echo "Last response line:"
        tail -n 1 "${OUTPUT_FILE}"
        return 1
    fi
    echo ""
}

# Open file descriptor for server input
exec {SERVER_INPUT_FD}>&${SERVER_PID}

echo "================================================"
echo "Sending test requests..."
echo "================================================"
echo ""

# Note: For stdio testing, we'll use a simpler approach
# Send all requests in sequence and check the output

cat > "${INPUT_FILE}" << 'EOF'
{"jsonrpc":"2.0","id":1,"method":"initialize","params":{"protocolVersion":"2025-06-18","capabilities":{},"clientInfo":{"name":"test-client","version":"1.0.0"}}}
{"jsonrpc":"2.0","id":2,"method":"prompts/list"}
{"jsonrpc":"2.0","id":3,"method":"tools/list"}
{"jsonrpc":"2.0","id":4,"method":"resources/list"}
{"jsonrpc":"2.0","id":5,"method":"resources/templates/list"}
EOF

echo -e "${BLUE}Sending batch requests...${NC}"
cat "${INPUT_FILE}"
echo ""

# Send all requests at once
cat "${INPUT_FILE}" | ${SERVER_CMD} 2>"${ERROR_FILE}" > "${OUTPUT_FILE}" &
BATCH_PID=$!

# Wait for completion
wait ${BATCH_PID}

echo -e "${BLUE}Responses received:${NC}"
cat "${OUTPUT_FILE}"
echo ""

# Validate responses
echo "================================================"
echo "Validating responses..."
echo "================================================"

response_count=$(wc -l < "${OUTPUT_FILE}")
if [ "${response_count}" -ge 5 ]; then
    echo -e "${GREEN}✓ Received ${response_count} responses${NC}"
else
    echo -e "${RED}✗ Expected 5 responses, got ${response_count}${NC}"
fi

if grep -q '"result"' "${OUTPUT_FILE}"; then
    echo -e "${GREEN}✓ Found successful result responses${NC}"
else
    echo -e "${RED}✗ No successful results found${NC}"
fi

if grep -q '"prompts"' "${OUTPUT_FILE}"; then
    echo -e "${GREEN}✓ Prompts list response found${NC}"
else
    echo -e "${YELLOW}⚠ Prompts list response not found (may be empty)${NC}"
fi

if grep -q '"tools"' "${OUTPUT_FILE}"; then
    echo -e "${GREEN}✓ Tools list response found${NC}"
else
    echo -e "${YELLOW}⚠ Tools list response not found (may be empty)${NC}"
fi

echo ""
echo "================================================"
echo -e "${GREEN}Testing completed!${NC}"
echo "================================================"
echo ""
echo "Note: The stdio server reads from stdin and writes to stdout."
echo "You can manually test it by piping JSON-RPC requests:"
echo ""
echo "  echo '{\"jsonrpc\":\"2.0\",\"id\":1,\"method\":\"initialize\",\"params\":{\"protocolVersion\":\"2025-06-18\",\"capabilities\":{},\"clientInfo\":{\"name\":\"test\",\"version\":\"1.0\"}}}' | ${SERVER_CMD}"
echo ""
