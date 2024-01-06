/*-
 * #%L
 * promptfx-0.1.0-SNAPSHOT
 * %%
 * Copyright (C) 2023 - 2024 Johns Hopkins University Applied Physics Laboratory
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
import tri.ai.embedding.EmbeddingService
import tri.ai.core.TextCompletion
import tri.ai.core.TextPlugin
import tri.ai.pips.UsageUnit

/** Controller for [PromptFx]. */
class PromptFxController : Controller() {

    val openAiPlugin = TextPlugin.defaultPlugin

    val completionEngine: SimpleObjectProperty<TextCompletion> =
        SimpleObjectProperty(TextPlugin.textCompletionModels().first())
    val chatService: SimpleObjectProperty<TextChat> =
        SimpleObjectProperty(TextPlugin.chatModels().first())
    val embeddingService: SimpleObjectProperty<EmbeddingService> =
        SimpleObjectProperty(TextPlugin.embeddingModels().first())

    val tokensUsed = SimpleIntegerProperty(0)
    val audioUsed = SimpleIntegerProperty(0)
    val imagesUsed = SimpleIntegerProperty(0)

    /** Update usage stats for the OpenAI endpoint. */
    fun updateUsage() {
        tokensUsed.value = openAiPlugin.client.usage[UsageUnit.TOKENS] ?: 0
        audioUsed.value = openAiPlugin.client.usage[UsageUnit.AUDIO_MINUTES] ?: 0
        imagesUsed.value = openAiPlugin.client.usage[UsageUnit.IMAGES] ?: 0
    }

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
