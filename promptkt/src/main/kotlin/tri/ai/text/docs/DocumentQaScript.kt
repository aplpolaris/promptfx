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
package tri.ai.text.docs

import com.aallam.openai.api.logging.LogLevel
import kotlinx.coroutines.runBlocking
import tri.ai.openai.OpenAiClient
import tri.util.*
import java.io.File
import java.util.logging.Level

/** Command-line app with parameters for asking questions of documents. */
object DocumentQaScript {

    private const val PARAM_FOLDER = "--folder"
    private const val PARAM_QUESTION = "--question"
    private const val PARAM_COMPLETION_MODEL = "--completion-model"
    private const val PARAM_EMBEDDING_MODEL = "--embedding-model"

    @JvmStatic
    fun main(args: Array<String>) {
        OpenAiClient.INSTANCE.settings.logLevel = LogLevel.None
        MIN_LEVEL_TO_LOG = Level.WARNING

        val joinArgs = args.joinToString(" ")
        val folder = joinArgs.parseArg(PARAM_FOLDER) { warning<DocumentQaScript>("Folder argument not found.") }
        val question = joinArgs.parseArg(PARAM_QUESTION) { warning<DocumentQaScript>("Question argument not found.") }
        val completionModel = joinArgs.parseArg(PARAM_COMPLETION_MODEL)
        val embeddingModel = joinArgs.parseArg(PARAM_EMBEDDING_MODEL)

        if (folder == null || question == null) {
            println(
                """
            $ANSI_GREEN
            Required arguments:
              --folder=<folder>
              --question=<question>
            Optional arguments:
              --completion-model=<modelId> (optional)
              --embedding-model=<modelId> (optional)
            $ANSI_RESET
        """.trimIndent()
            )
            return
        }

        runBlocking {
            val driver = createDriver(File(""), folder, completionModel, embeddingModel)
            info<DocumentQaScript>("  question: $question")
            val response = driver.answerQuestion(question)
            println(response.finalResult.firstValue)
            driver.close()
        }
    }

    //region PARSE ARGS

    private fun String.parseArg(arg: String, notFound: () -> Unit = { }): String? {
        val prefix1 = "\"$arg="
        if (prefix1 in this)
            return substringAfter(prefix1).substringBefore("\"")

        val prefix2 = "$arg="
        if (prefix2 !in this) {
            notFound()
            return null
        }
        val suffix = substringAfter(prefix2).trim()
        return if (suffix.startsWith("\""))
            suffix.substringAfter("\"").substringBefore("\"")
        else
            suffix.substringBefore(" ")
    }

    //endregion

    /** Creates driver from provided settings. */
    fun createDriver(root: File, folder: String?, completionModel: String?, embeddingModel: String?) = LocalDocumentQaDriver(root).apply {
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

}
