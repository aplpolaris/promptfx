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

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File
import java.net.URI

class TextLibraryUriEncodingTest {

    @Test
    fun `test URI encoding with special characters in file path`(@TempDir tempDir: File) {
        // Create files with various special characters that could cause encoding issues
        val testFiles = listOf(
            "test – document.txt",  // En-dash (U+2013) encodes to %E2%80%93
            "file with spaces.txt", // Spaces encode to %20
            "file with ñ.txt",     // Non-ASCII character
            "file%20with%20encoded.txt" // Already encoded characters
        )
        
        testFiles.forEach { fileName ->
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
            assertEquals(originalUri, loadedDoc.metadata.path, "URI mismatch for file: $fileName")
            
            // The document should be able to find the original file
            assertNotNull(loadedDoc.all, "Document content is null for file: $fileName")
            assertEquals(testFile.readText(), loadedDoc.all?.text, "Content mismatch for file: $fileName")
        }
    }

    @Test
    fun `test File constructor with URI object vs URI string`(@TempDir tempDir: File) {
        // Create a file with special characters
        val fileName = "test – document.txt"
        val testFile = File(tempDir, fileName)
        testFile.writeText("Test content")
        
        val uri = testFile.toURI()
        
        // This is the problematic pattern currently in the code
        val fileFromUriObject = File(uri.toString())  // This is wrong - treats encoded string as literal path
        
        // This is the correct way
        val fileFromUri = File(uri)  // This properly handles URI decoding
        
        // The correct approach should find the actual file
        assertTrue(fileFromUri.exists(), "File created from URI object should exist")
        
        // The incorrect approach might not find the file if there are special characters
        if (uri.toString().contains("%")) {
            assertFalse(fileFromUriObject.exists(), "File created from URI string should not exist when path contains encoded characters")
        }
    }

    @Test 
    fun `test reproduce issue 319 - multiple representations of same document`(@TempDir tempDir: File) {
        // This test reproduces the exact scenario described in issue #319
        // where the same document gets multiple internal representations
        
        // Create a file with the problematic characters mentioned in the issue
        val fileName = "document – test.txt"  // Contains en-dash that gets encoded
        val testFile = File(tempDir, fileName)
        testFile.writeText("Test document with special characters")
        
        // Simulate what happens in LocalFolderEmbeddingIndex
        val uri1 = testFile.toURI()  // First representation
        println("URI1: $uri1")
        
        // Create TextDoc and save to library
        val doc1 = TextDoc(uri1.toString(), testFile.readText()).apply {
            metadata.path = uri1
        }
        
        val library = TextLibrary().apply { docs.add(doc1) }
        val indexFile = File(tempDir, "embeddings2.json")
        TextLibrary.saveTo(library, indexFile)
        
        // Now load it back - this may create a different representation
        val loadedLibrary = TextLibrary.loadFrom(indexFile)
        val loadedDoc = loadedLibrary.docs.first()
        
        println("Original URI: ${doc1.metadata.path}")
        println("Loaded URI: ${loadedDoc.metadata.path}")
        
        // These should be equal - if they're not, that's the bug
        assertEquals(doc1.metadata.path, loadedDoc.metadata.path, 
                    "URI representations should be identical after save/load cycle")
        
        // Both should point to the same file
        assertNotNull(loadedDoc.all)
        assertEquals(testFile.readText(), loadedDoc.all?.text)
    }
}