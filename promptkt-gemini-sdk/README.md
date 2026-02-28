# PromptKt Gemini SDK Plugin

This module provides support for Google's Gemini AI models using the official [java-genai library](https://github.com/googleapis/java-genai).

## Overview

The `promptkt-gemini-sdk` plugin implements the `TextPlugin` interface to provide access to Gemini models through Google's official java-genai SDK. This is an alternative to the `promptkt-gemini` plugin, which uses direct REST API calls.

## Features

- **Chat Models**: Uses multimodal/vision-language models for chat functionality
- **Multimodal Models**: gemini-1.5-flash, gemini-1.5-flash-8b, gemini-1.5-pro
- **Vision-Language Models**: gemini-1.5-flash, gemini-1.5-flash-8b, gemini-1.5-pro
- **Text Completion**: Supported via chat models

**Note**: Embedding models are not yet supported in this SDK-based plugin. For embedding functionality, please use the `promptkt-gemini` plugin which uses the REST API directly.

## Configuration

The plugin supports two modes of operation:

### Mode 1: Gemini Developer API (API Key Only)

This is the simplest configuration, requiring only an API key:

- **File**: Create a file named `apikey-gemini.txt` in the application root directory containing your API key
- **Environment Variable**: Set the `GEMINI_API_KEY` environment variable

### Mode 2: Vertex AI (Project ID + Location + API Key)

For Vertex AI mode, you need to provide:

1. **API Key** (Required)
   - **File**: Create a file named `apikey-gemini.txt` in the application root directory
   - **Environment Variable**: Set the `GEMINI_API_KEY` environment variable

2. **Project ID** (Required for Vertex AI)
   - **File**: Create a file named `gemini-project-id.txt` in the application root directory
   - **Environment Variable**: Set the `GEMINI_PROJECT_ID` environment variable

3. **Location** (Optional, defaults to `us-central1`)
   - **File**: Create a file named `gemini-location.txt` in the application root directory
   - **Environment Variable**: Set the `GEMINI_LOCATION` environment variable

Common locations include:
- `us-central1` (default)
- `us-east4`
- `europe-west4`
- `asia-northeast1`

**Note**: If you provide a project ID, the plugin will automatically use Vertex AI mode. If you only provide an API key (no project ID), it will use the Gemini Developer API mode.

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

## Testing

The module includes unit tests that verify model index functionality. To run tests with actual API calls:

1. Configure your API key, project ID, and location as described above
2. Run tests without the exclusion tag:

```bash
mvn test -pl promptkt-gemini-sdk
```

Note: By default, tests tagged with `gemini-sdk` are excluded to avoid requiring API credentials for CI/CD.

## Dependencies

- **google-genai**: Google's official java-genai library for Gemini API
- **promptkt-core**: Core PromptKt interfaces and utilities
- **kotlin-stdlib**: Kotlin standard library
- **kotlinx-coroutines**: For async/await support