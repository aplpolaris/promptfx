package tri.promptfx.apps

import com.fasterxml.jackson.annotation.JsonIgnore
import javafx.beans.property.SimpleObjectProperty
import javafx.beans.value.ObservableValue
import javafx.scene.Node
import javafx.scene.control.Hyperlink
import javafx.scene.text.Text
import tornadofx.action
import tornadofx.observableListOf
import tornadofx.runLater
import tri.ai.core.TextCompletion
import tri.ai.embedding.*
import tri.ai.openai.instructTask
import tri.ai.pips.AiTaskResult
import tri.ai.pips.aitask
import java.io.File

/** Runs the document QA information retrieval, query, and summarization process. */
class DocumentQaPlanner {

    /** The embedding index. */
    var embeddingIndex: ObservableValue<out EmbeddingIndex?> = SimpleObjectProperty(NoOpEmbeddingIndex)
    /** The retrieved relevant snippets. */
    val snippets = observableListOf<SnippetMatch>()
    /** The most recent result of the QA task. */
    var lastResult: QuestionAnswerResult? = null

    /** The operation used to browse to a chunk. */
    var chunkOp: (EmbeddingMatch) -> Unit = { }

    /**
     * Asynchronous tasks to execute for answering the given question.
     * @param question question to answer
     * @param promptId prompt to use for answering the question
     * @param embeddingService embedding service to use
     * @param chunksToRetrieve number of chunks to retrieve
     * @param minChunkSize minimum size of a chunk for use in a prompt
     * @param contextStrategy strategy for constructing the context
     * @param contextChunks how many of the retrieved chunks to use for constructing the context
     * @param completionEngine completion engine to use
     * @param maxTokens maximum number of tokens to generate
     */
    fun plan(
        question: String,
        promptId: String,
        embeddingService: EmbeddingService,
        chunksToRetrieve: Int,
        minChunkSize: Int,
        contextStrategy: ContextStrategy,
        contextChunks: Int,
        completionEngine: TextCompletion,
        maxTokens: Int,
    ) = aitask("calculate-embeddings") {
        runLater { snippets.setAll() }
        findRelevantSection(question, chunksToRetrieve).also {
            runLater { snippets.setAll(it.value) }
        }
    }.aitask("question-answer") {
        val queryChunks = it.filter { it.snippetLength >= minChunkSize }
            .take(contextChunks)
        val context = contextStrategy.constructContext(queryChunks)
        val response = completionEngine.instructTask(promptId, question, context, maxTokens)
        val responseEmbedding = response.value?.let { embeddingService.calculateEmbedding(it) }
        response.map {
            QuestionAnswerResult(
                modelId = completionEngine.modelId,
                embeddingId = embeddingService.modelId,
                promptId = promptId,
                question = question,
                questionEmbedding = queryChunks.first().embeddingMatch.queryEmbedding,
                matches = snippets,
                response = response.value,
                responseEmbedding = responseEmbedding
            )
        }
    }.task("process-result") {
        println("Similarity of question to response: " + it.questionAnswerSimilarity())
        lastResult = it
        formatResult(it, chunkOp)
    }.planner

    //region SIMILARITY CALCULATIONS

    /** Finds the most relevant section to the query. */
    private suspend fun findRelevantSection(query: String, maxChunks: Int): AiTaskResult<List<SnippetMatch>> {
        val matches = embeddingIndex.value!!.findMostSimilar(query, maxChunks)
        return AiTaskResult.result(matches.map { SnippetMatch(it) })
    }

    suspend fun reindexAllDocuments() {
        (embeddingIndex.value as? LocalEmbeddingIndex)?.reindexAll()
    }

    //endregion

    //region FORMATTING RESULTS OF QA

    /** Formats the result of the QA task. */
    private fun formatResult(qaResult: QuestionAnswerResult, chunkOp: (EmbeddingMatch) -> Unit): FormattedText {
        val result = mutableListOf(FormattedTextNode(qaResult.response ?: "No response."))
        val docs = qaResult.matches.map { it.document }.toSet()
        docs.forEach { doc ->
            result.splitOn(doc) {
                val sourceDoc = qaResult.matches.first { it.document == doc }.embeddingMatch.document
                FormattedTextNode(sourceDoc.shortNameWithoutExtension, hyperlink = sourceDoc.path)
            }
        }
        result.splitOn("Citations:") {
            FormattedTextNode(it, style = "-fx-font-weight: bold;")
        }
        result.splitOn("\\[[0-9]+(,\\s*[0-9]+)*]".toRegex()) {
            FormattedTextNode(it, style = "-fx-fill: #8abccf; -fx-font-weight: bold;")
        }
        return FormattedText(result)
    }

    //endregion

}

//region DATA OBJECTS DESCRIBING TASK

/** Result object. */
data class QuestionAnswerResult(
    val modelId: String,
    val embeddingId: String,
    val promptId: String?,
    val question: String,
    val questionEmbedding: List<Double>,
    val matches: List<SnippetMatch>,
    val response: String?,
    val responseEmbedding: List<Double>?
) {
    override fun toString() = response ?: "No response. Question: $question"

    /** Calculates the similarity between the question and response. */
    internal fun questionAnswerSimilarity() = responseEmbedding?.let { cosineSimilarity(questionEmbedding, it) } ?: 0
}

/** A snippet match that can be serialized. */
data class SnippetMatch(
    @get:JsonIgnore val embeddingMatch: EmbeddingMatch,
    val document: String,
    val snippetStart: Int,
    val snippetEnd: Int,
    val snippetText: String,
    val snippetEmbedding: List<Double>,
    val score: Double
) {
    @get:JsonIgnore
    val snippetLength = snippetEnd - snippetStart

    constructor(match: EmbeddingMatch) : this(
        match,
        match.document.shortName,
        match.section.start,
        match.section.end,
        match.readText(),
        match.section.embedding,
        match.score
    )

    /** Test for a matching document. */
    fun matchesDocument(doc: String) = embeddingMatch.document.shortNameWithoutExtension == doc
}

//endregion

//region DATA OBJECTS DESCRIBING FORMATTED TEXT

/** A result that contains plain text and links. */
class FormattedText(val nodes: List<FormattedTextNode>) {
    var hyperlinkOp: (String) -> Unit = { }
    override fun toString() = nodes.joinToString("") { it.text }
}

/** A text node within [FormattedText]. */
class FormattedTextNode(
    val text: String,
    val style: String? = null,
    val hyperlink: String? = null
) {
    /** Splits this node by a [Regex], keeping the same style and hyperlink. */
    fun splitOn(find: Regex, replace: (String) -> FormattedTextNode): List<FormattedTextNode> {
        val result = mutableListOf<FormattedTextNode>()
        var index = 0
        find.findAll(text).forEach {
            val text0 = text.substring(index, it.range.first)
            if (text0.length > 1) {
                result += FormattedTextNode(text0, style, hyperlink)
            }
            result += replace(it.value)
            index = it.range.last + 1
        }
        result += FormattedTextNode(text.substring(index), style, hyperlink)
        return result
    }
}

/** Splits all text elements on a given search string. */
private fun MutableList<FormattedTextNode>.splitOn(find: String, replace: (String) -> FormattedTextNode) =
    splitOn(Regex.fromLiteral(find), replace)

/** Splits all text elements on a given search string. */
private fun MutableList<FormattedTextNode>.splitOn(find: Regex, replace: (String) -> FormattedTextNode) =
    toList().forEach {
        val newNodes = it.splitOn(find, replace)
        if (newNodes != listOf(it)) {
            addAll(indexOf(it), newNodes)
            remove(it)
        }
    }

/** Convert a [FormattedText] to HTML. */
fun FormattedText.toHtml(): String {
    val text = StringBuilder("<html>\n")
    nodes.forEach {
        val style = it.style ?: ""
        val bold = style.contains("-fx-font-weight: bold")
        val italic = style.contains("-fx-font-style: italic")
        val color = if (style.contains("-fx-fill:"))
            style.substringAfter("-fx-fill:").let { it.substringBefore(";", it) }
        else null
        val prefix = (if (bold) "<b>" else "") + (if (italic) "<i>" else "") +
                (if (color != null) "<font color=\"$color\">" else "")
        val textWithBreaks = it.text.replace("\n", "<br>")
        val suffix = (if (color != null) "</font>" else "") +
                (if (italic) "</i>" else "") + (if (bold) "</b>" else "")
        text.append(prefix)
        if (it.hyperlink != null)
            text.append("<a href=\"$textWithBreaks\">${it.hyperlink}</a>")
        else
            text.append(textWithBreaks)
        text.append(suffix)
    }
    return text.toString()
}

/** Convert a [FormattedText] to JavaFx nodes. */
fun FormattedText.toFxNodes() =
    nodes.map { it.toFxNode(hyperlinkOp) }

/** Convert a formatted text node to an FX node. */
fun FormattedTextNode.toFxNode(hyperlinkOp: (String) -> Unit): Node =
    when (hyperlink) {
        null -> Text(text).apply { style = style }
        else -> Hyperlink(text).apply {
            style = style
            action { hyperlinkOp(text) }
        }
    }

//endregion

//region CONTEXT TEXT BUILDERS

/**
 * Strategy for constructing a context from a set of matches.
 * This may rearrange the snippets and/or construct the concatenated text for an LLM query.
 */
sealed class ContextStrategy(val id: String) {
    /** Constructs the context from the given matches. */
    abstract fun constructContext(matches: List<SnippetMatch>): String
}

class ContextStrategyBasic : ContextStrategy("Basic") {
    override fun constructContext(matches: List<SnippetMatch>) =
        matches.joinToString("\n```\n") {
            "Document: ${File(it.embeddingMatch.document.path).name}\n```\nRelevant Text: ${it.snippetText}"
        }
}

class ContextStrategyBasic2 : ContextStrategy("Basic") {
    override fun constructContext(matches: List<SnippetMatch>): String {
        val docs = matches.groupBy { it.embeddingMatch.document.shortName }
        return docs.entries.mapIndexed { i, entry ->
            "  - \"\"" + entry.value.joinToString("\n... ") { it.snippetText.trim() } + "\"\"" +
                    " [${i+1}] ${entry.key}"
        }.joinToString("\n  ")
    }
}

class ContextStrategyBasic3 : ContextStrategy("Basic") {
    override fun constructContext(matches: List<SnippetMatch>): String {
        var n = 1
        val docs = matches.groupBy { it.embeddingMatch.document.shortName }
        return docs.map { (doc, text) ->
            val combinedText = text.joinToString("\n... ") { it.snippetText.trim() }
            "[[Citation ${n++}]] ${doc}\n\"\"\"\n$combinedText\n\"\"\"\n"
        }.joinToString("\n\n")
    }
}

//endregion