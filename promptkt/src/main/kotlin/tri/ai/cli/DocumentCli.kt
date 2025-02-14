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
import com.github.ajalt.clikt.parameters.options.*
import com.github.ajalt.clikt.parameters.types.int
import com.github.ajalt.clikt.parameters.types.path
import kotlinx.coroutines.runBlocking
import tri.ai.core.TextPlugin
import tri.ai.embedding.LocalFolderEmbeddingIndex
import tri.ai.openai.OpenAiClient
import tri.ai.openai.OpenAiEmbeddingService
import tri.ai.openai.OpenAiModelIndex
import tri.ai.text.chunks.process.LocalTextDocIndex
import tri.ai.text.chunks.process.SmartTextChunker
import tri.ai.text.docs.LocalDocumentQaDriver
import tri.util.*
import java.io.File
import java.nio.file.Path
import java.util.logging.Level
import kotlin.io.path.Path
import kotlin.system.exitProcess

fun main(args: Array<String>) =
    DocumentCli()
        .subcommands(DocumentChat(), DocumentChunker(), DocumentEmbeddings(), DocumentQa())
        .main(args)

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

    override fun run() {
        currentContext.obj = DocumentQaConfig(root, folder, model, embedding)
    }
}

/** Command-line app for asking questions of documents. */
class DocumentChat : CliktCommand(name = "chat", help = "Ask questions and switch between folders until done") {
    private val config by requireObject<DocumentQaConfig>()

    override fun run() {
        OpenAiClient.INSTANCE.settings.logLevel = LogLevel.None
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
                    val result = driver.answerQuestion(input) // TODO - add support for including chat history
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
    private val question by argument(help = "Question to ask about the documents")
        .validate {
            require(it.isNotBlank()) { "Question must not be blank." }
        }

    override fun run() {
        OpenAiClient.INSTANCE.settings.logLevel = LogLevel.None
        MIN_LEVEL_TO_LOG = Level.WARNING
        val driver = createQaDriver(config)

        info<DocumentQa>("  question: $question")
        val response = runBlocking {
            driver.answerQuestion(question)
        }
        println(response.finalResult.firstValue)

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
    private val maxChunkSize by option(help = "Maximum chunk size for embeddings")
        .int()
        .default(1000)

    override fun run() {
        val docsFolder = config.docsFolder
        val embeddingService = TextPlugin.embeddingModel(config.embeddingModel!!)
        val index = LocalFolderEmbeddingIndex(docsFolder, embeddingService)
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
    }
}

/** Command-line app for chunking documents into text, without generating embeddings. */
class DocumentChunker: CliktCommand(name = "chunk", help = "Chunk documents into smaller pieces") {
    private val config by requireObject<DocumentQaConfig>()
    private val reindexAll by option(help = "Reindex all documents in the folder")
        .flag(default = false)
    private val reindexNew by option(help = "Reindex new documents in the folder (default)")
        .flag(default = true)
    private val maxChunkSize by option(help = "Maximum chunk size for embeddings")
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
        val chunker = SmartTextChunker(maxChunkSize)
        docs.processChunks(chunker, reindexAll)

        println("${ANSI_CYAN}Saving document set info...$ANSI_RESET")
        docs.saveIndex()

        println("${ANSI_CYAN}Processing complete.$ANSI_RESET")
    }
}

/** Shared config object for document QA. */
class DocumentQaConfig(val root: Path, val folder: String, val completionModel: String?, val embeddingModel: String?) {
    val docsFolder: File = if (folder == "") root.toFile() else File(root.toFile(), folder)
}

/** Creates driver from provided settings. */
fun createQaDriver(config: DocumentQaConfig) = LocalDocumentQaDriver(config.root.toFile()).apply {
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
    initialize()
}