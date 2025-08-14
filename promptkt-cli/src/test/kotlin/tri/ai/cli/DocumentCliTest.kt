/*-
 * #%L
 * tri.promptfx:promptkt
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

import com.github.ajalt.clikt.core.subcommands
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import kotlin.io.path.Path
import kotlin.io.path.readText

class DocumentCliTest {

    private fun main(args: Array<String>) =
        DocumentCli()
            .subcommands(DocumentChat(), DocumentChunker(), DocumentEmbeddings(), DocumentQa())
            .main(args)

    private val path = Path("C:\\data\\chatgpt")
    private val folder = Path("test3")

    @Test
    @Disabled
    fun testChunk() {
        main(arrayOf(
            "--root=$path",
            "chunk",
            "--reindex-all"
        ))
        println(path.resolve("docs.json").readText())
    }

    @Test
    @Disabled
    fun testEmbeddings() {
        main(arrayOf(
            "--root=$path",
            "embeddings",
            "--reindex-all"
        ))
    }

    @Test
    @Disabled
    fun testQa() {
        main(arrayOf(
            "--root=$path",
            "--folder=$folder",
            "qa",
            "What is Llama?"
        ))
    }
}
