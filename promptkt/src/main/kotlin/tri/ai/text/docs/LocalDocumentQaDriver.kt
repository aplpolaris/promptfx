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
package tri.ai.text.docs

import tri.ai.core.TextPlugin
import tri.ai.embedding.LocalFolderEmbeddingIndex
import tri.ai.pips.AiPipelineExecutor
import tri.ai.pips.AiPipelineResult
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
            require(value == "" || value in folders) { "Expected blank folder (to use root folder) or value to be a subfolder of root, but was '$value'." }
            field = value
        }

    val docsFolder
        get() = if (folder == "") root else File(root, folder)

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
    override var temp: Double = 1.0
    override var maxTokens: Int = 2000

    private val prompt = AiPromptLibrary.lookupPrompt("$PROMPT_PREFIX-docs")
    private val joiner = GroupingTemplateJoiner("$JOINER_PREFIX-citations")

    override fun initialize() {
    }

    override fun close() {
        TextPlugin.orderedPlugins.forEach { it.close() }
    }

    override suspend fun answerQuestion(input: String): AiPipelineResult<String> {
        val index = LocalFolderEmbeddingIndex(docsFolder, embeddingModelInst)
        val planner = DocumentQaPlanner(index, completionModelInst, listOf(), 1).plan(
            question = input,
            prompt = prompt,
            chunksToRetrieve = 8,
            minChunkSize = 50,
            contextStrategy = joiner,
            contextChunks = 10,
            maxTokens = maxTokens,
            temp = temp,
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
