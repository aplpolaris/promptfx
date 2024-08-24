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
import tri.ai.gemini.GeminiModelIndex.GEMINI_PRO
import tri.ai.pips.AiTaskResult
import java.time.Duration

/** Text completion with Gemini models. */
class GeminiTextCompletion(override val modelId: String = GEMINI_PRO, val client: GeminiClient = GeminiClient.INSTANCE) :
    TextCompletion {

    override fun toString() = "$modelId (Gemini)"

    override suspend fun complete(text: String, tokens: Int?, temperature: Double?, stop: String?, numResponses: Int?): AiTaskResult<String> {
        val t0 = System.currentTimeMillis()
        val resp = client.generateContent(text, modelId, numResponses)
        val millis = Duration.ofMillis(System.currentTimeMillis() - t0)
        return AiTaskResult(
            values = resp.candidates!!.flatMap {
                it.content.parts.map { it.text!! }
            },
            modelId = modelId,
            duration = millis,
            durationTotal = millis
        )
    }

}

