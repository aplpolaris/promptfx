/*-
 * #%L
 * tri.promptfx:promptfx
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
package tri.promptfx

/**
 * Global manager for models available within PromptFx.
 * Model availability is determined by the current [PromptFxPolicy], further restricted by
 * include/exclude patterns from [PromptFxRuntimeConfig].
 */
object PromptFxModels {

    var policy: PromptFxPolicy = PromptFxPolicyUnrestricted

    /** Returns all available chat engines, including both text and multimodal. */
    fun chatEngines() = (chatModels().map { AiChatEngine.Text(it) } +
            multimodalModels().map { AiChatEngine.Multimodal(it) })
        .sortedBy { it.modelId }
    fun chatEngineDefault() = chatEngines().find { it.modelId == chatModelDefault().modelId }

    fun textCompletionModels() = policy.textCompletionModels().filter { PromptFxRuntimeConfig.isModelActive(it.modelId) }
    fun textCompletionModelDefault() = textCompletionModels().firstOrNull() ?: policy.textCompletionModelDefault()

    fun embeddingModels() = policy.embeddingModels().filter { PromptFxRuntimeConfig.isModelActive(it.modelId) }
    fun embeddingModelDefault() = embeddingModels().firstOrNull() ?: policy.embeddingModelDefault()

    fun chatModels() = policy.chatModels().filter { PromptFxRuntimeConfig.isModelActive(it.modelId) }
    fun chatModelDefault() = chatModels().firstOrNull() ?: policy.chatModelDefault()

    fun multimodalModels() = policy.multimodalModels().filter { PromptFxRuntimeConfig.isModelActive(it.modelId) }
    fun multimodalModelDefault() = multimodalModels().firstOrNull() ?: policy.multimodalModelDefault()

    fun imageModels() = policy.imageModels().filter { PromptFxRuntimeConfig.isModelActive(it.modelId) }
    fun imageModelDefault() = imageModels().firstOrNull() ?: policy.imageModelDefault()

    fun textToSpeechModels() = policy.textToSpeechModels()
    fun textToSpeechModelDefault() = policy.textToSpeechModelDefault()

    fun speechToTextModels() = policy.speechToTextModels()
    fun speechToTextModelDefault() = policy.speechToTextModelDefault()

    fun modelIds() = (
            textCompletionModels().map { it.modelId } +
            embeddingModels().map { it.modelId } +
            chatModels().map { it.modelId } +
            multimodalModels().map { it.modelId } +
            imageModels().map { it.modelId } +
            textToSpeechModels().map { it.modelId } +
            speechToTextModels().map { it.modelId }
        ).toSet()

    /** Returns all model IDs configured in the current policy, regardless of runtime config filters. */
    fun policyModelIds() = (
            policy.textCompletionModels().map { it.modelId } +
            policy.embeddingModels().map { it.modelId } +
            policy.chatModels().map { it.modelId } +
            policy.multimodalModels().map { it.modelId } +
            policy.imageModels().map { it.modelId } +
            policy.textToSpeechModels().map { it.modelId } +
            policy.speechToTextModels().map { it.modelId }
        ).toSet()

}
