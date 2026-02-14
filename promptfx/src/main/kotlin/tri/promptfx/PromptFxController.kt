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

import javafx.beans.property.SimpleIntegerProperty
import javafx.beans.property.SimpleObjectProperty
import tornadofx.Controller
import tri.ai.core.TextChat
import tri.ai.core.TextCompletion
import tri.ai.core.TextPlugin
import tri.ai.embedding.EmbeddingStrategy
import tri.ai.openai.OpenAiPlugin
import tri.ai.openai.UsageUnit
import tri.ai.pips.AiPipelineResult
import tri.ai.text.chunks.SmartTextChunker
import tri.promptfx.prompts.PromptTraceHistoryModel

/** Controller for [PromptFx]. */
class PromptFxController : Controller() {

    val openAiPlugin = TextPlugin.orderedPlugins.first { it is OpenAiPlugin } as OpenAiPlugin

    val completionEngine: SimpleObjectProperty<TextCompletion> =
        SimpleObjectProperty(PromptFxModels.textCompletionModelDefault())
    val chatService: SimpleObjectProperty<TextChat> =
        SimpleObjectProperty(PromptFxModels.chatModelDefault())
    val embeddingStrategy: SimpleObjectProperty<EmbeddingStrategy> =
        SimpleObjectProperty(EmbeddingStrategy(PromptFxModels.embeddingModelDefault(), SmartTextChunker()))

    val promptHistory = find<PromptTraceHistoryModel>()

    val tokensUsed = SimpleIntegerProperty(0)
    val audioUsed = SimpleIntegerProperty(0)
    val imagesUsed = SimpleIntegerProperty(0)

    //region UPDATERS

    /** Adds a pipeline execution result to history. */
    fun addPromptTraces(viewTitle: String, traces: AiPipelineResult) {
        val interim = (traces.interimResults.values - traces.finalResult).map {
            it.copy(execInfo = it.exec.copy(intermediateResult = true, viewId = viewTitle))
        }
        promptHistory.prompts.addAll(interim)
        val final = traces.finalResult.let { it.copy(execInfo = it.exec.copy(intermediateResult = false, viewId = viewTitle)) }
        promptHistory.prompts.add(final)
    }

    /** Update usage stats for the OpenAI endpoint. */
    fun updateUsage() {
        tokensUsed.value = openAiPlugin.client.usage[UsageUnit.TOKENS] ?: 0
        audioUsed.value = openAiPlugin.client.usage[UsageUnit.AUDIO_SECONDS] ?: 0
        imagesUsed.value = openAiPlugin.client.usage[UsageUnit.IMAGES] ?: 0
    }

    //endregion

    /** Called to release resources when the application is closed. */
    fun close() {
        try {
            openAiPlugin.client.client.close()
        } catch (x: Exception) {
            println("There was an error closing the OpenAI client: ${x.message}")
        }
        TextPlugin.orderedPlugins.forEach {
            try {
                it.close()
            } catch (x: Exception) {
                println("There was an error closing the plugin $it: ${x.message}")
            }
        }
    }

}
