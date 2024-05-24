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

/** Models available in the Gemini API. */
object GeminiModels {

    //region MODEL ID's

    const val EMBED1 = "embedding-001"
    const val EMBED4 = "text-embedding-004"

    const val GEMINI_PRO = "gemini-pro"
    const val GEMINI_1_PRO = "gemini-1.0-pro"
    const val GEMINI_1_PRO_001 = "gemini-1.0-pro-001"
    const val GEMINI_1_PRO_LATEST = "gemini-1.0-pro-latest"

    const val GEMINI_PRO_VISION = "gemini-pro-vision"
    const val GEMINI_1_PRO_VISION_LATEST = "gemini-1.0-pro-vision-latest"

    //endregion

    fun embeddingModels() = listOf(EMBED1, EMBED4)
    fun completionModels(includeSnapshots: Boolean = false) = listOf(GEMINI_PRO, GEMINI_1_PRO)
    fun chatModels(includeSnapshots: Boolean = false) = listOf(GEMINI_PRO, GEMINI_1_PRO)
    fun visionLanguageModels() = listOf<String>()
    fun imageGeneratorModels() = listOf<String>()

}
