/*-
 * #%L
 * tri.promptfx:promptfx
 * %%
 * Copyright (C) 2023 - 2024 Johns Hopkins University Applied Physics Laboratory
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
import tri.promptfx.ui.FormattedPromptTraceResult
import java.io.File
import java.io.FileFilter

/** Standalone app for asking questions of documents. */
object DocumentQaRunner {

    private val platform by lazy { Platform.startup {  } }
    private val view by lazy {
        initPlatform()
        DocumentQaView()
    }

    val folders by lazy { view.getFolders() }

    private fun initPlatform() = platform

    fun ask(input: String, folder: String?): String {
        return runBlocking {
            // initialize toolkit and view
            if (folder != null) {
                view.setFolder(folder)
            }
            println("Asking a question about documents in ${view.getFolder()}.")
            view.question.set(input)
            val result = AiPipelineExecutor.execute(view.plan().plan(), IgnoreMonitor).finalResult as FormattedPromptTraceResult
            result.firstValue
        }
    }

    private fun DocumentQaView.getFolder() =
        documentFolder.get().name

    private fun DocumentQaView.getFolders() =
        documentFolder.get().parentFile
            .listFiles(FileFilter { it.isDirectory })!!
            .map { it.name }

    private fun DocumentQaView.setFolder(folder: String): Boolean {
        val folderFile = File(documentFolder.get().parentFile, folder)
        return if (folderFile.exists()) {
            documentFolder.set(folderFile)
            true
        } else {
            false
        }
    }

    @JvmStatic
    fun main(args: Array<String>) {
        OpenAiClient.INSTANCE.settings.logLevel = LogLevel.None

        runBlocking {
            println("Using completion engine ${view.controller.completionEngine.value}")
            println("You can use any of these folders: ${view.getFolders()}")

            // initialize toolkit and view
            println("Asking a question about documents in ${view.getFolder()}. Say 'bye' to exit, or 'switch x' to switch to a different folder.")
            print("> ")
            var input = readln()
            while (input != "bye") {
                if (input.startsWith("switch ")) {
                    val folder = input.removePrefix("switch ")
                    if (view.setFolder(folder)) {
                        println("Asking a question about documents in ${view.getFolder()}. Say 'bye' to exit, or 'switch x' to switch to a different folder.")
                    } else {
                        println("Invalid folder $folder. You can use any of these folders: ${view.getFolders()}")
                    }
                } else {
                    view.question.set(input)
                    val result = AiPipelineExecutor.execute(view.plan().plan(), IgnoreMonitor)
                    println(result.finalResult)
                }
                print("> ")
                input = readln()
            }
            println("Goodbye!")

            Platform.exit()
        }
    }
}
