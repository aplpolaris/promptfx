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

/** Index of Anthropic models and their capabilities. */
object AnthropicModelIndex {

    // Claude 3.5 models
    const val CLAUDE_3_5_SONNET_20241022 = "claude-3-5-sonnet-20241022"
    const val CLAUDE_3_5_SONNET_20240620 = "claude-3-5-sonnet-20240620"
    const val CLAUDE_3_5_HAIKU_20241022 = "claude-3-5-haiku-20241022"

    // Claude 3 models
    const val CLAUDE_3_OPUS_20240229 = "claude-3-opus-20240229"
    const val CLAUDE_3_SONNET_20240229 = "claude-3-sonnet-20240229"
    const val CLAUDE_3_HAIKU_20240307 = "claude-3-haiku-20240307"

    /** Returns a list of all chat models. */
    fun chatModels() = listOf(
        CLAUDE_3_5_SONNET_20241022,
        CLAUDE_3_5_SONNET_20240620,
        CLAUDE_3_5_HAIKU_20241022,
        CLAUDE_3_OPUS_20240229,
        CLAUDE_3_SONNET_20240229,
        CLAUDE_3_HAIKU_20240307
    )

    /** Returns a list of all chat models (same as chatModels for Anthropic). */
    fun chatModelsInclusive() = chatModels()

    /** Returns a list of text completion models. */
    fun completionModels() = chatModels()

    /** Returns a list of multimodal models (Claude 3+ supports images). */
    fun multimodalModels() = listOf(
        CLAUDE_3_5_SONNET_20241022,
        CLAUDE_3_5_SONNET_20240620,
        CLAUDE_3_5_HAIKU_20241022,
        CLAUDE_3_OPUS_20240229,
        CLAUDE_3_SONNET_20240229,
        CLAUDE_3_HAIKU_20240307
    )

    /** Returns a list of vision-language models (Claude 3+ supports vision). */
    fun visionLanguageModels() = multimodalModels()

    /** Returns a list of embedding models (Anthropic doesn't provide embeddings directly). */
    fun embeddingModels() = emptyList<String>()

    /** Returns a list of image generator models (Anthropic doesn't generate images). */
    fun imageGeneratorModels() = emptyList<String>()

}
