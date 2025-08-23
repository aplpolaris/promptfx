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
package tri.util.io

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import tri.util.io.LocalFileManager.metadataFile
import tri.util.io.LocalFileManager.readMetadata
import tri.util.io.LocalFileManager.writeMetadata
import java.io.File

class LocalFileManagerMetadataTest {

    @Test
    fun `test writeMetadata and readMetadata round trip`(@TempDir tempDir: File) {
        val testFile = File(tempDir, "test.txt")
        testFile.writeText("Test content")

        val originalMetadata = mapOf(
            "title" to "Test Title",
            "author" to "Test Author",
            "number" to 42,
            "active" to true,
            "tags" to listOf("tag1", "tag2", "tag3")
        )

        // Write metadata
        testFile.writeMetadata(originalMetadata)

        // Verify metadata file was created
        val metaFile = File(tempDir, "test.meta.json")
        assertTrue(metaFile.exists())

        // Read metadata back
        val readMetadata = testFile.readMetadata()

        // Verify all values are preserved
        assertEquals("Test Title", readMetadata["title"])
        assertEquals("Test Author", readMetadata["author"])
        assertEquals(42, readMetadata["number"])
        assertEquals(true, readMetadata["active"])
        assertEquals(listOf("tag1", "tag2", "tag3"), readMetadata["tags"])
    }

    @Test
    fun `test readMetadata returns empty map when meta file does not exist`(@TempDir tempDir: File) {
        val testFile = File(tempDir, "no-meta.txt")
        testFile.writeText("Test content")

        // No metadata file exists
        val metaFile = File(tempDir, "no-meta.meta.json")
        assertFalse(metaFile.exists())

        // Should return empty map
        val readMetadata = testFile.readMetadata()
        assertTrue(readMetadata.isEmpty())
    }

    @Test
    fun `test readMetadata handles corrupted json gracefully`(@TempDir tempDir: File) {
        val testFile = File(tempDir, "corrupted.txt")
        testFile.writeText("Test content")

        // Create corrupted metadata file
        val metaFile = File(tempDir, "corrupted.meta.json")
        metaFile.writeText("{ invalid json content without proper closing")

        // Should return empty map when JSON is corrupted
        val readMetadata = testFile.readMetadata()
        assertTrue(readMetadata.isEmpty())
    }

    @Test
    fun `test readMetadata handles empty json file`(@TempDir tempDir: File) {
        val testFile = File(tempDir, "empty.txt")
        testFile.writeText("Test content")

        // Create empty metadata file
        val metaFile = File(tempDir, "empty.meta.json")
        metaFile.writeText("{}")

        // Should return empty map
        val readMetadata = testFile.readMetadata()
        assertTrue(readMetadata.isEmpty())
    }

    @Test
    fun `test metadataFile function naming convention`(@TempDir tempDir: File) {
        // Test with different file extensions
        val txtFile = File(tempDir, "document.txt")
        assertEquals("document.meta.json", txtFile.metadataFile().name)

        val pdfFile = File(tempDir, "document.pdf")
        assertEquals("document.meta.json", pdfFile.metadataFile().name)

        val docxFile = File(tempDir, "document.docx")
        assertEquals("document.meta.json", docxFile.metadataFile().name)

        // Test with already .meta.json file
        val metaFile = File(tempDir, "document.meta.json")
        assertEquals("document.meta.json", metaFile.metadataFile().name)

        // Test with complex filename
        val complexFile = File(tempDir, "my-document_v2.final.txt")
        assertEquals("my-document_v2.final.meta.json", complexFile.metadataFile().name)
    }
}