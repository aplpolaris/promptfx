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
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.required
import com.github.ajalt.clikt.parameters.types.path
import kotlinx.coroutines.runBlocking
import tri.ai.openai.OpenAiClient
import tri.ai.text.docs.LocalDocumentQaDriver
import tri.util.*
import java.io.File
import java.util.logging.Level
import kotlin.io.path.Path

/** Command-line app with parameters for asking questions of documents. */
class DocumentQaScript: CliktCommand() {

    private val root by option(help = "Root path containing folders.")
        .path(mustExist = true)
        .default(Path(""))
    private val folder by option(help = "Folder containing documents to search.").required()
    private val completionModel by option(help = "Completion model to use.")
    private val embeddingModel by option(help = "Embedding model to use.")

    private val question by argument(help = "Question to ask about the documents.")

    override fun run() {
        OpenAiClient.INSTANCE.settings.logLevel = LogLevel.None
        MIN_LEVEL_TO_LOG = Level.WARNING
        val driver = createQaDriver(root.toFile(), folder, completionModel, embeddingModel)

        info<DocumentQaScript>("  question: $question")
        val response = runBlocking {
            driver.answerQuestion(question)
        }
        println(response.finalResult.firstValue)

        driver.close()
    }

}

fun main(args: Array<String>) = DocumentQaScript().main(args)

/** Creates driver from provided settings. */
fun createQaDriver(root: File, folder: String?, completionModel: String?, embeddingModel: String?) = LocalDocumentQaDriver(root).apply {
    if (folder != null) {
        this.folder = folder
    } else {
        this.folder = folders.first()
    }
    info<DocumentQaScript>("Asking question about documents in ${this.folder}")
    if (completionModel != null) {
        this.completionModel = completionModel
    }
    info<DocumentQaScript>("  using completion engine ${this.completionModel}")
    if (embeddingModel != null) {
        this.embeddingModel = embeddingModel
    }
    info<DocumentQaScript>("  using embedding model ${this.embeddingModel}")
    initialize()
}