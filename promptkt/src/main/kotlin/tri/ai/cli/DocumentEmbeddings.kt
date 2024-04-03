/*-
 * #%L
 * tri.promptfx:promptkt
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
package tri.ai.cli

import kotlinx.coroutines.runBlocking
import tri.ai.embedding.LocalFolderEmbeddingIndex
import tri.ai.openai.OpenAiEmbeddingService
import tri.util.ANSI_GREEN
import tri.util.ANSI_RESET
import java.io.File
import kotlin.system.exitProcess

/** Runnable for chunking documents and calculating embeddings for chunks. */
object DocumentEmbeddings {

    @JvmStatic
    fun main(args: Array<String>) {
//        val args = arrayOf("D:\\data\\chatgpt\\doc-insight-test", "--reindex-all", "--max-chunk-size=1000")
        println("""
                $ANSI_GREEN
                Arguments expected:
                  <root folder> <options>
                Options:
                  --reindex-all
                  --reindex-new (default)
                  --max-chunk-size=<size> (default 1000)
                $ANSI_RESET
            """.trimIndent())

        if (args.isEmpty())
            exitProcess(0)

        val path = args[0]
        val reindexAll = args.contains("--reindex-all")
        val reindexNew = args.contains("--reindex-new") || !reindexAll
        val maxChunkSize = args.find { it.startsWith("--max-chunk-size") }?.substringAfter("=", "")?.toIntOrNull() ?: 1000

        val root = File(path)
        val embeddingService = OpenAiEmbeddingService()
        val index = LocalFolderEmbeddingIndex(root, embeddingService)
        index.maxChunkSize = maxChunkSize
        runBlocking {
            if (reindexNew) {
                println("Reindexing new documents in $root...")
                index.reindexNew() // this triggers the reindex, and saves the library
            } else if (reindexAll) {
                println("Reindexing all documents in $root...")
                index.reindexAll() // this triggers the reindex, and saves the library
            } else {
                TODO("Impossible to get here.")
            }
            println("Reindexing complete.")
        }
        exitProcess(0)
    }

}
