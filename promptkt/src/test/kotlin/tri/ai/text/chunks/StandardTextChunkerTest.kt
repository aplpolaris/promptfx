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
package tri.ai.text.chunks

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import tri.ai.embedding.TextChunkerTest

class StandardTextChunkerTest {

    @Test
    fun testChunkWhile() {
        val list = listOf(1, 2, 3, 4, 5, 6, 7)
        val result = list.chunkWhile { it.sum() <= 6 }
        assertEquals(listOf(listOf(1, 2, 3), listOf(4), listOf(5), listOf(6), listOf(7)), result)
    }

    @Test
    fun testTextChunking() {
        val chunker = StandardTextChunker(5000)
        val fullText = TextChunkerTest::class.java.getResource("resources/pg1513.txt")!!.readText()

        println("--- Simple ---")
        val chunks1 = chunker.chunkTextBySectionsSimple(fullText)
        chunks1.take(10).forEach { section ->
            val text = fullText.substring(section.first).replace("\\s+".toRegex(), " ")
            println("  ${section.first} ${text.take(200)}")
        }

        println("--- Section Chunking ---")
        val chunks2 = with(chunker) {
            TextDocumentImpl("id", fullText).all.chunkBySections(combineShortSections = true)
        }
        chunks2.take(10).forEach { section ->
            val text = fullText.substring(section.range).replace("\\s+".toRegex(), " ").trim()
            println("  ${section.range} ${text.take(200)}")
        }
    }

}
