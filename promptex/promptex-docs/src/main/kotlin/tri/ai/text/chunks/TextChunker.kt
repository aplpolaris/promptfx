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
package tri.ai.text.chunks

//    override fun chunkTextBySections(text: String, maxChunkSize: Int) =
//        with (SmartTextChunker(maxChunkSize)) {
//            TextChunkRaw(text).chunkBySections(combineShortSections = true)
//        }
/**
 * An interface for chunking text and calculating embeddings.
 * Clients are responsible for API limitations, e.g. that limit the number of embeddings that can be calculated in a single API call.
 */
interface TextChunker {

    /**
     * Divide text at reasonable positions. Each chunk should have a length of at most [maxChunkSize].
     */
    fun chunkText(text: String, maxChunkSize: Int): List<TextChunk>

}

/** Text chunker that returns unalterated input. */
object NoOpTextChunker : TextChunker {
    override fun chunkText(text: String, maxChunkSize: Int) = listOf(TextChunkRaw(text))
}
