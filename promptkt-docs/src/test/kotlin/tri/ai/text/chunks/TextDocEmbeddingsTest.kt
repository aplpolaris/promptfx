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
package tri.ai.text.chunks

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import tri.ai.core.EmbeddingPrecision
import tri.ai.text.chunks.TextDocEmbeddings.chunkCount
import tri.ai.text.chunks.TextDocEmbeddings.embeddingModels
import tri.ai.text.chunks.TextDocEmbeddings.putEmbeddingInfo
import tri.ai.text.chunks.TextDocEmbeddings.summaryInfo

class TextDocEmbeddingsTest {

    @Test
    fun `test chunkCount returns zero for empty library`() {
        val library = TextLibrary("test")
        assertEquals(0, library.chunkCount())
    }

    @Test
    fun `test chunkCount returns correct count for library with chunks`() {
        val library = TextLibrary("test").apply {
            docs.add(TextDoc("doc1").apply {
                chunks.add(TextChunkRaw("chunk1"))
                chunks.add(TextChunkRaw("chunk2"))
            })
            docs.add(TextDoc("doc2").apply {
                chunks.add(TextChunkRaw("chunk3"))
            })
        }
        assertEquals(3, library.chunkCount())
    }

    @Test
    fun `test embeddingModels returns empty set for library without embeddings`() {
        val library = TextLibrary("test").apply {
            docs.add(TextDoc("doc1").apply {
                chunks.add(TextChunkRaw("chunk1"))
            })
        }
        assertTrue(library.embeddingModels().isEmpty())
    }

    @Test
    fun `test embeddingModels returns model ids for library with embeddings`() {
        val library = TextLibrary("test").apply {
            docs.add(TextDoc("doc1").apply {
                val chunk = TextChunkRaw("chunk1")
                chunk.putEmbeddingInfo("model-1", listOf(0.1, 0.2, 0.3), EmbeddingPrecision.FULL)
                chunks.add(chunk)
            })
        }
        val models = library.embeddingModels()
        assertEquals(1, models.size)
        assertTrue(models.contains("model-1"))
    }

    @Test
    fun `test embeddingModels returns all unique model ids`() {
        val library = TextLibrary("test").apply {
            docs.add(TextDoc("doc1").apply {
                val chunk1 = TextChunkRaw("chunk1")
                chunk1.putEmbeddingInfo("model-1", listOf(0.1, 0.2), EmbeddingPrecision.FULL)
                chunk1.putEmbeddingInfo("model-2", listOf(0.3, 0.4), EmbeddingPrecision.FULL)
                chunks.add(chunk1)
            })
            docs.add(TextDoc("doc2").apply {
                val chunk2 = TextChunkRaw("chunk2")
                chunk2.putEmbeddingInfo("model-1", listOf(0.5, 0.6), EmbeddingPrecision.FULL)
                chunks.add(chunk2)
            })
        }
        val models = library.embeddingModels()
        assertEquals(2, models.size)
        assertTrue(models.contains("model-1"))
        assertTrue(models.contains("model-2"))
    }

    @Test
    fun `test summaryInfo for empty library`() {
        val library = TextLibrary("test")
        val summary = library.summaryInfo()
        assertEquals("0 document(s), 0 chunk(s)", summary)
    }

    @Test
    fun `test summaryInfo for library without embeddings`() {
        val library = TextLibrary("test").apply {
            docs.add(TextDoc("doc1").apply {
                chunks.add(TextChunkRaw("chunk1"))
                chunks.add(TextChunkRaw("chunk2"))
            })
        }
        val summary = library.summaryInfo()
        assertEquals("1 document(s), 2 chunk(s)", summary)
    }

    @Test
    fun `test summaryInfo for library with embeddings`() {
        val library = TextLibrary("test").apply {
            docs.add(TextDoc("doc1").apply {
                val chunk1 = TextChunkRaw("chunk1")
                chunk1.putEmbeddingInfo("text-embedding-ada-002", listOf(0.1, 0.2), EmbeddingPrecision.FULL)
                chunks.add(chunk1)
            })
        }
        val summary = library.summaryInfo()
        assertEquals("1 document(s), 1 chunk(s), embeddings: text-embedding-ada-002", summary)
    }

    @Test
    fun `test summaryInfo for library with multiple embedding models`() {
        val library = TextLibrary("test").apply {
            docs.add(TextDoc("doc1").apply {
                val chunk1 = TextChunkRaw("chunk1")
                chunk1.putEmbeddingInfo("model-a", listOf(0.1, 0.2), EmbeddingPrecision.FULL)
                chunk1.putEmbeddingInfo("model-b", listOf(0.3, 0.4), EmbeddingPrecision.FULL)
                chunks.add(chunk1)
            })
        }
        val summary = library.summaryInfo()
        assertTrue(summary.contains("1 document(s), 1 chunk(s)"))
        assertTrue(summary.contains("embeddings:"))
        assertTrue(summary.contains("model-a"))
        assertTrue(summary.contains("model-b"))
    }
}
