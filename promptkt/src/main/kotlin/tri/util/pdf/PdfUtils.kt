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
package tri.util.pdf

import org.apache.pdfbox.Loader
import org.apache.pdfbox.pdmodel.PDDocument
import org.apache.pdfbox.pdmodel.PDPage
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject
import org.apache.pdfbox.pdmodel.interactive.action.PDActionURI
import org.apache.pdfbox.pdmodel.interactive.annotation.PDAnnotationLink
import org.apache.pdfbox.text.PDFTextStripper
import org.apache.pdfbox.text.PDFTextStripperByArea
import java.awt.geom.Rectangle2D
import java.io.File
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId


/** Utilities for working with PDF files. */
object PdfUtils {

    /** Extract metadata from PDF. */
    fun pdfMetadata(file: File) = Loader.loadPDF(file).use {
        it.documentInformation.let {
            mapOf(
                "pdf.title" to it.title,
                "pdf.author" to it.author,
                "pdf.subject" to it.subject,
                "pdf.keywords" to it.keywords,
                "pdf.creator" to it.creator,
                "pdf.producer" to it.producer,
                "pdf.creationDate" to LocalDateTime.ofInstant(it.creationDate.toInstant(), ZoneId.systemDefault()),
                "pdf.modificationDate" to LocalDateTime.ofInstant(
                    it.modificationDate.toInstant(),
                    ZoneId.systemDefault()
                ),
                "file.modificationDate" to LocalDateTime.ofInstant(
                    Instant.ofEpochMilli(file.lastModified()),
                    ZoneId.systemDefault()
                )
            )
        }
    }

    /** Extract text from PDF. */
    fun pdfText(file: File) =
        pdfPageInfo(file).joinToString("\n\n\n") { it.text }

    /** Extract text from PDF into pages. */
    fun pdfPageInfo(file: File) = Loader.loadPDF(file).use { doc ->
        (1..doc.numberOfPages).map { doc.pageInfo(it) }
    }

    //region PAGE UTILS

    /** Extract text from a specific page of a PDF. */
    private fun PDDocument.pageInfo(pageNumber: Int): PdfPageInfo {
        val page = getPage(pageNumber - 1)
        val text = textOnPage(pageNumber)
        val images = page.images()
        val links = page.links()
        return PdfPageInfo(pageNumber, text, images, links)
    }

    /** Extract text from a specific page of a PDF. */
    private fun PDDocument.textOnPage(pageNumber: Int): String =
        PDFTextStripper().apply {
            startPage = pageNumber
            endPage = pageNumber
        }.getText(this)

    /** Get links on a specific page of a PDF. */
    private fun PDPage.links(): List<PdfLinkInfo> {
        val uriList = annotations.filterIsInstance<PDAnnotationLink>().map {
            (it.action as? PDActionURI)?.uri
        }
        val textList = extractText(*linkAnnotationLocations().toTypedArray())
        return uriList.zip(textList).map { PdfLinkInfo(text = it.second.trim(), url = it.first ?: "") }
    }

    /** Extract images on a specific page of a PDF. */
    private fun PDPage.images(): List<PdfImageInfo> {
        val index = resources.xObjectNames.associateBy { it.name }
        return PdfImageFinder().apply { processPage(this@images) }.result.map {
            val img = (resources.getXObject(index[it.name]) as? PDImageXObject)?.image
            it.withImage(img)
        }
    }

    //endregion

    //region PARSING/FINDING UTILS

    /** Get locations of annotations on a page. */
    private fun PDPage.linkAnnotationLocations(): List<Rectangle2D.Float> =
        annotations.filterIsInstance<PDAnnotationLink>().map {
            val rect = it.rectangle;
            val y = if (rotation == 0)
                mediaBox.height - rect.upperRightY
            else
                rect.upperRightY
            Rectangle2D.Float(rect.lowerLeftX, y, rect.width, rect.height)
        }

    /** Get text in a given location on a page. */
    private fun PDPage.extractText(vararg loc: Rectangle2D.Float): List<String> {
        val stripper = PDFTextStripperByArea()
        loc.forEachIndexed { i, rect ->
            stripper.addRegion(i.toString(), rect)
        }
        stripper.extractRegions(this)
        return loc.withIndex().map {
            stripper.getTextForRegion(it.index.toString())
        }
    }

    //endregion

    /**
     * Look for a matching text in PDF file, returning the page number if found.
     * Since the text may be split across pages, we check for matches of substrings of the text -- the first 20, the last 20,
     * and the middle 20, with the middle 20 weighted higher in case of ties.
     */
    fun findTextInPdf(pdfFile: File, searchText: String): Int {
        val mid20 = searchText.substring(searchText.length / 2 - 10, searchText.length / 2 + 10)
        val searchFor = listOf(searchText.take(20), mid20, mid20, searchText.takeLast(20))
        val partialMatches = mutableMapOf<Int, Pair<String, Int>>()
        Loader.loadPDF(pdfFile).use { document ->
            val stripper = PDFTextStripper()
            val totalPages = document.numberOfPages
            for (page in 1..totalPages) {
                stripper.startPage = page
                stripper.endPage = page
                val textOnPage = stripper.getText(document)
                val matchCount = searchFor.count { it in textOnPage }
                if (matchCount == 4)
                    return page
                else if (matchCount > 0)
                    partialMatches[page] = textOnPage to matchCount
            }
        }
        return if (partialMatches.isEmpty()) -1 else
            partialMatches.maxByOrNull { it.value.second }!!.key
    }

}

/** Information on a PDF page. */
data class PdfPageInfo(
    val pageNumber: Int,
    val text: String,
    val images: List<PdfImageInfo>,
    val links: List<PdfLinkInfo>
)

/** Information on a PDF link. */
data class PdfLinkInfo(
    val text: String,
    val url: String
)
