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
package tri.util.io.pdf

import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import tri.ai.core.TextPlugin
import tri.ai.process.pdf.PdfMetadataGuesser
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
        assertEquals(7, metadata.size)
        assertEquals("Microsoft® Word LTSC", metadata["pdf.creator"])
    }

    @Tag("openai")
    @Test
    fun testPdfGuesser() {
        runBlocking {
            val metadata = PdfMetadataGuesser.guessPdfMetadata(TextPlugin.chatModels().first(), file, 2) {
                println(it)
            }
            println(metadata)
        }
    }

}
