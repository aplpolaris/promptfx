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
package tri.ai.text.docs

import tri.ai.pips.AiPipelineResult

/** Provides access to necessary components for document Q&A. */
interface DocumentQaDriver {

    /** Set of folders, or document sets, to work with. */
    val folders: List<String>
    /** The currently selected folder, or document set. */
    var folder: String

    /** The chat model (by id). */
    var chatModel: String
    /** The text embedding model (by id). */
    var embeddingModel: String
    /** Temperature. */
    var temp: Double
    /** Maximum tokens. */
    var maxTokens: Int

    /** Initialize the driver. */
    fun initialize()
    /** Close the driver. */
    fun close()

    /** Answer a question using documents in the current folder. */
    suspend fun answerQuestion(input: String, numResponses: Int = 1, historySize: Int = 1): AiPipelineResult<String>

}

