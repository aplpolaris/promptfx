/*-
 * #%L
 * tri.promptfx:promptkt
 * %%
 * Copyright (C) 2023 - 2025 Johns Hopkins University Applied Physics Laboratory
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */
package tri.ai.gemini

import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import java.net.URI

// https://ai.google.dev/api/generate-content#method:-models.generatecontent

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
    val role: ContentRole? = null
) {
    companion object {
        /** Content with a single text part. */
        fun text(text: String) = Content(listOf(Part(text)), ContentRole.user)
        /** Content with a system message. */
        fun systemMessage(text: String) = text(text) // TODO - support for system messages if Gemini supports it
    }
}

@Serializable
enum class ContentRole {
    user, model
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

// https://ai.google.dev/gemini-api/docs/document-processing
const val MIME_TYPE_TEXT = "text/plain"
const val MIME_TYPE_ENUM = "text/x.enum"
const val MIME_TYPE_CSV = "text/csv"
const val MIME_TYPE_HTML = "text/html"
const val MIME_TYPE_MD = "text/md"
const val MIME_TYPE_RTF = "text/rtf"
const val MIME_TYPE_XML = "text/xml"
const val MIME_TYPE_JSON = "application/json"
const val MIME_TYPE_PDF = "application/pdf"

// https://ai.google.dev/gemini-api/docs/vision
const val MIME_TYPE_JPEG = "image/jpeg"
const val MIME_TYPE_PNG = "image/png"
const val MIME_TYPE_HEIC = "image/heic"

// https://ai.google.dev/gemini-api/docs/audio
const val MIME_TYPE_WAV = "audio/wav"
const val MIME_TYPE_MP3 = "audio/mp3"

// https://ai.google.dev/gemini-api/docs/vision#prompting-video
const val MIME_TYPE_MP4 = "video/mp4"
const val MIME_TYPE_MOV = "video/mov"
const val MIME_TYPE_MPEG = "video/mpeg"
const val MIME_TYPE_MPG = "video/mpg"

@Serializable
data class Blob(
    val mimeType: String,
    val data: String
) {
    companion object {
        /** Generate blob from image URL. */
        fun fromDataUrl(url: URI) = fromDataUrl(url.toASCIIString())

        /** Generate blob from image URL. */
        fun fromDataUrl(urlStr: String): Blob {
            if (urlStr.startsWith("data:")) {
                val mimeType = urlStr.substringBefore(";base64,").substringAfter("data:")
                val base64 = urlStr.substringAfter(";base64,")
                return Blob(mimeType, base64)
            } else {
                throw UnsupportedOperationException("Expected a data URL but was $urlStr")
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

@Serializable
enum class CodeLanguage {
    LANGUAGE_UNSPECIFIED,
    PYTHON
}

@Serializable
data class CodeExecutionResult(
    val outcome: CodeExecutionOutcome,
    val output: String? = null
)

@Serializable
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

@Serializable(with = TypeSerializer::class)
enum class Type {
    TYPE_UNSPECIFIED,
    STRING,
    NUMBER,
    INTEGER,
    BOOLEAN,
    ARRAY,
    OBJECT;

    companion object {
        fun fromString(value: String) =
            values().find { it.name.equals(value, ignoreCase = true) }
                ?: throw SerializationException("Unknown type: $value")
    }
}

object TypeSerializer : KSerializer<Type> {
    override val descriptor: SerialDescriptor =
        PrimitiveSerialDescriptor("Type", PrimitiveKind.STRING)

    override fun serialize(encoder: Encoder, value: Type) =
        encoder.encodeString(value.name.lowercase()) // Preserve lowercase formatting

    override fun deserialize(decoder: Decoder) =
        Type.fromString(decoder.decodeString())
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

@Serializable
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

@Serializable
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
    val responseSchema: Schema? = null,
    val candidateCount: Int? = null, // only 1 allowed for now
    val maxOutputTokens: Int? = null,
    val temperature: Double? = null,
    val topP: Double? = null,
    val topK: Int? = null,
    val presencePenalty: Double? = null,
    val frequencyPenalty: Double? = null,
    val responseLogprobs: Boolean? = null,
    val logprobs: Int? = null
) {
    init {
        require(responseMimeType in ALLOWED_MIMES) { "Unexpected responseMimeType: $responseMimeType" }
    }
}

//endregion

//region [GenerateContentResponse]

@Serializable
data class GenerateContentResponse(
    var candidates: List<Candidate>?,
    var promptFeedback: PromptFeedback? = null,
    var usageMetadata: UsageMetadata? = null
)

@Serializable
data class Candidate(
    val content: Content,
    val finishReason: FinishReason,
    val safetyRatings: List<SafetyRating>? = null,
    val citationMetadata: List<CitationMetadata>? = null,
    val tokenCount: Int? = null,
    val groundingAttributions: List<GroundingAttribution>? = null,
    val groundingMetadata: GroundingMetadata? = null,
    val avgLogprobs: Double? = null,
    val logprobsResult: LogprobsResult? = null,
    val index: Int? = null
)

@Serializable
enum class FinishReason {
    FINISH_REASON_UNSPECIFIED,
    STOP,
    MAX_TOKENS,
    SAFETY,
    RECITATION,
    LANGUAGE,
    OTHER,
    BLOCKLIST,
    PROHIBITED_CONTENT,
    SPII,
    MALFORMED_FUNCTION_CALL
}

@Serializable
data class CitationMetadata(
    val citationSources: List<CitationSource>
)

@Serializable
data class CitationSource(
    val startIndex: Int? = null,
    val endIndex: Int? = null,
    val uri: String? = null,
    val license: String? = null
)

@Serializable
data class GroundingAttribution(
    val sourceId: AttributionSourceId,
    val content: Content
)

@Serializable
data class AttributionSourceId(
    val groundingPassage: GroundingPassageId,
    val semanticRetrieverChunk: SemanticRetrieverChunk
)

@Serializable
data class GroundingPassageId(
    val passageID: String,
    val partIndex: Int
)

@Serializable
data class SemanticRetrieverChunk(
    val source: String,
    val chunk: String
)

@Serializable
data class GroundingMetadata(
    val groundingChunks: List<GroundingChunk>,
    val groundingSupports: List<GroundingSupport>,
    val webSearchQueries: List<String>,
    val searchEntryPoint: SearchEntryPoint? = null,
    val retrievalMetadata: RetrievalMetadata
)

@Serializable
data class GroundingChunk(
    val web: GroundingChunkWeb? = null
)

@Serializable
data class GroundingChunkWeb(
    val uri: String,
    val title: String
)

@Serializable
data class GroundingSupport(
    val groundingChunkIndices: List<Int>,
    val confidenceScores: List<Float>,
    val segment: Segment
)

@Serializable
data class Segment(
    val partIndex: Int,
    val startIndex: Int,
    val endIndex: Int,
    val text: String
)

@Serializable
data class RetrievalMetadata(
    val googleSearchDynamicRetrievalScore: Float? = null
)

@Serializable
data class SearchEntryPoint(
    val renderedContent: String? = null,
    val sdkBlob: String? = null
)

@Serializable
data class LogprobsResult(
    val topCandidates: List<TopCandidate>,
    val chosenCandidates: List<LogprobsCandidate>
)

@Serializable
data class TopCandidate(
    val candidates: List<LogprobsCandidate>
)

@Serializable
data class LogprobsCandidate(
    val token: String,
    val tokenId: Int,
    val logProbability: Double
)

@Serializable
data class PromptFeedback(
    val blockReason: BlockReason? = null,
    val safetyRatings: List<SafetyRating>? = null
)

@Serializable
enum class BlockReason {
    BLOCK_REASON_UNSPECIFIED,
    SAFTEY,
    OTHER,
    BLOCKLIST,
    PROHIBITED_CONTENT
}

@Serializable
data class SafetyRating(
    val category: HarmCategory,
    val probability: HarmProbability,
    val blocked: Boolean? = null
)

@Serializable
enum class HarmProbability {
    HARM_PROBABILITY_UNSPECIFIED,
    NEGLIGIBLE,
    LOW,
    MEDIUM,
    HIGH
}

@Serializable
data class Error(
    val message: String
)

@Serializable
data class UsageMetadata(
    val promptTokenCount: Int,
    val cachedContentTokenCount: Int? = null,
    val candidatesTokenCount: Int,
    val totalTokenCount: Int
)

//endregion
