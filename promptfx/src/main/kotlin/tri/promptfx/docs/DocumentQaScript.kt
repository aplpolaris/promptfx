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
package tri.promptfx.docs

import com.aallam.openai.api.logging.LogLevel
import javafx.application.Platform
import kotlinx.coroutines.runBlocking
import tri.ai.openai.OpenAiClient
import tri.ai.pips.AiPipelineExecutor
import tri.ai.pips.IgnoreMonitor
import tri.promptfx.PromptFxModels
import tri.promptfx.ui.FormattedPromptTraceResult
import tri.util.*
import java.io.File
import java.util.logging.Level

/** Command-line app with parameters for asking questions of documents. */
object DocumentQaScript {

    private const val PARAM_FOLDER = "--folder"
    private const val PARAM_QUESTION = "--question"
    private const val PARAM_COMPLETION_MODEL = "--completion-model"
    private const val PARAM_EMBEDDING_MODEL = "--embedding-model"

    private val platform by lazy { Platform.startup {  } }
    private fun initPlatform() = platform
    private val view by lazy {
        initPlatform()
        DocumentQaView()
    }

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
            val response = ask(folder, question, completionModel, embeddingModel)
            println(response)
        }

        Platform.exit()
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

    //region DocumentQaView ACCESSORS

    private fun DocumentQaView.getFolder() =
        documentFolder.get().name

    private fun DocumentQaView.setFolder(folder: String): Boolean {
        val folderFile = File(documentFolder.get().parentFile, folder)
        return if (folderFile.exists()) {
            documentFolder.set(folderFile)
            true
        } else {
            false
        }
    }

    //endregion

    /** Ask a question of a document folder. */
    fun ask(
        folder: String,
        question: String,
        modelId: String?,
        embeddingModelId: String?,
    ) = runBlocking {
        info<DocumentQaScript>("Asking question about $folder: $question")
        if (modelId != null) {
            info<DocumentQaScript>("Using completion engine ${view.controller.completionEngine.value}")
            view.controller.completionEngine.set(
                PromptFxModels.policy.textCompletionModels().find { it.modelId == modelId }!!
            )
        }
        if (embeddingModelId != null) {
            info<DocumentQaScript>("Using embedding model ${view.controller.embeddingService.value}")
            view.controller.embeddingService.set(
                PromptFxModels.policy.embeddingModels().find { it.modelId == embeddingModelId }!!
            )
        }
        view.setFolder(folder)
        view.question.set(question)
        val result = AiPipelineExecutor.execute(view.plan().plan(), IgnoreMonitor).finalResult as FormattedPromptTraceResult
        result.firstValue
    }

}
