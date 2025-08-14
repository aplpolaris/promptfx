/*-
 * #%L
 * tri.promptfx:promptfx
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
package tri.ai.cli

import com.aallam.openai.api.logging.LogLevel
import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.requireObject
import com.github.ajalt.clikt.core.subcommands
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.arguments.validate
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.validate
import com.github.ajalt.clikt.parameters.types.double
import com.github.ajalt.clikt.parameters.types.int
import com.github.ajalt.clikt.parameters.types.path
import kotlinx.coroutines.runBlocking
import tri.ai.core.TextPlugin
import tri.ai.embedding.EmbeddingStrategy
import tri.ai.embedding.LocalFolderEmbeddingIndex
import tri.ai.openai.OpenAiAdapter
import tri.ai.openai.OpenAiModelIndex
import tri.ai.text.chunks.LocalTextDocIndex
import tri.ai.text.chunks.SmartTextChunker
import tri.ai.text.docs.LocalDocumentQaDriver
import tri.util.*
import java.io.File
import java.nio.file.Path
import java.util.logging.Level
import kotlin.io.path.Path

object DocumentCliRunner {
    @JvmStatic
    fun main(args: Array<out String>) =
        DocumentCli()
            .subcommands(DocumentChat(), DocumentChunker(), DocumentEmbeddings(), DocumentQa())
            .main(args)
}

/** Base command for document QA. */
class DocumentCli : CliktCommand(name = "document") {
    private val root by option(help = "Root path containing folders or documents")
        .path(mustExist = true)
        .default(Path("."))
        .validate {
            require(it.toFile().isDirectory) { "Root path must be a directory." }
        }
    private val folder by option(help = "Folder containing documents (relative to root path)")
        .default("")
    private val model by option(help = "Chat/completion model to use (default ${OpenAiModelIndex.GPT35_TURBO_ID})")
        .default(OpenAiModelIndex.GPT35_TURBO_ID)
        .validate {
            require(it in TextPlugin.textCompletionModels().map { it.modelId }) { "Invalid model $it" }
        }
    private val embedding by option(help = "Embedding model to use (default ${OpenAiModelIndex.EMBEDDING_ADA})")
        .default(OpenAiModelIndex.EMBEDDING_ADA)
        .validate {
            require(it in TextPlugin.embeddingModels().map { it.modelId }) { "Invalid model $it" }
        }
    private val temp by option(help = "Temperature for completion (default 0.5)")
        .double()
        .default(0.5)
    private val maxTokens by option(help = "Maximum tokens for completion (default 2000)")
        .int()
        .default(2000)
    private val templateId by option(help = "Q&A prompt template id (qa/chat modes, default question-answer-docs)")

    override fun run() {
        currentContext.obj = DocumentQaConfig(root, folder, model, embedding, temp, maxTokens, templateId)
    }
}

/** Command-line app for asking questions of documents. */
class DocumentChat : CliktCommand(name = "chat", help = "Ask questions and switch between folders until done") {
    private val config by requireObject<DocumentQaConfig>()
    private val chatHistory by option(help = "Number of chat history messages to include (default 0)")
        .int()
        .default(0)
        .validate {
            require(it >= 0) { "Chat history must be greater than or equal to 0." }
        }

    override fun run() {
        OpenAiAdapter.INSTANCE.settings.logLevel = LogLevel.None
        MIN_LEVEL_TO_LOG = Level.WARNING
        val driver = createQaDriver(config)

        val status: (String) -> String = { "Asking a question about documents in ${ANSI_YELLOW}$it${ANSI_RESET}. Say '${ANSI_GREEN}bye${ANSI_RESET}' to exit, or '${ANSI_GREEN}switch x${ANSI_RESET}' to switch to a different folder." }
        val folderStatus: (String) -> String = { "You can use any of these folders: ${ANSI_CYAN}$it${ANSI_RESET}" }

        runBlocking {
            println("Using completion engine ${ANSI_CYAN}${driver.completionModel}${ANSI_RESET}")
            println("Using embedding service ${ANSI_CYAN}${driver.embeddingModel}${ANSI_RESET}")
            println(folderStatus(driver.folders.toString()))

            // initialize toolkit and view
            println(status(driver.folder))
            print("> ")
            var input = readln()
            while (input != "bye") {
                if (input.startsWith("switch ")) {
                    val folder = input.removePrefix("switch ")
                    try {
                        driver.folder = folder
                        println(status(driver.folder))
                    } catch (x: IllegalArgumentException) {
                        println("Invalid folder $folder. " + status(driver.folders.toString()))
                    }
                } else {
                    val result = driver.answerQuestion(input, historySize = chatHistory)
                    println(result.finalResult)
                }
                print("> ")
                input = readln()
            }
            println("Goodbye!")

            driver.close()
        }
    }

}

/** Command-line app for generating a response using a folder of documents. */
class DocumentQa: CliktCommand(name = "qa", help = "Ask a single question") {
    private val config by requireObject<DocumentQaConfig>()
    private val numResponses by option(help = "Number of responses to generate per question (default 1)")
        .int()
        .default(1)
        .validate {
            require(it > 0) { "Number of responses must be greater than 0." }
        }
    private val question by argument(help = "Question to ask about the documents")
        .validate {
            require(it.isNotBlank()) { "Question must not be blank." }
        }

    override fun run() {
        OpenAiAdapter.INSTANCE.settings.logLevel = LogLevel.None
        MIN_LEVEL_TO_LOG = Level.WARNING
        val driver = createQaDriver(config)

        info<DocumentQa>("  question: $question")
        val response = runBlocking {
            driver.answerQuestion(question, numResponses = numResponses)
        }
        val aggregatedResponses = response.finalResult.output?.outputs?.joinToString("\n\n") ?: ""
        println(aggregatedResponses)

        driver.close()
    }

}

/** Command-line app for working with embedding files for a folder of documents. */
class DocumentEmbeddings: CliktCommand(name = "embeddings", help = "Generate/update local embeddings file for a given folder") {
    private val config by requireObject<DocumentQaConfig>()
    private val reindexAll by option(help = "Reindex all documents in the folder")
        .flag(default = false)
    private val reindexNew by option(help = "Reindex new documents in the folder (default)")
        .flag(default = true)
    private val maxChunkSize by option(help = "Maximum chunk size (# of characters) for embeddings (default 1000)")
        .int()
        .default(1000)

    override fun run() {
        val docsFolder = config.docsFolder
        val embeddingModel = TextPlugin.embeddingModel(config.embeddingModel!!)
        val index = LocalFolderEmbeddingIndex(docsFolder, EmbeddingStrategy(embeddingModel, SmartTextChunker()))
        index.maxChunkSize = maxChunkSize
        runBlocking {
            if (reindexAll) {
                println("Reindexing all documents in $docsFolder...")
                index.reindexAll() // this triggers the reindex, and saves the library
            } else {
                println("Reindexing new documents in $docsFolder...")
                index.reindexNew() // this triggers the reindex, and saves the library
            }
            println("Reindexing complete.")
        }
        TextPlugin.orderedPlugins.forEach { it.close() }
    }
}

/** Command-line app for chunking documents into text, without generating embeddings. */
class DocumentChunker: CliktCommand(name = "chunk", help = "Chunk documents into smaller pieces") {
    private val config by requireObject<DocumentQaConfig>()
    private val reindexAll by option(help = "Reindex all documents in the folder")
        .flag(default = false)
    private val reindexNew by option(help = "Reindex new documents in the folder (default)")
        .flag(default = true)
    private val maxChunkSize by option(help = "Maximum chunk size (# of characters) for embeddings (default 1000)")
        .int()
        .default(1000)
    private val indexFile by option(help = "Index file name for the documents (default docs.json)")
        .default("docs.json")

    override fun run() {
        val docsFolder = config.docsFolder
        val indexFile = File(docsFolder, indexFile)

        println("${ANSI_CYAN}Refreshing file text in $docsFolder...$ANSI_RESET")
        val docs = LocalTextDocIndex(docsFolder, indexFile)
        docs.loadIndex()
        docs.processDocuments(reindexAll)

        println("${ANSI_CYAN}Chunking documents with max-chunk-size=$maxChunkSize...$ANSI_RESET")
        docs.processChunks(SmartTextChunker(), maxChunkSize,reindexAll)

        println("${ANSI_CYAN}Saving document set info...$ANSI_RESET")
        docs.saveIndex()

        println("${ANSI_CYAN}Processing complete.$ANSI_RESET")
        TextPlugin.orderedPlugins.forEach { it.close() }
    }
}

/** Shared config object for document QA. */
class DocumentQaConfig(
    val root: Path,
    val folder: String,
    val completionModel: String?,
    val embeddingModel: String?,
    val temp: Double?,
    val maxTokens: Int?,
    val templateId: String?
) {
    val docsFolder: File = if (folder == "") root.toFile() else File(root.toFile(), folder)
}

/** Creates driver from provided settings. */
fun createQaDriver(config: DocumentQaConfig) = LocalDocumentQaDriver(config.root.toFile()).apply {
    folder = config.folder

    info<DocumentQa>("Asking question about documents in $folder")
    if (config.completionModel != null) {
        try {
            completionModel = config.completionModel
        } catch (x: NoSuchElementException) {
            error("Completion model ${config.completionModel} not found.")
        }
    }
    info<DocumentQa>("  using completion engine $completionModel")
    if (config.embeddingModel != null) {
        try {
            embeddingModel = config.embeddingModel
        } catch (x: NoSuchElementException) {
            error("Embedding model ${config.embeddingModel} not found.")
        }
    }
    info<DocumentQa>("  using embedding model $embeddingModel")
    if (config.temp != null) {
        temp = config.temp
        info<DocumentQa>("  using temperature $temp")
    }
    if (config.maxTokens != null) {
        maxTokens = config.maxTokens
        info<DocumentQa>("  using max tokens $maxTokens")
    }
    if (config.templateId != null) {
        templateId = config.templateId
        info<DocumentQa>("  using template $templateId")
    }
    initialize()
}