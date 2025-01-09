package tri.ai.text.docs

import tri.ai.core.TextPlugin
import tri.ai.embedding.LocalFolderEmbeddingIndex
import tri.ai.pips.AiPipelineExecutor
import tri.ai.pips.AiPipelineResult
import tri.ai.pips.IgnoreMonitor
import tri.ai.pips.PrintMonitor
import tri.ai.prompt.AiPromptLibrary
import tri.ai.prompt.trace.AiPromptTraceSupport
import tri.ai.text.chunks.GroupingTemplateJoiner
import java.io.File

/**
 * A local file driver for document Q&A, using plugins for completion and embedding models.
 * This driver requires a root folder, using the child folders as the set of available document sets.
 * Documents and embeddings within a folder are managed by [LocalFolderEmbeddingIndex].
 */
class LocalDocumentQaDriver(val root: File) : DocumentQaDriver {

    init {
        require(root.exists()) { "Root directory does not exist" }
        require(root.isDirectory) { "Root must be a directory" }
    }

    override val folders: List<String> =
        root.listFiles()!!
            .filter { it.isDirectory }
            .map { it.name }
    override var folder: String = ""
        set(value) {
            require(value in folders) { "Folder $value not found" }
            field = value
        }

    private var completionModelInst = TextPlugin.textCompletionModels().first()
    private var embeddingModelInst = TextPlugin.embeddingModels().first()

    override var completionModel
        get() = completionModelInst.modelId
        set(value) {
            completionModelInst = TextPlugin.textCompletionModels().first { it.modelId == value }
        }
    override var embeddingModel
        get() = embeddingModelInst.modelId
        set(value) {
            embeddingModelInst = TextPlugin.embeddingModels().first { it.modelId == value }
        }

    private val prompt = AiPromptLibrary.lookupPrompt("$PROMPT_PREFIX-docs")
    private val joiner = GroupingTemplateJoiner("$JOINER_PREFIX-citations")

    override fun initialize() {
    }

    override fun close() {
        TextPlugin.orderedPlugins.forEach { it.close() }
    }

    override suspend fun answerQuestion(input: String): AiPipelineResult<String> {
        val index = LocalFolderEmbeddingIndex(File(root, folder), embeddingModelInst)
        val planner = DocumentQaPlanner(index, completionModelInst).plan(
            question = input,
            prompt = prompt,
            chunksToRetrieve = 8,
            minChunkSize = 50,
            contextStrategy = joiner,
            contextChunks = 10,
            maxTokens = 1000,
            temp = 1.0,
            numResponses = 1,
            snippetCallback = { }
        )
        val monitor = PrintMonitor()
        val result = AiPipelineExecutor.execute(planner.plan, monitor).finalResult as AiPromptTraceSupport<String>
        return result.asPipelineResult()
    }

    companion object {
        const val PROMPT_PREFIX = "question-answer"
        const val JOINER_PREFIX = "snippet-joiner"
    }

}