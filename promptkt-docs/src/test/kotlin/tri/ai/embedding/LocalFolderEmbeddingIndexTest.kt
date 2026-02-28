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
package tri.ai.embedding

import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import tri.ai.core.EmbeddingModel
import tri.ai.text.chunks.SmartTextChunker
import tri.ai.text.chunks.SmartTextChunkerTest
import tri.ai.text.chunks.TextChunkInDoc
import tri.ai.text.chunks.TextChunker
import kotlin.io.path.toPath

class LocalFolderEmbeddingIndexTest {

    @Test
    fun `test index`() = runTest {
        val docsPath = SmartTextChunkerTest::class.java.getResource("resources")!!.toURI().toPath().toFile()
        val index = LocalFolderEmbeddingIndex(docsPath, EmbeddingStrategy(MockEmbeddingModel(), MockEmbeddingModel()))
        index.calculateAndGetDocs().forEach {
            println(it.metadata.path)
            it.chunks.take(10).forEach { chunk ->
                val chunkDoc = chunk as TextChunkInDoc
                val text = chunk.text(it.all).replace("\\s+".toRegex(), " ")
                println("  ${chunkDoc.first} ${chunkDoc.last} ${text.take(200)}")
            }
        }
    }

}

class MockEmbeddingModel: EmbeddingModel, TextChunker {
    override val modelId = "mock"

    override fun chunkText(text: String, maxChunkSize: Int) =
        SmartTextChunker().chunkText(text, maxChunkSize)

    override suspend fun calculateEmbedding(text: List<String>, outputDimensionality: Int?): List<List<Double>> {
        return text.map { listOf(1.0) }
    }
}
