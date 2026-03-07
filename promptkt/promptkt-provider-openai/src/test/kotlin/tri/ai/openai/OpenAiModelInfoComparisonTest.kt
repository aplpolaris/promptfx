/*-
 * #%L
 * tri.promptfx:promptkt
 * %%
 * Copyright (C) 2023 - 2026 Johns Hopkins University Applied Physics Laboratory
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
package tri.ai.openai

import com.aallam.openai.api.chat.ChatCompletionRequest
import com.aallam.openai.api.chat.ChatMessage
import com.aallam.openai.api.chat.ChatRole
import com.aallam.openai.api.completion.CompletionRequest
import com.aallam.openai.api.model.ModelId
import com.aallam.openai.api.response.ResponseInput
import com.aallam.openai.api.response.ResponseRequest
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import tri.ai.core.DataModality
import tri.ai.core.ModelInfo
import tri.ai.core.ModelLifecycle
import tri.ai.core.ModelType
import tri.util.ANSI_BLUE
import tri.util.ANSI_RESET
import kotlin.time.Duration.Companion.seconds

/**
 * Compares model capabilities detected via minimal API probing against the model index configuration,
 * recommending any type changes needed.
 *
 * The comparison is structured around two model "cards":
 *  - [DetectedModelCard]: populated by testing each model against the completions, chat, and responses APIs
 *  - [LocalIndexModelCard]: populated from the local model index ([OpenAiModelIndex])
 *
 * From the delta between these cards, [ModelRecommendation] objects are generated for any model whose
 * index type diverges from the type that best fits its actual API capabilities.
 */
class OpenAiModelInfoComparisonTest {

    private val client = OpenAiAdapter.INSTANCE.client

    companion object {
        /** Model types that only support specialized APIs and are excluded from recommendation checks. */
        private val UTILITY_MODEL_TYPES = setOf(
            ModelType.MODERATION,
            ModelType.TEXT_EMBEDDING,
            ModelType.TEXT_TO_SPEECH,
            ModelType.SPEECH_TO_TEXT,
            ModelType.IMAGE_GENERATOR
        )

        /** Regex matching dated snapshot model IDs, e.g. `gpt4o-1120` or `gpt4-06-13` or `gpt5-preview` or `gpt5-latest`. */
        private val SNAPSHOT_MODEL_REGEX = Regex(".*-(\\d{4}|\\d{2}-\\d{2}|preview)$")
    }

    //region DATA CLASSES

    data class DetectedModelCard(
        val id: String,
        val supportsCompletions: Boolean,
        val supportsChat: Boolean,
        val supportsResponses: Boolean
    ) {
        override fun toString() =
            "$id [completions=$supportsCompletions, chat=$supportsChat, responses=$supportsResponses]"
    }

    data class LocalIndexModelCard(
        val id: String,
        val type: ModelType,
        val inputs: List<DataModality>?,
        val outputs: List<DataModality>?,
        val lifecycle: ModelLifecycle
    )

    data class ModelRecommendation(
        val id: String,
        val indexCard: LocalIndexModelCard,
        val detectedCard: DetectedModelCard,
        val recommendedType: ModelType?
    )

    //endregion

    //region TESTS

    @Test
    @Tag("openai")
    fun `test model card comparison and recommendations`() {
        runTest(timeout = 300.seconds) {
            val indexCards = getIndexCards()

            val recommendations = mutableListOf<ModelRecommendation>()
            for (indexCard in indexCards) {
                val detected = detectModelCard(indexCard.id)
                val recommended = recommendType(detected, indexCard)
                recommendations += ModelRecommendation(indexCard.id, indexCard, detected, recommended)
            }

            printReport(indexCards.size, recommendations)
        }
    }

    @Test
    @Tag("openai")
    fun `test identify new models not in index`() {
        runTest(timeout = 300.seconds) {
            val res = client.models()
            val apiModelIds = res.map { it.id.id }.toSet()
            val indexIds = OpenAiModelIndex.modelInfoIndex.values.map { it.id }.toSet()

            // Models from the API not in the local index, excluding snapshot/dated variants
            val newModelIds = (apiModelIds - indexIds)
                .filter { !it.isSnapshotModel() }
                .sorted()

            println("=".repeat(80))
            println("New Models Not in Local Index — ${newModelIds.size} found")
            println("=".repeat(80))

            if (newModelIds.isEmpty()) {
                println("No new models found.")
            } else {
                newModelIds.forEach { modelId ->
                    val detected = detectModelCard(modelId)
                    val recommended = recommendTypeFromDetected(detected)
                    println()
                    println("Model: $modelId")
                    println("  Detected:         $detected")
                    println("  Suggested type:   ${recommended ?: "(unknown — no API responded)"}")
                }
                println()
                println("Recommendation: add these ${newModelIds.size} model(s) to openai-models.yaml.")
            }
            println("=".repeat(80))
        }
    }

    //endregion

    //region CARD POPULATION

    /** Returns index cards for all non-deprecated, non-utility models, sorted by id. */
    private fun getIndexCards(): List<LocalIndexModelCard> =
        OpenAiModelIndex.modelInfoIndex.values
            .filter { it.lifecycle !in setOf(ModelLifecycle.DEPRECATED, ModelLifecycle.DISCONTINUED) }
            .filter { it.type !in UTILITY_MODEL_TYPES }
            .sortedBy { it.id }
            .map { it.toIndexCard() }

    /** Converts a [ModelInfo] to an [LocalIndexModelCard]. */
    private fun ModelInfo.toIndexCard() =
        LocalIndexModelCard(id = id, type = type, inputs = inputs, outputs = outputs, lifecycle = lifecycle)

    /**
     * Probes the model against the three OpenAI APIs and returns a [DetectedModelCard].
     * Each probe uses the minimum possible request (max 5 output tokens) to minimise cost.
     */
    private suspend fun detectModelCard(modelId: String): DetectedModelCard {
        val supportsCompletions = runCatching {
            client.completion(CompletionRequest(
                model = ModelId(modelId),
                prompt = "1+1=",
                maxTokens = 5
            ))
        }.isSuccess

        val supportsChat = runCatching {
            client.chatCompletion(ChatCompletionRequest(
                model = ModelId(modelId),
                messages = listOf(ChatMessage(ChatRole.User, "1+1=")),
                maxTokens = 5
            ))
        }.isSuccess

        val supportsResponses = runCatching {
            client.response(ResponseRequest(
                model = ModelId(modelId),
                input = ResponseInput("1+1="),
                maxOutputTokens = 16
            ))
        }.isSuccess

        return DetectedModelCard(
            id = modelId,
            supportsCompletions = supportsCompletions,
            supportsChat = supportsChat,
            supportsResponses = supportsResponses
        )
    }

    //endregion

    //region RECOMMENDATION LOGIC

    /**
     * Computes the recommended [ModelType] for a model based on its detected capabilities and index configuration.
     *
     * Rules (applied in priority order):
     * - Completions only → [ModelType.TEXT_COMPLETION]
     * - Chat or Responses, but text-only I/O → [ModelType.TEXT_CHAT]
     * - Chat but not Responses (implies non-text I/O) → [ModelType.TEXT_VISION_CHAT]
     * - Responses supported → [ModelType.RESPONSES]
     *
     * Returns null if no determination can be made (e.g., no API responded).
     */
    private fun recommendType(detected: DetectedModelCard, indexCard: LocalIndexModelCard): ModelType? {
        val hasNonTextInput = indexCard.inputs?.any { it != DataModality.text } == true
        return when {
            detected.supportsCompletions && !detected.supportsChat && !detected.supportsResponses ->
                ModelType.TEXT_COMPLETION
            (detected.supportsChat || detected.supportsResponses) && !hasNonTextInput ->
                ModelType.TEXT_CHAT
            detected.supportsChat && !detected.supportsResponses ->
                ModelType.TEXT_VISION_CHAT
            detected.supportsResponses ->
                ModelType.RESPONSES
            else -> null
        }
    }

    /**
     * Computes the recommended [ModelType] for a newly discovered model based solely on detected API capabilities.
     * Since there is no index entry, text-only I/O is assumed (conservative default).
     * Precedence: completions-only → TEXT_COMPLETION; responses → RESPONSES; chat → TEXT_CHAT.
     *
     * Returns null if no determination can be made (e.g., no API responded).
     */
    private fun recommendTypeFromDetected(detected: DetectedModelCard): ModelType? = when {
        detected.supportsCompletions && !detected.supportsChat && !detected.supportsResponses ->
            ModelType.TEXT_COMPLETION
        detected.supportsResponses ->
            ModelType.RESPONSES
        detected.supportsChat ->
            ModelType.TEXT_CHAT
        else -> null
    }

    /**
     * Returns true if this model id looks like a dated snapshot (e.g. `gpt-4o-2024-11-20`
     * or `gpt-4-0613`), i.e. ends with a suffix matching `-YYYY`, `-YYYY-MM`, or `-YYYY-MM-DD`.
     */
    private fun String.isSnapshotModel() = matches(SNAPSHOT_MODEL_REGEX)

    //endregion

    //region REPORTING

    private fun printReport(modelCount: Int, recommendations: List<ModelRecommendation>) {
        println("=".repeat(80))
        println("Model Card Comparison — $modelCount models probed")
        println("=".repeat(80))

        val typeNull = recommendations.filter { it.recommendedType == null }
        val typeMisMatch = (recommendations - typeNull).filter { it.recommendedType != it.indexCard.type }
        val rest = recommendations - typeNull - typeMisMatch

        if (typeNull.isEmpty() && typeMisMatch.isEmpty()) {
            println("=".repeat(80))
            println("All model types are consistent with detected API capabilities.")
        } else {
            println("=".repeat(80))
            println("The following models match API tests:")
            rest.forEach { rec -> printModel(rec) }
            println("=".repeat(80))
            println("The following models had no API responses, so no recommendation can be made:")
            typeNull.forEach { rec -> printModel(rec) }
            println("=".repeat(80))
            println("The following models had API responses that suggest a different type than the index:")
            typeMisMatch.forEach { rec -> printModel(rec) }
        }
        println("=".repeat(80))
    }

    private fun printModel(rec: ModelRecommendation) {
        println()
        println("${ANSI_BLUE}Model: ${rec.id}${ANSI_RESET}")
        println("  Index type: ${rec.indexCard.type}")
        println("    Inputs:  ${rec.indexCard.inputs ?: "(unspecified)"}")
        println("    Outputs: ${rec.indexCard.outputs ?: "(unspecified)"}")
        println("  Detected: ${rec.detectedCard}")
        println("    Recommended type: ${rec.recommendedType}")
    }

    //endregion
}
