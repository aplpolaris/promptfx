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
import tri.util.MIN_LEVEL_TO_LOG
import java.io.File
import java.util.logging.Level

/** Standalone app for asking questions of documents. */
object DocumentQaChat {

    @JvmStatic
    fun main(args: Array<String>) {
        OpenAiClient.INSTANCE.settings.logLevel = LogLevel.None
        MIN_LEVEL_TO_LOG = Level.INFO

        val driver = DocumentQaScript.createDriver(File("."), null, null, null)

        runBlocking {
            println("Using completion engine ${driver.completionModel}")
            println("Using embedding service ${driver.embeddingModel}")
            println("You can use any of these folders: ${driver.folders}")

            // initialize toolkit and view
            println("Asking a question about documents in ${driver.folder}. Say 'bye' to exit, or 'switch x' to switch to a different folder.")
            print("> ")
            var input = readln()
            while (input != "bye") {
                if (input.startsWith("switch ")) {
                    val folder = input.removePrefix("switch ")
                    driver.folder = folder
                    if (driver.folder == folder) {
                        println("Asking a question about documents in ${driver.folder}. Say 'bye' to exit, or 'switch x' to switch to a different folder.")
                    } else {
                        println("Invalid folder $folder. You can use any of these folders: ${driver.folders}")
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
