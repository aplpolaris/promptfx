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

import tri.ai.text.chunks.process.LocalTextDocIndex
import tri.ai.text.chunks.process.StandardTextChunker
import tri.util.ANSI_CYAN
import tri.util.ANSI_GREEN
import tri.util.ANSI_RESET
import java.io.File
import kotlin.system.exitProcess

/** Runnable for extracting text from documents, and chunking documents into smaller pieces. */
object DocumentChunker {

    @JvmStatic
    fun main(args: Array<String>) {
        val args = arrayOf("D:\\data\\chatgpt\\doc-insight-test", "--reindex-new", "--max-chunk-size=5000")
        println(
            """
            $ANSI_GREEN
            Arguments expected:
              <root folder> <options>
            Options:
              --reindex-all
              --reindex-new (default)
              --max-chunk-size=<size> (default 1000)
              --index-file=<file> (default docs.json)
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
        val indexFile = File(rootFolder, args.find { it.startsWith("--index-file") }
            ?.substringAfter("=", "")
            ?: "docs.json")

        println("${ANSI_CYAN}Refreshing file text in $rootFolder...$ANSI_RESET")
        val docs = LocalTextDocIndex(rootFolder, indexFile)
        docs.loadIndex()
        docs.processDocuments(reindexAll)

        println("${ANSI_CYAN}Chunking documents with max-chunk-size=$maxChunkSize...$ANSI_RESET")
        val chunker = StandardTextChunker(maxChunkSize)
        docs.processChunks(chunker, reindexAll)

        println("${ANSI_CYAN}Saving document set info...$ANSI_RESET")
        docs.saveIndex()

        println("${ANSI_CYAN}Processing complete.$ANSI_RESET")
        exitProcess(0)
    }
}
