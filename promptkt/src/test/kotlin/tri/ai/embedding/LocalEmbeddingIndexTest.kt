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
import kotlin.io.path.toPath

class LocalEmbeddingIndexTest {

    @Test
    fun `test index`() = runTest {
        val docsPath = LocalEmbeddingIndexTest::class.java.getResource("resources")!!.toURI().toPath().toFile()
        val index = LocalEmbeddingIndex(docsPath, MockEmbeddingService())
        index.getEmbeddingIndex().forEach { (path, doc) ->
            println(path)
            doc.sections.take(10).forEach { section ->
                val text = index.readSnippet(doc, section).replace("\\s+".toRegex(), " ")
                println("  ${section.start} ${section.end} ${text.take(200)}")
            }
        }
    }

}

class MockEmbeddingService: EmbeddingService {
    override val modelId = "mock"

    override fun chunkTextBySections(text: String, maxChunkSize: Int): List<TextChunk> {
        return with (TextChunker(maxChunkSize)) { TextChunk(text).chunkBySections(combineShortSections = true) }
    }

    override suspend fun calculateEmbedding(text: List<String>): List<List<Double>> {
        return text.map { listOf(1.0) }
    }
}
