`promptkt-cli` provides a number of command-line runnables that can be executed as follows. See full details below.
```
java -cp XX tri.ai.cli.McpCliKt
java -cp XX tri.ai.cli.PromptBatchRunnerKt

java -cp XX tri.ai.cli.DocumentCliRunner

java -cp XX tri.ai.cli.SimpleChatCliKt
java -cp XX tri.ai.cli.MemoryChatCliKt
```

**NOTE: CLI utilities were changed significantly in versions 0.10.0+.**

Command-line utilities in PromptFx use [Clikt](https://ajalt.github.io/clikt/).

## Document Utilities

`tri.ai.cli.DocumentCliRunner.kt` provides a number of utilities for working with documents, including document scraping, chunking, embedding vectors, and document Q&A. These are enabled as separate "commands" with the same root executable.

```
Usage: document [<options>] <command> [<args>]

Options:
  --root=<path>       Root path containing folders or documents
  --folder=<text>     Folder containing documents (relative to root path)
  --model=<text>      Chat/completion model to use (default gpt-3.5-turbo)
  --embedding=<text>  Embedding model to use (default text-embedding-ada-002)
  --temp=<float>      Temperature for completion (default 0.5)
  --max-tokens=<int>  Maximum tokens for completion (default 2000)
  -h, --help          Show this message and exit

Commands:
  chunk       Chunk documents into smaller pieces
  embeddings  Generate/update local embeddings file for a given folder
  qa          Ask a single question
  chat        Ask questions and switch between folders until done
```

Here is a sample command-line invocation (with a placeholder XX for the full jar file name):
```
java -cp XX tri.ai.cli.DocumentCliRunner --root=C:\\data\\chatgpt chat
```

### Document Chunking

The document `chunk` command generates chunks from documents, with the following arguments (where `options-doc` are the above document utilities options):

```
Usage: document [<options-doc>] chunk [<options>]

  Chunk documents into smaller pieces

Options:
  --reindex-all           Reindex all documents in the folder
  --reindex-new           Reindex new documents in the folder (default)
  --max-chunk-size=<int>  Maximum chunk size (# of characters) for embeddings
                          (default 1000)
  --index-file=<text>     Index file name for the documents (default docs.json)
  -h, --help              Show this message and exit
```

The document chunker supports extensions `.pdf`, `.docx`, `.doc`, and `.txt`, and will preprocess documents, extracting plaintext and metadata, and then break up the plaintext into chunks. The chunk information is saved in the index file, with the following schema:

```json
{
  "version" : "1.0",
  "metadata" : { },
  "docs" : [ {
    "metadata" : {
      "id" : "file.txt",
      "title" : "file",
      "dateTime" : [ 2025, 2, 13, 15, 49, 34, 630000000 ],
      "path" : "file:/D:/path-to-file/file.txt",
      "relativePath" : "file.txt"
    },
    "chunks" : [ {
      "first" : 0,
      "last" : 2670
    } ]
  } ]
}
```
This file can be loaded into the `Text Manager` view as described in [[Documents]].

In the future, we plan to add parameters to specify:
- option to include chunk text in JSON output
- alternate chunking strategies
- hierarchical and/or multiple simultaneous chunking strategies
- overlapping chunking strategies
- automatic embedding calculations

### Document Embeddings

The document `embeddings` command generates chunks *and* embeddings from documents, with the following options (where `options-doc` are the above document utilities options):

```
Usage: document [<options-doc>] embeddings [<options>]

  Generate/update local embeddings file for a given folder

Options:
  --reindex-all           Reindex all documents in the folder
  --reindex-new           Reindex new documents in the folder (default)
  --max-chunk-size=<int>  Maximum chunk size (# of characters) for embeddings
                          (default 1000)
  -h, --help              Show this message and exit
```

The document chunker works as described above. This runnable also calculates embedding vectors using the specified embedding model, saving the result to `embeddings2.json` in the same folder. Embeddings are associated with chunk metadata as shown here:
```json
{
  "version" : "1.0",
  "metadata" : {
    "id" : "test3",
    "path" : "file:/D:/path-to-file/"
  },
  "docs" : [ {
    "metadata" : {
      "id" : "file:/D:/path-to-file/file.txt",
      "path" : "file:/D:/path-to-file/file.txt"
    },
    "chunks" : [ {
      "first" : 0,
      "last" : 956,
      "attributes" : {
        "embeddings" : {
          "text-embedding-ada-002" : [ 0.0, 0.1 ]
        }
      }
    } ]
  } ]
}
```
This file overwrites the default file used in [[Documents]] views, and can be loaded into the `Text Manager` view as described in [[Documents]].

You can use this to calculate embeddings for multiple models by running the script multiple times with different options for `--embedding`. Note that `--reindex-all` will reset the file.

### Document Q&A

The document `qa` command will answer a question provided by a parameter. This will use the `embeddings2.json` file already in the path (or generate one if it does not already exist) and provide a response with citations using retrieval-augmented generation. The question should be surrounded in quotes if it includes spaces.

```
Usage: document qa [<options>] <question>

  Ask a single question

Options:
  -h, --help  Show this message and exit

Arguments:
  <question>  Question to ask about the documents.
```

#### Sample PowerShell Script for Generating a Set of Answers

Here is a sample PowerShell script that can be used to generate answers for a set of questions. This would require an API key file in the same directory, and look for a different question in each line of `questions.txt`, generating results saved in `output.txt`.
```ps1
# Print the current Java version
$javaVersion = java -version 2>&1
Write-Host "Current Java Version:"
Write-Host $javaVersion

# Define the root path to the collection of folders
$rootPath = "C:\path-to-folder\my-research-papers"

# Define the path to your jar file
$jarFilePath = "C:\path-to-jar\promptfx-x.x.x-jar-with-dependencies.jar"

# Define the file containing the list of questions
$questionsFile = "questions.txt"

# Define the output file to store the results
$outputFile = "outputs.txt"

# Clear the output file if it exists to start fresh
Clear-Content $outputFile

# Read the list of questions from the file
$questions = Get-Content $questionsFile

# Loop through each question
foreach ($question in $questions) {
    # Construct the command
    Write-Host $question
    $command = "java -cp `"$jarFilePath`" tri.ai.cli.DocumentCliRunner --root=$rootPath qa `"$question`""
    Write-Host $command
    
    # Execute the command and capture the output
    $output = Invoke-Expression $command
    
    # Append the question and the corresponding output to the output file
    Add-Content $outputFile "`nQuestion: $question"
    Add-Content $outputFile "Answer: $output"
    Add-Content $outputFile "`n---`n"
}

Write-Host "Processing complete. Results saved in $outputFile."

# Pause to allow the user to view the output before closing
Read-Host -Prompt "Press Enter to exit"
```

### Document Chat

The document `chat` command enables a command-line chat interface for asking questions of the current path/folder. The user can ask repeated questions until exiting, and optionally switch between folders if there are multiple subfolders relative to the top-level path. (For this feature, the initial command-line parameters should specific both `root` and `folder`.) The user can use the command `bye` to exit.

```
Usage: document chat [<options>]

  Ask questions and switch between folders until done

Options:
  -h, --help  Show this message and exit
```

## Prompt Utilities

### `McpCliKt` (0.12.0+)

`tri.ai.cli.McpCliKt` provides an MCP-compatible prompt server, supporting both local (built-in prompt libraries) and remote servers. This is a *beta* feature, with not all functionality fully supported.

```
Usage: mcp-prompt [<options>] <command> [<args>]...

  Interface to MCP prompt servers - list, fill, and execute prompts, list and
  execute tools, or start a local server

Options:
  -s, --server=<text>          MCP server URL (use 'local' for local server)
  -p, --prompt-library=<text>  Custom prompt library file or directory path
                               (for local server only)
  -t, --tool-library=<text>    FUTURE TBD -- Custom tool library file or
                               directory path (for local server only)
  -v, --verbose                Verbose output
  -h, --help                   Show this message and exit

Commands:
  prompts-list     List all available prompts in the MCP server
  prompts-get      Get a prompt filled with arguments
  prompts-execute  Execute a prompt - fill it with arguments and display the
                   result after calling an LLM
  tools-list       List all available tools from in the MCP server
  tools-execute    Execute a tool with input and display the result
  start            Start an MCP server on stdio, with locally provided prompts
                   and tools
```

Sample usage:
```
# Show help
java -cp XX tri.ai.cli.McpCliKt --help

# List available prompts
java -cp XX tri.ai.cli.McpCliKt prompts-list

# Use custom prompt library
java -cp XX tri.ai.cli.McpCliKt --prompt-library ./my-prompts prompts-list

# Get filled prompt
java -cp XX tri.ai.cli.McpCliKt prompts-get "text-translate/translate" input="Hello world" instruct="French"

# Translate text with verbose output
java -cp XX tri.ai.cli.McpCliKt --verbose prompts-execute "text-translate/translate" input="Hello world" instruct="French"

# Execute with custom McpCliKt 
java -cp XX tri.ai.cli.McpCliKt prompts-execute --model "gpt-4" "examples/hello-world" name="World"

# List available tools
java -cp XX tri.ai.cli.McpCliKt tools-list

# Execute tool with given inputs
java -cp XX tri.ai.cli.McpCliKt tools-execute web_search query="best downhill mountain bike" max_results=3

# Start local MCP server on stdio
java -cp XX tri.ai.cli.McpCliKt start
```

### `PromptBatchRunner`

`tri.ai.cli.PromptBatchRunnerKt` is designed to execute a batch series of prompts and compile the results in an output file.

```
Usage: prompt-batch [<options>] <inputfile> <outputfile>

Options:
  --database  Output as database format
  -h, --help  Show this message and exit

Arguments:
  <inputfile>   input file
  <outputfile>  output file
```

Sample usage:
```
java -cp XX tri.ai.cli.PromptBatchRunnerKt
```

Input/output files should be either `.json`, `.yaml`, or `.yml` files. Input files correspond to the object defined in `tri.ai.prompt.trace.batch.AiPromptBatchCyclic.kt`. Here is a sample input file:

```json
{
  "id" : "test",
  "model" : "gpt-3.5-turbo",
  "prompt" : [ "Give me a friendly saying in {{language}}." ],
  "promptParams" : {
    "language" : [ "French", "German", "Japanese" ]
  },
  "runs" : 6
}
```

The output file will include a unique trace for each prompt execution. The trace includes information about the prompt, the model, the result, and execution details. If the `database` flag is set, these will be divided into different tables, resulting in a much smaller file size. Otherwise, the result will be a list of prompt trace objects, with potentially a lot of duplicated information (such as model configuration).

Here is a sample output:
```json
[ {
  "prompt" : {
    "prompt" : "Give me a friendly saying in {{language}}.",
    "promptParams" : {
      "language" : "French"
    }
  },
  "model" : {
    "modelId" : "gpt-3.5-turbo"
  },
  "exec" : {
    "queryTokens" : 15,
    "responseTokens" : 17,
    "responseTimeMillis" : 741
  },
  "output" : {
    "outputs" : [ "\"Bon courage et bonne journée!\" (Good luck and have a great day!)" ]
  },
  "uuid" : "6f917b13-8b7e-465e-9f47-6cbfeeb51f52"
} ]
```

Here is a sample output with the `--database` flag:
```json
{
  "traces" : [ {
    "uuid" : "75dc67db-f1f7-402e-b729-ea6b3ad8f795",
    "promptIndex" : 0,
    "modelIndex" : 0,
    "execIndex" : 0,
    "outputIndex" : 0
  } ],
  "prompts" : [ {
    "prompt" : "Give me a friendly saying in {{language}}.",
    "promptParams" : {
      "language" : "French"
    }
  } ],
  "models" : [ {
    "modelId" : "gpt-3.5-turbo"
  } ],
  "execs" : [ {
    "queryTokens" : 15,
    "responseTokens" : 11,
    "responseTimeMillis" : 534
  } ],
  "outputs" : [ {
    "outputs" : [ "\"Bonne journée ! - Have a nice day!\"" ]
  } ]
}
```

# Command-Line Chat (Experimental)

**NOTE: CLI utilities were changed significantly in versions 0.10.0+.**

## `SimpleChatCli`

`tri.ai.cli.SimpleChatCliKt` is a simple command-line chat using one of the registered models and a configurable history size.

```
Usage: chat-simple [<options>]

Options:
  --model=<text>       Chat model or LLM to use (default gpt-3.5-turbo)
  --historySize=<int>  Maximum chat history size (default 10)
  --verbose            Verbose logging
  -h, --help           Show this message and exit
```

Sample usage:
```
java -cp XX tri.ai.cli.SimpleChatCliKt
```

## `MemoryChatCli`

`tri.ai.cli.MemoryChatCliKt` is an experimental chat where chat history is saved along with embedding vectors and periodic conversation summaries. These embeddings are used to find relevant topics within a long-running conversation, allowing the model to speak to parts of the conversation that happened much earlier, or even within a different session. The conversation, including user input, responses, and conversation summaries, is saved to `memory.json`, and this history is loaded back into memory during the next chat.

```
Usage: chat-memory [<options>]

Options:
  --model=<text>      Chat model or LLM to use (default gpt-3.5-turbo)
  --embedding=<text>  Embedding model to use (default text-embedding-ada-002)
  --verbose           Verbose logging
  -h, --help          Show this message and exit
```
Sample usage:

```
java -cp XX tri.ai.cli.MemoryChatCliKt
```