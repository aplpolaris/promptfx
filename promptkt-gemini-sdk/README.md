# PromptKt Gemini SDK Plugin

This module provides support for Google's Gemini AI models using the official [Google Cloud Vertex AI Java SDK](https://github.com/googleapis/java-genai).

## Overview

The `promptkt-gemini-sdk` plugin implements the `TextPlugin` interface to provide access to Gemini models through Google's official SDK. This is an alternative to the `promptkt-gemini` plugin, which uses direct REST API calls.

## Features

- **Chat Models**: Uses multimodal/vision-language models for chat functionality
- **Multimodal Models**: gemini-1.5-flash, gemini-1.5-flash-8b, gemini-1.5-pro
- **Vision-Language Models**: gemini-1.5-flash, gemini-1.5-flash-8b, gemini-1.5-pro
- **Text Completion**: Supported via chat models

**Note**: Embedding models are not yet supported in this SDK-based plugin. For embedding functionality, please use the `promptkt-gemini` plugin which uses the REST API directly.

## Configuration

The plugin requires three pieces of configuration:

### 1. API Key (Required)

You can provide the API key in one of two ways:

- **File**: Create a file named `apikey-gemini.txt` in the application root directory containing your API key
- **Environment Variable**: Set the `GEMINI_API_KEY` environment variable

### 2. Project ID (Required)

You can provide the Google Cloud project ID in one of two ways:

- **File**: Create a file named `gemini-project-id.txt` in the application root directory containing your project ID
- **Environment Variable**: Set the `GEMINI_PROJECT_ID` environment variable

### 3. Location (Optional)

The location/region for the Vertex AI endpoint. Defaults to `us-central1` if not specified.

You can override this in one of two ways:

- **File**: Create a file named `gemini-location.txt` in the application root directory containing your preferred location
- **Environment Variable**: Set the `GEMINI_LOCATION` environment variable

Common locations include:
- `us-central1` (default)
- `us-east4`
- `europe-west4`
- `asia-northeast1`

## Setup

### Getting a Google Cloud API Key and Project ID

1. Go to the [Google Cloud Console](https://console.cloud.google.com/)
2. Create a new project or select an existing one
3. Enable the Vertex AI API for your project
4. Create API credentials (Service Account or API Key)
5. Note your Project ID from the project settings

### Maven Dependency

```xml
<dependency>
    <groupId>com.googlecode.blaisemath</groupId>
    <artifactId>promptkt-gemini-sdk</artifactId>
    <version>0.13.1-SNAPSHOT</version>
</dependency>
```

## Usage

The plugin is automatically discovered via Java's ServiceLoader mechanism. Once configured, it will be available alongside other text plugins.

### Example: Using the Plugin

```kotlin
import tri.ai.core.TextPlugin
import tri.ai.geminisdk.GeminiSdkPlugin

// The plugin is automatically loaded
val plugin = GeminiSdkPlugin()

// Check if configured
if (plugin.client.isConfigured()) {
    // Get available models
    val chatModels = plugin.chatModels()
    
    // Use a chat model
    val chatModel = chatModels.first()
    // ... use the model
}
```

### Example: Generating Text

```kotlin
import tri.ai.core.*
import tri.ai.geminisdk.GeminiSdkPlugin

val plugin = GeminiSdkPlugin()
val model = plugin.textCompletionModels().first { it.modelId == "gemini-1.5-flash" }

// Generate text completion
val result = model.complete(
    text = "Write a short poem about coding",
    variation = MChatVariation(temperature = 0.7),
    tokens = 500
)

println(result.outputInfo.textContent())
```

## Differences from promptkt-gemini

The `promptkt-gemini` plugin uses direct HTTP/REST API calls via Ktor, while this plugin uses Google's official SDK.

**Advantages of promptkt-gemini (REST API)**:
- Supports embedding models (text-embedding-004, etc.)
- Simpler dependencies
- More direct control over API calls

**Advantages of promptkt-gemini-sdk (Official SDK)**:
- Better integration with Google Cloud authentication
- More consistent API updates from Google
- Additional enterprise features
- Potentially better performance through optimized client implementations

**Recommendation**: Use `promptkt-gemini` (REST API) for embedding functionality. Use this SDK-based plugin when you need better Google Cloud integration or enterprise features.

## Testing

The module includes unit tests that verify model index functionality. To run tests with actual API calls:

1. Configure your API key, project ID, and location as described above
2. Run tests without the exclusion tag:

```bash
mvn test -pl promptkt-gemini-sdk
```

Note: By default, tests tagged with `gemini-sdk` are excluded to avoid requiring API credentials for CI/CD.

## Dependencies

- **google-cloud-vertexai**: 1.24.0 - Google's official Vertex AI SDK
- **promptkt-core**: Core PromptKt interfaces and utilities
- **kotlin-stdlib**: Kotlin standard library
- **kotlinx-coroutines**: For async/await support

## License

This module is licensed under the Apache License 2.0, same as the parent PromptFx project.
