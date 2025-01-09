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
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.defaultLazy
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.validate
import com.github.ajalt.clikt.parameters.types.path
import kotlinx.coroutines.runBlocking
import tri.ai.openai.OpenAiClient
import tri.ai.text.docs.LocalDocumentQaDriver
import tri.util.*
import java.nio.file.Path
import java.util.logging.Level
import kotlin.io.path.Path

fun main(args: Array<String>) =
    DocumentQaCli()
        .subcommands(DocumentQaChat(), DocumentQaScript())
        .main(args)

/** Base command for document QA. */
class DocumentQaCli: CliktCommand(name = "document-qa") {
    private val root by option(help = "Root path containing folders.")
        .path(mustExist = true)
        .default(Path("."))
        .validate {
            require(it.toFile().isDirectory) { "Root path must be a directory." }
            require(it.toFile().listFiles()?.any { it.isDirectory } == true) { "Root path must contain folders." }
        }
    private val folder by option(help = "Initial folder containing documents to search.")
        .defaultLazy {
            root.toFile().listFiles()!!.firstOrNull { it.isDirectory }!!.name.toString()
        }
    private val completionModel by option(help = "Completion model to use.")
    private val embeddingModel by option(help = "Embedding model to use.")

    override fun run() {
        currentContext.obj = DocumentQaConfig(root, folder, completionModel, embeddingModel)
    }
}

/** Standalone app for asking questions of documents. */
class DocumentQaChat : CliktCommand(name = "chat") {
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
                    val result = driver.answerQuestion(input)
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

/** Command-line app with parameters for asking questions of documents. */
class DocumentQaScript: CliktCommand(name = "question") {
    private val config by requireObject<DocumentQaConfig>()
    private val question by argument(help = "Question to ask about the documents.")

    override fun run() {
        OpenAiClient.INSTANCE.settings.logLevel = LogLevel.None
        MIN_LEVEL_TO_LOG = Level.WARNING
        val driver = createQaDriver(config)

        info<DocumentQaScript>("  question: $question")
        val response = runBlocking {
            driver.answerQuestion(question)
        }
        println(response.finalResult.firstValue)

        driver.close()
    }

}

/** Shared config object for document QA. */
class DocumentQaConfig(val root: Path, val folder: String?, val completionModel: String?, val embeddingModel: String?)

/** Creates driver from provided settings. */
fun createQaDriver(config: DocumentQaConfig) = LocalDocumentQaDriver(config.root.toFile()).apply {
    folder = config.folder ?: folders.first()
    info<DocumentQaScript>("Asking question about documents in $folder")
    if (config.completionModel != null) {
        completionModel = config.completionModel
    }
    info<DocumentQaScript>("  using completion engine $completionModel")
    if (config.embeddingModel != null) {
        embeddingModel = config.embeddingModel
    }
    info<DocumentQaScript>("  using embedding model $embeddingModel")
    initialize()
}