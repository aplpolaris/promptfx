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

/**
 * Compares model capabilities detected via minimal API probing against the model index configuration,
 * recommending any type changes needed.
 *
 * The comparison is structured around two model "cards":
 *  - [DetectedModelCard]: populated by testing each model against the completions, chat, and responses APIs
 *  - [IndexModelCard]: populated from the local model index ([OpenAiModelIndex])
 *
 * From the delta between these cards, [ModelRecommendation] objects are generated for any model whose
 * index type diverges from the type that best fits its actual API capabilities.
 */
class ModelInfoComparisonTest {

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
    }

    //region DATA CLASSES

    /**
     * Model card populated by probing the model against each OpenAI API.
     *
     * @param id              Model identifier
     * @param supportsCompletions whether the model responds to the `/v1/completions` API
     * @param supportsChat    whether the model responds to the `/v1/chat/completions` API
     * @param supportsResponses whether the model responds to the `/v1/responses` API
     */
    data class DetectedModelCard(
        val id: String,
        val supportsCompletions: Boolean,
        val supportsChat: Boolean,
        val supportsResponses: Boolean
    ) {
        override fun toString() =
            "$id [completions=$supportsCompletions, chat=$supportsChat, responses=$supportsResponses]"
    }

    /**
     * Model card populated from the local model index.
     *
     * @param id       Model identifier
     * @param type     Currently configured [ModelType]
     * @param inputs   Configured input modalities (or null if unspecified)
     * @param outputs  Configured output modalities (or null if unspecified)
     * @param lifecycle Model lifecycle stage
     */
    data class IndexModelCard(
        val id: String,
        val type: ModelType,
        val inputs: List<DataModality>?,
        val outputs: List<DataModality>?,
        val lifecycle: ModelLifecycle
    )

    /**
     * Recommendation produced when a model's detected capabilities suggest a different [ModelType]
     * than what is currently configured in the index.
     *
     * @param id              Model identifier
     * @param indexCard       The index-configured card
     * @param detectedCard    The API-detected card
     * @param recommendedType The type recommended based on detected capabilities
     */
    data class ModelRecommendation(
        val id: String,
        val indexCard: IndexModelCard,
        val detectedCard: DetectedModelCard,
        val recommendedType: ModelType
    )

    //endregion

    //region TESTS

    @Test
    @Tag("openai")
    fun `test model card comparison and recommendations`() {
        runTest {
            val indexCards = getIndexCards()

            val recommendations = mutableListOf<ModelRecommendation>()
            for (indexCard in indexCards) {
                val detected = detectModelCard(indexCard.id)
                val recommended = recommendType(detected, indexCard)
                if (recommended != null && recommended != indexCard.type) {
                    recommendations += ModelRecommendation(indexCard.id, indexCard, detected, recommended)
                }
            }

            printReport(indexCards.size, recommendations)
        }
    }

    //endregion

    //region CARD POPULATION

    /** Returns index cards for all non-deprecated, non-utility models, sorted by id. */
    private fun getIndexCards(): List<IndexModelCard> =
        OpenAiModelIndex.modelInfoIndex.values
            .filter { it.lifecycle !in setOf(ModelLifecycle.DEPRECATED, ModelLifecycle.DISCONTINUED) }
            .filter { it.type !in UTILITY_MODEL_TYPES }
            .sortedBy { it.id }
            .map { it.toIndexCard() }

    /** Converts a [ModelInfo] to an [IndexModelCard]. */
    private fun ModelInfo.toIndexCard() =
        IndexModelCard(id = id, type = type, inputs = inputs, outputs = outputs, lifecycle = lifecycle)

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
                maxOutputTokens = 5
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
    private fun recommendType(detected: DetectedModelCard, indexCard: IndexModelCard): ModelType? {
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

    //endregion

    //region REPORTING

    private fun printReport(modelCount: Int, recommendations: List<ModelRecommendation>) {
        println("=".repeat(80))
        println("Model Card Comparison — ${modelCount} models probed")
        println("=".repeat(80))

        if (recommendations.isEmpty()) {
            println("All model types are consistent with detected API capabilities.")
        } else {
            recommendations.forEach { rec ->
                println()
                println("Model:  ${rec.id}")
                println("  Index type:       ${rec.indexCard.type}")
                println("  Recommended type: ${rec.recommendedType}")
                println("  Detected:         ${rec.detectedCard}")
                println("  Inputs:  ${rec.indexCard.inputs ?: "(unspecified)"}")
                println("  Outputs: ${rec.indexCard.outputs ?: "(unspecified)"}")
            }
            println()
            println("${recommendations.size} model(s) may need a type update.")
        }
        println("=".repeat(80))
    }

    //endregion
}
