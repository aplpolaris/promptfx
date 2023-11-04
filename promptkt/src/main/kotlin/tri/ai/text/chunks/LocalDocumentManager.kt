/*-
 * #%L
 * promptkt-0.1.12-SNAPSHOT
 * %%
 * Copyright (C) 2023 Johns Hopkins University Applied Physics Laboratory
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
package tri.ai.text.chunks

import tri.util.ANSI_CYAN
import tri.util.ANSI_GREEN
import tri.util.ANSI_RESET
import java.io.File
import kotlin.system.exitProcess

object LocalDocumentManager {
    /** Runnable for working with document sets. */
    @JvmStatic
    fun main(args: Array<String>) {
        val args = arrayOf("D:\\data\\chatgpt\\doc-insight-test", "--reindex-all", "--max-chunk-size=5000")
        println(
            """
            $ANSI_GREEN
            Arguments expected:
              <root folder> <options>
            Options:
              --reindex-all
              --reindex-new (default)
              --max-chunk-size=<size> (default 1000)
            $ANSI_RESET
        """.trimIndent()
        )

        if (args.isEmpty())
            exitProcess(0)

        val path = args[0]
        val reindexAll = args.contains("--reindex-all")
        val maxChunkSize = args.find { it.startsWith("--max-chunk-size") }
            ?.substringAfter("=", "")
            ?.toIntOrNull() ?: 1000
        val rootFolder = File(path)

        println("${ANSI_CYAN}Refreshing file text in $rootFolder...$ANSI_RESET")
        val docs = LocalTextDocumentSet(rootFolder)
        docs.loadIndex()
        docs.processDocuments(reindexAll)

        println("${ANSI_CYAN}Chunking documents with max-chunk-size=$maxChunkSize...$ANSI_RESET")
        val chunker = StandardTextChunker(maxChunkSize)
        val updatedChunks = docs.processChunks(chunker, reindexAll)
        updatedChunks.forEach {
            when (it) {
                is TextSection -> println("  ${it.first} ${it.last} ${it.text.firstFiftyChars()}")
                else -> println("  ${it.text.firstFiftyChars()}")
            }
        }

        println("${ANSI_CYAN}Saving document set info...$ANSI_RESET")
        docs.saveIndex()

        println("${ANSI_CYAN}Processing complete.$ANSI_RESET")
        exitProcess(0)
    }

    private fun String.firstFiftyChars() = replace("[\r\n]+".toRegex(), " ").let {
        if (it.length <= 50) it.trim() else (it.substring(0, 50).trim()+"...")
    }
}
