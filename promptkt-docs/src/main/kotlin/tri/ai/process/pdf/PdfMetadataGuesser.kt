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
package tri.ai.process.pdf

import com.fasterxml.jackson.databind.JsonMappingException
import com.fasterxml.jackson.module.kotlin.readValue
import tri.ai.core.MChatVariation
import tri.ai.core.TextCompletion
import tri.ai.openai.jsonMapper
import tri.ai.prompt.PromptDef
import tri.ai.prompt.PromptLibrary
import tri.ai.prompt.fill
import tri.ai.prompt.template
import tri.ai.text.chunks.TextDocMetadata
import tri.util.fine
import tri.util.io.pdf.PdfUtils
import java.io.File

/** Attempts to guess metadata from a PDF file. */
object PdfMetadataGuesser {

    private val PROMPT = PromptLibrary.INSTANCE.get("docs-metadata/guess")!!

    /** Attempt to extract metadata from a PDF file, using up to [pageLimit] pages. Each page is processed by an LLM query, so this may consume a lot of tokens. */
    suspend fun guessPdfMetadata(model: TextCompletion, file: File, pageLimit: Int, progress: (String) -> Unit): MultipleGuessedMetadataObjects {
        val text = PdfUtils.pdfPageInfo(file).take(pageLimit)
        val parsedMetadata = text.mapNotNull {
            val progressString = "Processing page ${it.pageNumber} for ${file.name}..."
            progress(progressString)
            val result = model.complete(PROMPT.template().fillInput(it.text), tokens = 1000, variation = MChatVariation(temperature = 0.2)).firstValue
            val parsed = result
                .betweenTripleTicks()
                .betweenBraces()
                .attemptToParse(label = "Page ${it.pageNumber}")
            if (parsed == null) null else it to parsed
        }.toMap()
        return parsedMetadata.values.resolveConflicts()
    }

    /** Convert [tri.ai.text.chunks.TextDocMetadata] to a [GuessedMetadataObject]. */
    fun TextDocMetadata.toGuessedMetadataObject(label: String) = GuessedMetadataObject(
        label,
        title = title,
        subtitle = properties[SUBTITLE]?.toString(),
        authors = author?.attemptParseToList(),
        date = dateTime?.toString() ?: date?.toString(),
        keywords = listProperty(KEYWORDS),
        abstract = properties[ABSTRACT]?.toString(),
        executiveSummary = properties["executiveSummary"]?.toString(),
        sections = listProperty(SECTIONS),
        captions = listProperty(CAPTIONS),
        references = listProperty(REFERENCES),
        other = properties.filterKeys { it !in listOf(TITLE,
            SUBTITLE, "author", DATE, KEYWORDS, ABSTRACT, "executiveSummary", SECTIONS, CAPTIONS, REFERENCES
        ) }.filterValues { it != null } as Map<String, Any>
    )

    //region HELPERS

    /**
     * Resolve any conflicting information in metadata. The first result is the attempted deconflicted object. The remaining are the inputs.
     * Empty results are not included.
     */
    fun Collection<GuessedMetadataObject>.resolveConflicts(): MultipleGuessedMetadataObjects {
        val title = firstNotNullOfOrNull { it.title }
        val subtitle = firstNotNullOfOrNull { it.subtitle }
        val authors = firstNotNullOfOrNull { it.authors }
        val date = firstNotNullOfOrNull { it.date }
        val keywords = firstOrNull { it.keywords?.isNotEmpty() ?: false }?.keywords ?: emptyList()
        val abstract = firstNotNullOfOrNull { it.abstract }
        val executiveSummary = firstNotNullOfOrNull { it.executiveSummary }
        val sections = flatMap { it.sections ?: listOf() }.distinct()
        val captions = flatMap { it.captions ?: listOf() }.distinct()
        val references = flatMap { it.references ?: listOf() }.distinct()
        val other = flatMap { it.other.entries }.groupBy { it.key }.mapValues {
            it.value.map { it.value }.distinct().let {
                if (it.size == 1) it[0] else it
            }
        }
        val combined = GuessedMetadataObject(COMBINED, title, subtitle, authors, date, keywords, abstract, executiveSummary, sections, captions, references, other)
        return MultipleGuessedMetadataObjects(combined, this.toList())
    }

    private fun String.betweenTripleTicks() = if ("```" in this) substringAfter("```").substringAfter("\n").substringBefore("```").trim() else trim()
    private fun String.betweenBraces() = "{" + substringAfter("{").substringBeforeLast("}") + "}"

    private fun String.attemptToParse(label: String): GuessedMetadataObject? = try {
        val content = jsonMapper.readValue<Map<String, Any>>(this)
        GuessedMetadataObject(
            label,
            content[TITLE].string(),
            content[SUBTITLE].string(),
            content[AUTHORS].stringlist(),
            content[DATE].string(),
            content[KEYWORDS].stringlist(),
            content[ABSTRACT].string(),
            content[EXECUTIVE_SUMMARY].string(),
            content[SECTIONS].stringlist(),
            content[CAPTIONS].stringlist(),
            content[REFERENCES].stringlist(),
            content[OTHER] as? Map<String, Any> ?: emptyMap()
        )
    } catch (x: JsonMappingException) {
        fine<PdfMetadataGuesser>("Failed to parse metadata: $this")
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

    //endregion

    //region CONSTANTS

    const val COMBINED = "Combined"
    private const val TITLE = "title"
    private const val SUBTITLE = "subtitle"
    private const val AUTHORS = "authors"
    private const val DATE = "date"
    private const val KEYWORDS = "keywords"
    private const val ABSTRACT = "abstract"
    private const val EXECUTIVE_SUMMARY = "executive_summary"
    private const val SECTIONS = "sections"
    private const val CAPTIONS = "captions"
    private const val REFERENCES = "references"
    private const val OTHER = "other"

    //endregion

}

/** Values obtained by guessing metadata from a text sample. */
data class GuessedMetadataObject(
    val label: String,
    val title: String?,
    val subtitle: String?,
    val authors: List<String>?,
    val date: String?,
    val keywords: List<String>?,
    val abstract: String?,
    val executiveSummary: String?,
    val sections: List<String>?,
    val captions: List<String>?,
    val references: List<String>?,
    val other: Map<String, Any>
)

/** Collection of multiple guessed objects. Supports a "combined" object and multiple "source" objects. */
class MultipleGuessedMetadataObjects(
    val combined: GuessedMetadataObject,
    val sources: List<GuessedMetadataObject>
)

private fun TextDocMetadata.listProperty(key: String) =
    (properties[key] as? List<String>) ?: properties[key]?.toString()?.attemptParseToList()

private fun String.attemptParseToList() =
    split(",").map { it.trim() }
