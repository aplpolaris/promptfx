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
package tri.ai.text.chunks.process

import com.fasterxml.jackson.databind.JsonMappingException
import com.fasterxml.jackson.module.kotlin.readValue
import tri.ai.core.TextCompletion
import tri.ai.openai.jsonMapper
import tri.ai.prompt.AiPrompt
import tri.ai.prompt.AiPromptLibrary
import tri.util.info
import tri.util.pdf.PdfUtils
import java.io.File

/** Attempts to guess metadata from a PDF file. */
object PdfMetadataGuesser {

    private val PROMPT = AiPromptLibrary.lookupPrompt("document-guess-metadata")

    /** Attempt to extract metadata from a PDF file, using up to [pageLimit] pages. Each page is processed by an LLM query, so this may consume a lot of tokens. */
    suspend fun guessPdfMetadata(model: TextCompletion, file: File, pageLimit: Int): GuessedMetadataObject {
        val text = PdfUtils.pdfPageInfo(file).take(pageLimit)
        val parsedMetadata = text.mapNotNull {
            info<PdfMetadataGuesser>("Processing page ${it.pageNumber} for $file...")
            val result = model.complete(PROMPT.fill(AiPrompt.INPUT to it.text), tokens = 1000, temperature = 0.2).value!!
            val parsed = result
                .betweenTripleTicks()
                .betweenBraces()
                .attemptToParse()
            if (parsed == null) null else it to parsed
        }.toMap()
        return parsedMetadata.values.resolveConflicts()
    }

    /** Resolve any conflicting information in metadata. */
    private fun Collection<GuessedMetadataObject>.resolveConflicts(): GuessedMetadataObject {
        val title = firstNotNullOfOrNull { it.title }
        val subtitle = firstNotNullOfOrNull { it.subtitle }
        val authors = firstNotNullOfOrNull { it.authors }
        val date = firstNotNullOfOrNull { it.date }
        val keywords = firstOrNull { it.keywords.isNotEmpty() }?.keywords ?: emptyList()
        val abstract = firstNotNullOfOrNull { it.abstract }
        val executiveSummary = firstNotNullOfOrNull { it.executiveSummary }
        val sections = flatMap { it.sections }.distinct()
        val captions = flatMap { it.captions }.distinct()
        val references = flatMap { it.references }.distinct()
        val other = flatMap { it.other.entries }.groupBy { it.key }.mapValues { it.value.map { it.value }.distinct() }
        return GuessedMetadataObject(title, subtitle, authors, date, keywords, abstract, executiveSummary, sections, captions, references, other)
    }

    private fun String.betweenTripleTicks() = if ("```" in this) substringAfter("```").substringAfter("\n").substringBefore("```").trim() else trim()
    private fun String.betweenBraces() = "{" + substringAfter("{").substringBeforeLast("}") + "}"

    private fun String.attemptToParse(): GuessedMetadataObject? = try {
        val content = jsonMapper.readValue<Map<String, Any>>(this)
        GuessedMetadataObject(
            content["title"].string(),
            content["subtitle"].string(),
            content["authors"].stringlist(),
            content["date"].string(),
            content["keywords"].stringlist(),
            content["abstract"].string(),
            content["executive_summary"].string(),
            content["sections"].stringlist(),
            content["captions"].stringlist(),
            content["references"].stringlist(),
            content["other"] as? Map<String, Any> ?: emptyMap()
        )
    } catch (x: JsonMappingException) {
        println("Failed to parse metadata: $this")
        null
    }

    private fun Any?.string() = when {
        this == null || this == "NONE" || this == "" -> null
        else -> toString()
    }

    private fun Any?.stringlist() = when (this) {
        null, "NONE", "" -> emptyList()
        is List<*> -> map { it.toString() }
        else -> emptyList()
    }.filter { it != "NONE" }

}

data class GuessedMetadataObject(
    val title: String?,
    val subtitle: String?,
    val authors: List<String>?,
    val date: String?,
    val keywords: List<String>,
    val abstract: String?,
    val executiveSummary: String?,
    val sections: List<String>,
    val captions: List<String>,
    val references: List<String>,
    val other: Map<String, Any>
)
