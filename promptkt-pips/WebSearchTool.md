# WebSearchTool Documentation

The `WebSearchTool` is a standard web search tool that provides access to web search capabilities without requiring API keys. It uses DuckDuckGo's search service to retrieve relevant web results.

## Features

- **No API Keys Required**: Uses DuckDuckGo HTML interface
- **Configurable Results**: Return 1-10 results per search (default: 5)
- **JSON Schema Validation**: Strict input validation
- **Error Handling**: Graceful degradation on failures
- **Thread Safe**: Built with Ktor HTTP client

## JSON Schema

```json
{
  "type": "object",
  "properties": {
    "query": {
      "type": "string",
      "description": "The search query to execute"
    },
    "max_results": {
      "type": "integer",
      "description": "Maximum number of results to return (default: 5, max: 10)",
      "minimum": 1,
      "maximum": 10
    }
  },
  "required": ["query"]
}
```

## Usage Examples

### Basic Search
```json
{
  "query": "machine learning"
}
```

### Search with Custom Result Limit
```json
{
  "query": "artificial intelligence trends 2024",
  "max_results": 3
}
```

## Response Format

### Successful Response
```json
{
  "query": "machine learning",
  "results": [
    {
      "title": "Machine Learning - Wikipedia",
      "url": "https://en.wikipedia.org/wiki/Machine_learning",
      "description": "Machine learning is a method of data analysis that automates analytical model building..."
    },
    {
      "title": "What is Machine Learning? | IBM",
      "url": "https://www.ibm.com/topics/machine-learning",
      "description": "Machine learning is a branch of artificial intelligence (AI) and computer science..."
    }
  ]
}
```

### Error Response
```json
{
  "query": "search query",
  "error": "Search failed: Network timeout",
  "results": []
}
```

## Tool Integration

The `WebSearchTool` extends `JsonTool` and can be used with:

1. **JsonToolExecutor**: For LLM function calling
2. **Workflow Systems**: As a workflow solver
3. **Direct Usage**: Programmatic search queries

### Kotlin Usage
```kotlin
import tri.ai.tool.WebSearchTool
import kotlinx.serialization.json.*

val tool = WebSearchTool()
val input = buildJsonObject {
    put("query", "Kotlin programming")
    put("max_results", 5)
}

val result = tool.run(input)
tool.close() // Clean up HTTP client
```

## Implementation Details

- **HTTP Client**: Ktor with OkHttp engine
- **HTML Parsing**: Regex-based (no external dependencies)  
- **Search Provider**: DuckDuckGo HTML interface
- **Result Clamping**: Automatically limits results between 1-10
- **Error Handling**: Returns error information in consistent JSON format

## Testing

The tool includes comprehensive tests:
- JSON schema validation
- Parameter validation and clamping  
- Error handling
- Response format verification
- Resource cleanup

Run tests with:
```bash
mvn test -Dtest=WebSearchToolTest -pl promptkt-pips
```

## Limitations

- Depends on DuckDuckGo HTML structure (may need updates if they change)
- No advanced search features (filters, date ranges, etc.)
- Results limited to 10 per query
- No search result ranking or relevance scoring
- Simple regex parsing may miss some edge cases