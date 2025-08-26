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
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File
import java.text.Normalizer

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

    @Test
    fun `test URI encoding with special characters in file path`(@TempDir tempDir: File) {
        // Create files with various special characters that could cause encoding issues
        val testFiles = listOf(
            "test – document.txt",  // En-dash (U+2013) encodes to %E2%80%93
            "file with spaces.txt", // Spaces encode to %20
            "file with ñ.txt",     // Non-ASCII character
            "file%20with%20encoded.txt" // Already encoded characters
        )

        testFiles.forEach { rawName ->
            val fileName = Normalizer.normalize(rawName, Normalizer.Form.NFC)
            val testFile = File(tempDir, fileName)
            testFile.writeText("Content of $fileName")

            // Create a TextDoc with the file's URI
            val originalUri = testFile.toURI()
            val textDoc = TextDoc(originalUri.toString(), testFile.readText()).apply {
                metadata.path = originalUri
            }

            // Create and save TextLibrary
            val library = TextLibrary().apply {
                docs.add(textDoc)
            }
            val indexFile = File(tempDir, "embeddings_${testFiles.indexOf(fileName)}.json")
            TextLibrary.saveTo(library, indexFile)

            // Load the library back - this should properly handle URI encoding
            val loadedLibrary = TextLibrary.loadFrom(indexFile)

            assertEquals(1, loadedLibrary.docs.size, "Failed for file: $fileName")
            val loadedDoc = loadedLibrary.docs.first()

            // The loaded document should have the same URI as the original
            assertEquals(originalUri.toASCIIString(), loadedDoc.metadata.path!!.toASCIIString(), "URI mismatch for file: $fileName")

            // The document should be able to find the original file
            assertNotNull(loadedDoc.all, "Document content is null for file: $fileName")
            assertEquals(testFile.readText(), loadedDoc.all?.text, "Content mismatch for file: $fileName")
        }
    }

}
