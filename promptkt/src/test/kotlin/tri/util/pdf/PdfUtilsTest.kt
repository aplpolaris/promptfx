package tri.util.pdf

import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import tri.ai.core.TextPlugin
import tri.ai.text.chunks.process.PdfMetadataGuesser
import kotlin.io.path.toPath

class PdfUtilsTest {

    private val file = PdfUtilsTest::class.java.getResource("resources/TEST.pdf")!!.toURI().toPath().toFile()

    @Test
    fun testPdfPages() {
        val pages = PdfUtils.pdfPageInfo(file)
        pages.forEach {
            println("-".repeat(100))
            println("Page ${it.pageNumber}")
            println("  Text: ${it.text.take(100)}...")
            it.images.forEachIndexed { i, img ->
                println("  Image $i: ${img.bounds} ${img.width}✖${img.height} ${img.image!!.colorModel.pixelSize} bits per pixel")
            }
            it.links.forEachIndexed { i, link ->
                println("  Link $i: $link")
            }
        }
        assert(pages[0].text.startsWith("Galaxy: Link Space Visualization and Analysis of Network Traffic"))
        assertEquals(3, pages[0].images.size)
    }

    @Test
    fun testPdfMetadata() {
        val metadata = PdfUtils.pdfMetadata(file)
        println(metadata)
        assertEquals(9, metadata.size)
        assertEquals("Microsoft® Word LTSC", metadata["pdf.creator"])
    }

    @Disabled("This test requires an OpenAI API key")
    @Test
    fun testPdfGuesser() {
        runBlocking {
            val metadata = PdfMetadataGuesser.guessPdfMetadata(TextPlugin.textCompletionModels().first(), file, 2)
            println(metadata)
        }
    }

}
