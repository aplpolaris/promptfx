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
package tri.ai.embedding

import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import tri.ai.text.chunks.TextChunk
import tri.ai.text.chunks.TextChunkInDoc
import tri.ai.text.chunks.TextChunkRaw
import tri.ai.text.chunks.process.SmartTextChunker
import kotlin.io.path.toPath

class LocalFolderEmbeddingIndexTest {

    @Test
    fun `test index`() = runTest {
        val docsPath = LocalFolderEmbeddingIndexTest::class.java.getResource("resources")!!.toURI().toPath().toFile()
        val index = LocalFolderEmbeddingIndex(docsPath, MockEmbeddingService())
        index.library.docs.forEach {
            println(it.metadata.path)
            it.chunks.take(10).forEach { chunk ->
                val chunkDoc = chunk as TextChunkInDoc
                val text = chunk.text(it.all).replace("\\s+".toRegex(), " ")
                println("  ${chunkDoc.first} ${chunkDoc.last} ${text.take(200)}")
            }
        }
    }

}

class MockEmbeddingService: EmbeddingService {
    override val modelId = "mock"

    override fun chunkTextBySections(text: String, maxChunkSize: Int): List<TextChunk> {
        return with (SmartTextChunker(maxChunkSize)) { TextChunkRaw(text).chunkBySections(combineShortSections = true) }
    }

    override suspend fun calculateEmbedding(text: List<String>): List<List<Double>> {
        return text.map { listOf(1.0) }
    }
}
