package tri.ai.gemini

import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable
import java.net.URI

// https://ai.google.dev/api/generate-content#method:-models.generatecontent

const val GEMINI_ROLE_USER = "user"
const val GEMINI_ROLE_MODEL = "model"

@Serializable
data class GenerateContentRequest(
    val contents: List<Content>,
    val tools: List<Tool>? = null,
    val toolConfig: ToolConfig? = null,
    val safetySettings: List<SafetySetting>? = null,
    val systemInstruction: Content? = null, // this is a beta feature
    val generationConfig: GenerationConfig? = null,
    val cachedContent: String? = null
) {
    constructor(content: Content, systemInstruction: Content? = null, generationConfig: GenerationConfig? = null) :
            this(listOf(content), systemInstruction = systemInstruction, generationConfig = generationConfig)
}

//region [Content] and [Part]

@Serializable
data class Content(
    val parts: List<Part>,
    val role: String? = null
) {
    init { require(role in listOf(null, GEMINI_ROLE_USER, GEMINI_ROLE_MODEL)) { "Invalid role: $role" } }

    companion object {
        /** Content with a single text part. */
        fun text(text: String) = Content(listOf(Part(text)), GEMINI_ROLE_USER)
        /** Content with a system message. */
        fun systemMessage(text: String) = text(text) // TODO - support for system messages if Gemini supports it
    }
}

@Serializable
data class Part(
    // [Part] is a union type that can contain only one of the following accepted types
    val text: String? = null,
    val inlineData: Blob? = null,
    val functionCall: FunctionCall? = null,
    val functionResponse: FunctionResponse? = null,
    val fileData: FileData? = null,
    val executableCode: ExecutableCode? = null,
    val codeExecutionResult: CodeExecutionResult? = null
)

const val MIME_TYPE_TEXT = "text/plain"
const val MIME_TYPE_JSON = "application/json"
const val MIME_TYPE_JPEG = "image/jpeg"
const val MIME_TYPE_PNG = "image/png"

@Serializable
data class Blob(
    val mimeType: String,
    val data: String
) {
    companion object {
        /** Generate blob from image URL. */
        fun image(url: URI) = image(url.toASCIIString())

        /** Generate blob from image URL. */
        fun image(urlStr: String): Blob {
            if (urlStr.startsWith("data:image/")) {
                val mimeType = urlStr.substringBefore(";base64,").substringAfter("data:")
                val base64 = urlStr.substringAfter(";base64,")
                return Blob(mimeType, base64)
            } else {
                throw UnsupportedOperationException("Only data URLs are supported for images.")
            }
        }
    }
}

// TODO - [args] is a "Struct" which is essentially a map of key-value pairs, unsure how to serialize properly with kotlin
@Serializable
data class FunctionCall(
    val name: String,
    val args: Map<String, String>
)

// TODO - [response] is a "Struct" which is essentially a map of key-value pairs, unsure how to serialize properly with kotlin
@Serializable
data class FunctionResponse(
    val name: String,
    val response: Map<String, String>
)

@Serializable
data class FileData(
    val mimeType: String? = null,
    val fileUri: String
)

@Serializable
data class ExecutableCode(
    val language: CodeLanguage,
    val code: String,
)

enum class CodeLanguage {
    LANGUAGE_UNSPECIFIED,
    PYTHON
}

@Serializable
data class CodeExecutionResult(
    val outcome: CodeExecutionOutcome,
    val output: String? = null
)

enum class CodeExecutionOutcome {
    OUTCOME_UNSPECIFIED,
    OUTCOME_OK,
    OUTCOME_FAILED,
    OUTCOME_DEADLINE_EXCEEDED
}

//endregion

//region [Tool] and [ToolConfig]

@Serializable
data class Tool(
    val functionDeclarations: List<FunctionDeclaration>? = null,
    val googleSearchRetrieval: GoogleSearchRetrieval? = null,
    val codeExecution: CodeExecution? = null
)

@Serializable
data class FunctionDeclaration(
    val name: String,
    val description: String,
    val parameters: Schema? = null
)

@Serializable
data class Schema(
    val type: Type,
    val format: String? = null,
    val description: String? = null,
    val nullable: Boolean? = null,
    val `enum`: List<String>? = null,
    val maxItems: Int? = null,
    val minItems: Int? = null,
    val properties: Map<String, Schema>? = null,
    val required: List<String>? = null,
    val items: List<Schema>? = null
)

@Serializable
enum class Type {
    TYPE_UNSPECIFIED,
    STRING,
    NUMBER,
    INTEGER,
    BOOLEAN,
    ARRAY,
    OBJECT
}

@Serializable
data class GoogleSearchRetrieval(
    val dynamicRetrievalConfig: DynamicRetrievalConfig? = null
)

@Serializable
data class DynamicRetrievalConfig(
    val mode: DynamicRetrievalConfigMode,
    val dynamicThreshold: Float? = null
)

@Serializable
enum class DynamicRetrievalConfigMode {
    MODE_UNSPECIFIED,
    MODE_DYNAMIC
}

@Serializable
class CodeExecution { }

@Serializable
data class ToolConfig(
    val functionCallingConfig: FunctionCallingConfig? = null
)

@Serializable
data class FunctionCallingConfig(
    val mode: FunctionCallingConfigMode? = null,
    val allowedFunctionNames: List<String>? = null
)

enum class FunctionCallingConfigMode {
    MODE_UNSPECIFIED,
    AUTO,
    ANY,
    NONE
}

//endregion

//region CONFIGS

@Serializable
data class SafetySetting(
    val category: HarmCategory,
    val threshold: HarmBlockThreshold
)

enum class HarmCategory {
    HARM_CATEGORY_UNSPECIFIED,
    HARM_CATEGORY_DEROGATORY,
    HARM_CATEGORY_TOXICITY,
    HARM_CATEGORY_VIOLENCE,
    HARM_CATEGORY_SEXUAL,
    HARM_CATEGORY_MEDICAL,
    HARM_CATEGORY_DANGEROUS,
    HARM_CATEGORY_HARASSMENT,
    HARM_CATEGORY_HATE_SPEECH,
    HARM_CATEGORY_SEXUALLY_EXPLICIT,
    HARM_CATEGORY_DANGEROUS_CONTENT,
    HARM_CATEGORY_CIVIC_INTEGRITY
}

enum class HarmBlockThreshold {
    HARM_BLOCK_THRESHOLD_UNSPECIFIED,
    BLOCK_LOW_AND_ABOVE,
    BLOCK_MEDIUM_AND_ABOVE,
    BLOCK_ONLY_HIGH,
    BLOCK_NONE,
    OFF
}

private val ALLOWED_MIMES = setOf(null, MIME_TYPE_TEXT, MIME_TYPE_JPEG)

@Serializable
data class GenerationConfig(
    val stopSequences: List<String>? = null,
    val responseMimeType: String? = null,
    val candidateCount: Int? = null,
    val maxOutputTokens: Int? = null,
    val temperature: Double? = null,
    val topP: Double? = null,
    val topK: Int? = null
) {
    init {
        require(responseMimeType in ALLOWED_MIMES) { "Unexpected responseMimeType: $responseMimeType" }
    }
}

//endregion

@Serializable
data class GenerateContentResponse(
    var error: Error? = null,
    var candidates: List<Candidate>? = null
)

@Serializable
data class Candidate(
    val content: Content
)

@Serializable
data class Error(
    val message: String
)