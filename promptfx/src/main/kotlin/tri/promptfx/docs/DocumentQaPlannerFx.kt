package tri.promptfx.docs

import javafx.beans.property.SimpleIntegerProperty
import javafx.beans.property.SimpleObjectProperty
import javafx.beans.value.ObservableValue
import tornadofx.observableListOf
import tornadofx.runLater
import tri.ai.core.MChatRole
import tri.ai.core.TextChatMessage
import tri.ai.core.TextCompletion
import tri.ai.embedding.EmbeddingIndex
import tri.ai.embedding.EmbeddingMatch
import tri.ai.embedding.LocalFolderEmbeddingIndex
import tri.ai.embedding.NoOpEmbeddingIndex
import tri.ai.pips.AiTask
import tri.ai.pips.AiTaskList
import tri.ai.prompt.AiPrompt
import tri.ai.text.chunks.GroupingTemplateJoiner
import tri.ai.text.chunks.TextLibrary
import tri.ai.text.docs.DocumentQaPlanner
import tri.ai.text.docs.FormattedPromptTraceResult
import tri.ai.text.docs.QuestionAnswerResult
import tri.util.ANSI_GRAY
import tri.util.ANSI_RESET
import tri.util.info

/** Computation planner for [DocumentQaView]. */
class DocumentQaPlannerFx {
    /** A document library to use for chunks, if available. */
    var documentLibrary = SimpleObjectProperty<TextLibrary>(null)
    /** The embedding index. */
    var embeddingIndex: ObservableValue<out EmbeddingIndex?> = SimpleObjectProperty(NoOpEmbeddingIndex)
    /** The retrieved relevant snippets. */
    val snippets = observableListOf<EmbeddingMatch>()
    /** The most recent result of the QA task. */
    var lastResult: QuestionAnswerResult? = null
    /** The chat history. */
    var chatHistory = observableListOf<TextChatMessage>()
    /** The size of the chat history. */
    var historySize = SimpleIntegerProperty(4)

    /** Reindexes all documents in the current [EmbeddingIndex] (if applicable). */
    suspend fun reindexAllDocuments() {
        (embeddingIndex.value as? LocalFolderEmbeddingIndex)?.reindexAll()
    }

    fun taskList(
        question: String,
        prompt: AiPrompt?,
        chunksToRetrieve: Int?,
        minChunkSize: Int?,
        contextStrategy: GroupingTemplateJoiner,
        contextChunks: Int?,
        completionEngine: TextCompletion?,
        maxTokens: Int?,
        temp: Double?,
        numResponses: Int?
    ): AiTaskList<String> {
        val p = DocumentQaPlanner(embeddingIndex.value!!, completionEngine!!, chatHistory, historySize.value).plan(
            question = question,
            prompt = prompt!!,
            chunksToRetrieve = chunksToRetrieve!!,
            minChunkSize = minChunkSize!!,
            contextStrategy = contextStrategy,
            contextChunks = contextChunks!!,
            maxTokens = maxTokens!!,
            temp = temp!!,
            numResponses = numResponses!!,
            snippetCallback = { runLater { snippets.setAll(it) } }
        )
        return AiTaskList(p.plan.dropLast(2), p.plan.dropLast(1).last() as AiTask<QuestionAnswerResult>)
            .aitask("process-result") {
                info<DocumentQaPlanner>("$ANSI_GRAY Similarity of question to response: ${it.responseScore}$ANSI_RESET")
                lastResult = it
                chatHistory.add(TextChatMessage(MChatRole.User, question))
                chatHistory.add(TextChatMessage(MChatRole.Assistant, it.trace.firstValue))
                FormattedPromptTraceResult(it.trace, it.splitOutputs().map { it.formatResult() })
            }
    }
}