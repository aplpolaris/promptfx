/*-
 * #%L
 * tri.promptfx:promptkt
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
package tri.ai.openai

import tri.ai.core.TextPlugin

/**
 * OpenAI implementation of [TextPlugin].
 * Models are as described in `openai-models.yaml`.
 */
class OpenAiTextPlugin : TextPlugin {

    val client = OpenAiClient.INSTANCE

    override fun chatModels() =
        OpenAiModels.chatModels(false).map { OpenAiChat(it, client) }

    override fun textCompletionModels() =
        OpenAiModels.chatModels(false).map { OpenAiCompletionChat(it, client) } +
        OpenAiModels.completionModels(false).map { OpenAiCompletion(it, client) }

    override fun embeddingModels() =
        OpenAiModels.embeddingModels().map { OpenAiEmbeddingService(it, client) }

    override fun close() {
        client.client.close()
    }
}
