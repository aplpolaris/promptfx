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
package tri.ai.core

import tri.ai.prompt.trace.AiExecInfo
import tri.ai.prompt.trace.AiModelInfo
import tri.ai.prompt.trace.AiPromptTrace
import tri.ai.prompt.trace.AiOutputInfo

/**
 * Minimal test implementation of [TextPlugin] for testing purposes only.
 * This shim plugin provides minimal implementations to avoid circular dependencies.
 */
class TextPluginTest : TextPlugin {
    override fun isApiConfigured() = true
    override fun modelSource() = "TestShim"
    override fun modelInfo() = listOf(ModelInfo("test-model", ModelType.TEXT_COMPLETION, "TestShim"))
    override fun embeddingModels() = emptyList<EmbeddingModel>()
    override fun textCompletionModels(): List<TextCompletion> = listOf(TestTextCompletion())
    override fun chatModels(): List<TextChat> = listOf(TestTextChat())
    override fun multimodalModels() = emptyList<MultimodalChat>()
    @Deprecated("Use multimodalModels() instead")
    override fun visionLanguageModels() = emptyList<VisionLanguageChat>()
    override fun imageGeneratorModels() = emptyList<ImageGenerator>()
    override fun close() {}
}

/** Minimal test text completion model. */
internal class TestTextCompletion : TextCompletion {
    override val modelId = "test-model"
    override val modelSource = "TestShim"
    override fun toString() = modelDisplayName()
    override suspend fun complete(text: String, variation: MChatVariation, tokens: Int?, stop: List<String>?, numResponses: Int?) =
        AiPromptTrace(null, AiModelInfo(modelId), AiExecInfo(), AiOutputInfo.text("test response"))
}

/** Minimal test text chat model. */
internal class TestTextChat : TextChat {
    override val modelId = "test-model"
    override val modelSource = "TestShim"
    override fun toString() = modelDisplayName()
    override suspend fun chat(messages: List<TextChatMessage>, variation: MChatVariation, tokens: Int?, stop: List<String>?, numResponses: Int?, requestJson: Boolean?) =
        AiPromptTrace(
            null,
            AiModelInfo(modelId),
            AiExecInfo(),
            AiOutputInfo.messages(listOf(TextChatMessage(MChatRole.Assistant, "test response")))
        )
}
