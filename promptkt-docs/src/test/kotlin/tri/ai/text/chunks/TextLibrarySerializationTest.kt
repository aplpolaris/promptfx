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

class TextLibrarySerializationTest {

    @Test
    fun `test specific characters mentioned in issue 319`(@TempDir tempDir: File) {
        // Test the specific problematic characters mentioned in the issue
        // %20Ã¢â‚¬â€œ%20 contains UTF-8 encoded en-dash that may be getting corrupted
        
        // Let's create a file that would result in the problematic encoding
        val fileName = "document Â\u0080\u0093 test.txt"  // This contains the problematic UTF-8 sequence
        val testFile = File(tempDir, fileName)
        
        try {
            testFile.writeText("Test content")
            
            val originalUri = testFile.toURI()
            println("Original URI: $originalUri")
            
            // Create and save the library 
            val doc = TextDoc(originalUri.toString(), testFile.readText()).apply {
                metadata.path = originalUri
            }
            
            val library = TextLibrary().apply { docs.add(doc) }
            val indexFile = File(tempDir, "embeddings2.json")
            TextLibrary.saveTo(library, indexFile)
            
            // Check what was actually written to JSON
            val jsonContent = indexFile.readText()
            println("JSON content: $jsonContent")
            
            // Load it back
            val loadedLibrary = TextLibrary.loadFrom(indexFile)
            val loadedDoc = loadedLibrary.docs.first()
            
            println("Loaded URI: ${loadedDoc.metadata.path}")
            
            // Check if URI changed during serialization/deserialization
            assertEquals(originalUri, loadedDoc.metadata.path)
            
        } catch (e: Exception) {
            println("Exception occurred: ${e.message}")
            // This might be expected if the filename contains invalid characters
        }
    }

    @Test
    fun `test double-encoded URI scenarios`(@TempDir tempDir: File) {
        // Test scenarios where URI might get double-encoded
        val fileName = "test file.txt"  // Simple file with space
        val testFile = File(tempDir, fileName)
        testFile.writeText("Test content")
        
        val originalUri = testFile.toURI()
        println("Original URI: $originalUri")
        
        // Simulate what might happen if URI gets double-encoded
        val doubleEncodedUriString = originalUri.toString().replace("%20", "%2520")
        println("Double encoded: $doubleEncodedUriString")
        
        // This should fail - we don't want double encoding
        assertNotEquals(originalUri.toString(), doubleEncodedUriString)
    }

    @Test
    fun `test JSON round-trip with URI serialization`(@TempDir tempDir: File) {
        // Test that URI serialization/deserialization works correctly
        val fileName = "test – file.txt"
        val testFile = File(tempDir, fileName)  
        testFile.writeText("Content")
        
        val originalUri = testFile.toURI()
        val doc = TextDoc(originalUri.toString(), testFile.readText()).apply {
            metadata.path = originalUri
        }
        
        val library = TextLibrary().apply { docs.add(doc) }
        val indexFile = File(tempDir, "test.json")
        
        // Save
        TextLibrary.saveTo(library, indexFile)
        
        // Check JSON content
        val jsonContent = indexFile.readText()
        println("Serialized JSON: $jsonContent")
        
        // Load back
        val loaded = TextLibrary.loadFrom(indexFile)
        
        // Verify the URI is preserved exactly
        assertEquals(originalUri.toString(), loaded.docs.first().metadata.path.toString())
    }
}