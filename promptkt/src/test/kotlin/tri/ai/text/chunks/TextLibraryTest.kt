package tri.ai.text.chunks

import com.fasterxml.jackson.module.kotlin.readValue
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import tri.ai.embedding.TextChunkerTest

class TextLibraryTest {

    @Test
    fun testTextLibrary() {
        val lib = TextLibrary("test library").apply {
            books.add(TextBook("test book").apply {
                chunks.add(TextChunkRaw("this is a raw string"))
            })
            val raw = TextChunkRaw("this is all the content in this book")
            books.add(TextBook("test book 2", raw).apply {
                chunks.add(TextChunkInBook(0..20))
                chunks.add(TextChunkInBook(20..35))
            })
        }
        val str = TextLibrary.MAPPER
            .writerWithDefaultPrettyPrinter()
            .writeValueAsString(lib)
        println(str)
        val lib2 = TextLibrary.MAPPER.readValue<TextLibrary>(str)
        println(TextLibrary.MAPPER.writerWithDefaultPrettyPrinter().writeValueAsString(lib2))
    }

}