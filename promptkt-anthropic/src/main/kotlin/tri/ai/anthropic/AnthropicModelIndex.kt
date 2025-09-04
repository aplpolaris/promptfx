/*-
 * #%L
 * tri.promptfx:promptkt
 * %%
 * Copyright (C) 2023 - 2025 Johns Hopkins University Applied Physics Laboratory
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
package tri.ai.anthropic

/** Known Claude model ids and related utility methods. */
object AnthropicModelIndex {

    //region MODEL LISTS

    /** All known Claude models. */
    fun allModels() = listOf(
        "claude-3-5-sonnet-20241022",
        "claude-3-5-sonnet-20240620",
        "claude-3-5-haiku-20241022",
        "claude-3-opus-20240229",
        "claude-3-sonnet-20240229",
        "claude-3-haiku-20240307",
        "claude-2.1",
        "claude-2.0",
        "claude-instant-1.2"
    )

    /** Models that support text chat. */
    fun chatModels() = allModels()

    /** Models that support text completion. */
    fun completionModels() = chatModels()

    /** Models that support vision/multimodal input. */
    fun visionLanguageModels() = listOf(
        "claude-3-5-sonnet-20241022",
        "claude-3-5-sonnet-20240620",
        "claude-3-5-haiku-20241022",
        "claude-3-opus-20240229",
        "claude-3-sonnet-20240229",
        "claude-3-haiku-20240307"
    )

    /** Models that support multimodal chat (text + images). */
    fun multimodalModels() = visionLanguageModels()

    /** Models that support embedding generation. Currently none. */
    fun embeddingModels() = emptyList<String>()

    /** Models that support image generation. Currently none. */
    fun imageGeneratorModels() = emptyList<String>()

    //endregion

}