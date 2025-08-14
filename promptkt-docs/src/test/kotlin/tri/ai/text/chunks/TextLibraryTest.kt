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
package tri.ai.text.chunks

import com.fasterxml.jackson.module.kotlin.readValue
import org.junit.jupiter.api.Test

class TextLibraryTest {

    @Test
    fun testTextLibrary() {
        val lib = TextLibrary("test library").apply {
            docs.add(TextDoc("test book").apply {
                chunks.add(TextChunkRaw("this is a raw string"))
            })
            val raw = TextChunkRaw("this is all the content in this book")
            docs.add(TextDoc("test book 2", raw).apply {
                chunks.add(TextChunkInDoc(0..20))
                chunks.add(TextChunkInDoc(20..35))
            })
        }
        val str = TextLibrary.MAPPER
            .writerWithDefaultPrettyPrinter()
            .writeValueAsString(lib)
        println(str)
        val lib2 = TextLibrary.MAPPER.readValue<TextLibrary>(str)
        println(TextLibrary.MAPPER.writerWithDefaultPrettyPrinter().writeValueAsString(lib2))
    }

}
