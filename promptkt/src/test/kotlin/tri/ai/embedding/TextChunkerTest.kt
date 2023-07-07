package tri.ai.embedding

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import tri.ai.embedding.TextChunker.chunkBySections
import java.io.File

class TextChunkerTest {

    @Test
    fun testChunkWhile() {
        val list = listOf(1, 2, 3, 4, 5, 6, 7)
        val result = list.chunkWhile { it.sum() <= 6 }
        assertEquals(listOf(listOf(1, 2, 3), listOf(4), listOf(5), listOf(6), listOf(7)), result)
    }

    @Test
    fun testTextChunking() {
        val fullText = TextChunkerTest::class.java.getResource("resources/pg1513.txt")!!.readText()

        println("--- Simple ---")
        val chunks1 = TextChunker.chunkTextBySectionsSimple(fullText, 5000)
        chunks1.take(10).forEach { section ->
            val text = fullText.substring(section.first).replace("\\s+".toRegex(), " ")
            println("  ${section.first} ${text.take(200)}")
        }

        println("--- Section Chunking ---")
        val chunks2 = TextChunk(fullText).chunkBySections(5000, combineShortSections = true)
        chunks2.take(10).forEach { section ->
            val text = fullText.substring(section.range).replace("\\s+".toRegex(), " ").trim()
            println("  ${section.range} ${text.take(200)}")
        }
    }

}