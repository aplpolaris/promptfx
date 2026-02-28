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

/**
 * Common interface for all AI model implementations, providing a model identifier and source.
 * Use [modelDisplayName] to get a human-readable display string in the format "$modelId [$modelSource]".
 */
interface AiModel {
    /** Identifier for the model. */
    val modelId: String
    /** Source or provider of the model (e.g. "OpenAI", "Gemini"). */
    val modelSource: String

    /** Returns a display string in the format "$modelId [$modelSource]", or just "$modelId" if source is empty. */
    fun modelDisplayName() = if (modelSource.isEmpty()) modelId else "$modelId [$modelSource]"
}
