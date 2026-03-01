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

import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import tri.ai.text.chunks.SmartTextChunker.Companion.chunkWhile

class SmartTextChunkerTest {

    @Test
    fun testChunkWhile() {
        val list = listOf(1, 2, 3, 4, 5, 6, 7)
        val result = list.chunkWhile { it.sum() <= 6 }
        Assertions.assertEquals(listOf(listOf(1, 2, 3), listOf(4), listOf(5), listOf(6), listOf(7)), result)
    }

    @Test
    fun testTextChunking() {
        val chunker = SmartTextChunker()
        val fullText = SmartTextChunkerTest::class.java.getResource("resources/pg1513.txt")!!.readText()

        println("--- Section Chunking ---")
        val chunks2 = with(chunker) {
            TextDoc("id", fullText).all!!.chunkBySections(maxChunkSize = 5000, combineShortSections = true)
        }
        chunks2.take(10).forEach { section ->
            val sb = section as TextChunkInDoc
            val text = fullText.substring(sb.range).replace("\\s+".toRegex(), " ").trim()
            println("  ${sb.range} ${text.take(200)}")
        }
    }

}
