/*-
 * #%L
 * promptkt-0.1.0-SNAPSHOT
 * %%
 * Copyright (C) 2023 Johns Hopkins University Applied Physics Laboratory
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

/** OpenAI implementation of [TextPlugin]. */
class OpenAiTextPlugin : TextPlugin {

    val client = OpenAiClient.INSTANCE

    override fun chatModels() = listOf(
        OpenAiChat(COMBO_GPT35, client),
        OpenAiChat(COMBO_GPT4, client),
        OpenAiChat(COMBO_GPT35_16K, client)
    )

    override fun textCompletionModels() = listOf(
        OpenAiCompletionChat(COMBO_GPT35, client),
        OpenAiCompletionChat(COMBO_GPT35_16K, client),
        OpenAiCompletionChat(COMBO_GPT4, client),
        OpenAiCompletion(TEXT_ADA, client),
        OpenAiCompletion(TEXT_BABBAGE, client),
        OpenAiCompletion(TEXT_CURIE, client),
        OpenAiCompletion(TEXT_DAVINCI3, client)
    )

    override fun embeddingModels() = listOf(
        OpenAiEmbeddingService(client)
    )

    override fun close() {
        client.client.close()
    }
}
