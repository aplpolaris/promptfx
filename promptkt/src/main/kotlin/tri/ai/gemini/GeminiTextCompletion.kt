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
package tri.ai.gemini

import tri.ai.core.TextCompletion
import tri.ai.gemini.GeminiModels.GEMINI_PRO
import tri.ai.pips.AiTaskResult

/** Text completion with OpenAI models. */
class GeminiTextCompletion(override val modelId: String = GEMINI_PRO, val client: GeminiClient = GeminiClient.INSTANCE) :
    TextCompletion {

    override fun toString() = modelId

    override suspend fun complete(text: String, tokens: Int?, temperature: Double?, stop: String?): AiTaskResult<String> =
        client.generateContent(text, modelId).candidates!!.first().let {
            AiTaskResult.result(it.content.parts[0].text!!)
        }

}

