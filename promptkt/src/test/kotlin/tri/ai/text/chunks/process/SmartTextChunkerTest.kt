package tri.ai.text.chunks.process

import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import tri.ai.text.chunks.TextDoc
import tri.ai.text.chunks.TextChunkInDoc
import tri.ai.text.chunks.process.SmartTextChunker.Companion.chunkWhile

class SmartTextChunkerTest {

    @Test
    fun testChunkWhile() {
        val list = listOf(1, 2, 3, 4, 5, 6, 7)
        val result = list.chunkWhile { it.sum() <= 6 }
        Assertions.assertEquals(listOf(listOf(1, 2, 3), listOf(4), listOf(5), listOf(6), listOf(7)), result)
    }

    @Test
    fun testTextChunking() {
        val chunker = SmartTextChunker(5000)
        val fullText = SmartTextChunkerTest::class.java.getResource("resources/pg1513.txt")!!.readText()

        println("--- Simple ---")
        val chunks1 = chunker.chunkTextBySectionsSimple(fullText)
        chunks1.take(10).forEach { section ->
            val text = fullText.substring(section.first).replace("\\s+".toRegex(), " ")
            println("  ${section.first} ${text.take(200)}")
        }

        println("--- Section Chunking ---")
        val chunks2 = with(chunker) {
            TextDoc("id", fullText).all!!.chunkBySections(combineShortSections = true)
        }
        chunks2.take(10).forEach { section ->
            val sb = section as TextChunkInDoc
            val text = fullText.substring(sb.range).replace("\\s+".toRegex(), " ").trim()
            println("  ${sb.range} ${text.take(200)}")
        }
    }

}