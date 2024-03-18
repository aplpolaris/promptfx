package tri.ai.text.chunks

import com.fasterxml.jackson.module.kotlin.readValue
import org.junit.jupiter.api.Test

class TextLibraryTest {

    @Test
    fun testTextLibrary() {
        val lib = TextLibrary("test library").apply {
            books.add(TextDoc("test book").apply {
                chunks.add(TextChunkRaw("this is a raw string"))
            })
            val raw = TextChunkRaw("this is all the content in this book")
            books.add(TextDoc("test book 2", raw).apply {
                chunks.add(TextChunkInDoc(0..20))
                chunks.add(TextChunkInDoc(20..35))
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