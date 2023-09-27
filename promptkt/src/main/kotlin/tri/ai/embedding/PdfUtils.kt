/*-
 * #%L
 * promptkt-0.1.10-SNAPSHOT
 * %%
 * Copyright (C) 2023 Johns Hopkins University Applied Physics Laboratory
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
package tri.ai.embedding

import org.apache.pdfbox.pdmodel.PDDocument
import org.apache.pdfbox.text.PDFTextStripper
import java.io.File

/** Extract text from PDF. */
fun pdfText(file: File): String {
    val pageText = mutableMapOf<Int, String>()
    PDDocument.load(file).use { doc ->
        (1..doc.numberOfPages).forEach {
            pageText[it] = PDFTextStripper().apply {
                startPage = it
                endPage = it
            }.getText(doc)
        }
    }
    return pageText.entries.joinToString("\n\n\n") { it.value }
}

/**
 * Look for a matching text in PDF file, returning the page number if found.
 * Since the text may be split across pages, we check for matches of substrings of the text -- the first 20, the last 20,
 * and the middle 20, with the middle 20 weighted higher in case of ties.
 */
fun findTextInPdf(pdfFile: File, searchText: String): Int {
    val mid20 = searchText.substring(searchText.length / 2 - 10, searchText.length / 2 + 10)
    val searchFor = listOf(searchText.take(20), mid20, mid20, searchText.takeLast(20))
    val partialMatches = mutableMapOf<Int, Pair<String, Int>>()
    PDDocument.load(pdfFile).use { document ->
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
