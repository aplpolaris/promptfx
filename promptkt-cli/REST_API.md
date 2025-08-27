# Document Q&A REST API

This REST API provides access to the Document Q&A functionality for asking questions against a fixed document set.

## Starting the Server

### Option 1: Using the CLI command
```bash
# Using the document CLI with server subcommand
java -cp target/promptkt-cli-*-jar-with-dependencies.jar tri.ai.cli.DocumentCliRunner document --root /path/to/documents server --port 8080

# Example with test documents
java -cp target/promptkt-cli-*-jar-with-dependencies.jar tri.ai.cli.DocumentCliRunner document --root /tmp/test-docs server --port 8080
```

### Option 2: Using the standalone server
```bash
# Using the standalone server main class
java -cp target/promptkt-cli-*-jar-with-dependencies.jar tri.ai.rest.DocumentQaRestMain /path/to/documents 8080

# Example with test documents
java -cp target/promptkt-cli-*-jar-with-dependencies.jar tri.ai.rest.DocumentQaRestMain /tmp/test-docs 8080
```

## API Endpoints

### GET /
Returns basic information about the API service.

**Response:**
```json
{
  "service": "PromptFx Document Q&A API",
  "version": "1.0.0",
  "endpoints": ["/folders", "/qa"]
}
```

### GET /folders
Lists available document folders.

**Response:**
```json
{
  "folders": ["folder1", "folder2"],
  "currentFolder": ""
}
```

### POST /qa
Ask a question against the document set.

**Request:**
```json
{
  "question": "What is artificial intelligence?",
  "folder": "",
  "numResponses": 1,
  "historySize": 1
}
```

**Response:**
```json
{
  "answer": "Artificial Intelligence (AI) is a field of computer science...",
  "question": "What is artificial intelligence?",
  "folder": "",
  "success": true,
  "error": null
}
```

## Example Usage

### Using curl
```bash
# Check server status
curl http://localhost:8080/

# List available folders
curl http://localhost:8080/folders

# Ask a question
curl -X POST http://localhost:8080/qa \
  -H "Content-Type: application/json" \
  -d '{"question": "What is artificial intelligence?"}'
```

### Using Python
```python
import requests

# Ask a question
response = requests.post('http://localhost:8080/qa', json={
    'question': 'What programming languages are mentioned in the documents?'
})

result = response.json()
print(result['answer'])
```

## Configuration

The server uses the following configuration:
- **Root Path**: Directory containing document folders (can be set via command line)
- **Port**: Server port (default: 8080)
- **Host**: Server host (default: 0.0.0.0)
- **Chat Model**: Uses the default chat model configured in the system
- **Embedding Model**: Uses the default embedding model for document retrieval

## Requirements

- OpenAI API key (set in `apikey.txt` file or `OPENAI_API_KEY` environment variable)
- Document folder containing text files (.txt, .pdf, .doc, .docx)
- Java 17+