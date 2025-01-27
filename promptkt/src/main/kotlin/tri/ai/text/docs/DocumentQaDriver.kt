package tri.ai.text.docs

import tri.ai.pips.AiPipelineResult

/** Provides access to necessary components for document Q&A. */
interface DocumentQaDriver {

    /** Set of folders, or document sets, to work with. */
    val folders: List<String>
    /** The currently selected folder, or document set. */
    var folder: String

    /** The text completion model (by id). */
    var completionModel: String
    /** The text embedding model (by id). */
    var embeddingModel: String

    /** Initialize the driver. */
    fun initialize()
    /** Close the driver. */
    fun close()

    /** Answer a question using documents in the current folder. */
    suspend fun answerQuestion(input: String): AiPipelineResult<String>

}

