/*-
 * #%L
 * tri.promptfx:promptkt
 * %%
 * Copyright (C) 2023 - 2026 Johns Hopkins University Applied Physics Laboratory
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
import tri.util.io.LocalFileManager.writeMetadata
import java.io.File

class TextLibraryMetadataLoadingTest {

    @Test
    fun `test loading text library with metadata from meta json files`(@TempDir tempDir: File) {
        // Create a test text file
        val textFile = File(tempDir, "test-document.txt")
        textFile.writeText("This is the content of the test document.")

        // Create metadata for the file
        val metadata = mapOf(
            "title" to "Test Document Title",
            "author" to "Test Author",
            "subject" to "Test Subject",
            "keywords" to "test,document,sample",
            "customProperty" to "Custom Value"
        )

        // Write metadata to .meta.json file
        textFile.writeMetadata(metadata)

        // Verify the .meta.json file was created
        val metaFile = File(tempDir, "test-document.meta.json")
        assertTrue(metaFile.exists(), "Metadata file should exist")

        // Create a text library that references the text file
        val library = TextLibrary("test-library").apply {
            docs.add(TextDoc("test-doc").apply {
                this.metadata.path = textFile.toURI()
            })
        }

        // Save the library to embeddings.json
        val embeddingsFile = File(tempDir, "embeddings.json")
        TextLibrary.saveTo(library, embeddingsFile)

        // Load the library back
        val loadedLibrary = TextLibrary.loadFrom(embeddingsFile)

        // Verify the library was loaded correctly
        assertEquals(1, loadedLibrary.docs.size)
        val loadedDoc = loadedLibrary.docs.first()

        // Verify metadata was merged from .meta.json file
        assertEquals("Test Document Title", loadedDoc.metadata.title)
        assertEquals("Test Author", loadedDoc.metadata.author)
        assertEquals("Custom Value", loadedDoc.metadata.properties["customProperty"])
        assertEquals("Test Subject", loadedDoc.metadata.properties["subject"])
        assertEquals("test,document,sample", loadedDoc.metadata.properties["keywords"])

        // Verify the text content was loaded
        assertNotNull(loadedDoc.all)
        assertEquals("This is the content of the test document.", loadedDoc.all!!.text)
    }

    @Test
    fun `test loading text library without metadata files should still work`(@TempDir tempDir: File) {
        // Create a test text file without a .meta.json file
        val textFile = File(tempDir, "no-metadata-document.txt")
        textFile.writeText("This document has no metadata file.")

        // Create a text library that references the text file
        val library = TextLibrary("test-library-no-meta").apply {
            docs.add(TextDoc("no-meta-doc").apply {
                this.metadata.path = textFile.toURI()
                this.metadata.title = "Original Title"
                this.metadata.author = "Original Author"
            })
        }

        // Save the library to embeddings.json
        val embeddingsFile = File(tempDir, "embeddings-no-meta.json")
        TextLibrary.saveTo(library, embeddingsFile)

        // Load the library back
        val loadedLibrary = TextLibrary.loadFrom(embeddingsFile)

        // Verify the library was loaded correctly
        assertEquals(1, loadedLibrary.docs.size)
        val loadedDoc = loadedLibrary.docs.first()

        // Verify original metadata is preserved when no .meta.json file exists
        assertEquals("Original Title", loadedDoc.metadata.title)
        assertEquals("Original Author", loadedDoc.metadata.author)

        // Verify the text content was loaded
        assertNotNull(loadedDoc.all)
        assertEquals("This document has no metadata file.", loadedDoc.all!!.text)
    }

    @Test
    fun `test metadata from meta json file overrides embedded metadata`(@TempDir tempDir: File) {
        val textFile = File(tempDir, "override-test.txt")
        textFile.writeText("Document with metadata override test.")

        val metadataFromFile = mapOf(
            "title" to "Title from Meta File",
            "author" to "Author from Meta File",
            "newProperty" to "New Property Value"
        )
        textFile.writeMetadata(metadataFromFile)

        // Create a text library with different metadata embedded
        val library = TextLibrary("override-test-library").apply {
            docs.add(TextDoc("override-test-doc").apply {
                this.metadata.path = textFile.toURI()
                this.metadata.title = "Original Embedded Title"
                this.metadata.author = "Original Embedded Author"
                this.metadata.properties["originalProperty"] = "Original Property Value"
            })
        }
        val embeddingsFile = File(tempDir, "embeddings-override.json")
        TextLibrary.saveTo(library, embeddingsFile)
        val loadedLibrary = TextLibrary.loadFrom(embeddingsFile)

        // Verify the library was loaded correctly
        assertEquals(1, loadedLibrary.docs.size)
        val loadedDoc = loadedLibrary.docs.first()

        // Verify metadata merge from .meta.json file overrides embedded metadata, keeping old properties
        assertEquals("Title from Meta File", loadedDoc.metadata.title)
        assertEquals("Author from Meta File", loadedDoc.metadata.author)
        assertEquals("New Property Value", loadedDoc.metadata.properties["newProperty"])
        assertEquals("Original Property Value", loadedDoc.metadata.properties["originalProperty"])
    }

    @Test
    fun `test corrupted metadata file is gracefully handled`(@TempDir tempDir: File) {
        // Create a test text file
        val textFile = File(tempDir, "corrupted-metadata.txt")
        textFile.writeText("Document with corrupted metadata file.")

        // Create a corrupted .meta.json file
        val metaFile = File(tempDir, "corrupted-metadata.meta.json")
        metaFile.writeText("{ invalid json content }")

        // Create a text library that references the text file
        val library = TextLibrary("corrupted-meta-library").apply {
            docs.add(TextDoc("corrupted-meta-doc").apply {
                this.metadata.path = textFile.toURI()
                this.metadata.title = "Original Title"
                this.metadata.author = "Original Author"
            })
        }

        // Save the library to embeddings.json
        val embeddingsFile = File(tempDir, "embeddings-corrupted.json")
        TextLibrary.saveTo(library, embeddingsFile)

        // Load the library back - this should not fail despite corrupted metadata
        val loadedLibrary = TextLibrary.loadFrom(embeddingsFile)

        // Verify the library was loaded correctly
        assertEquals(1, loadedLibrary.docs.size)
        val loadedDoc = loadedLibrary.docs.first()

        // Verify original metadata is preserved when .meta.json is corrupted
        assertEquals("Original Title", loadedDoc.metadata.title)
        assertEquals("Original Author", loadedDoc.metadata.author)

        // Verify the text content was still loaded
        assertNotNull(loadedDoc.all)
        assertEquals("Document with corrupted metadata file.", loadedDoc.all!!.text)
    }

    @Test
    fun `test multiple documents with mixed metadata scenarios`(@TempDir tempDir: File) {
        // Create multiple test files with different metadata scenarios
        val file1 = File(tempDir, "doc1.txt")
        file1.writeText("Document 1 content")
        file1.writeMetadata(mapOf("title" to "Document 1 Title", "category" to "Category A"))

        val file2 = File(tempDir, "doc2.txt")
        file2.writeText("Document 2 content")
        // No metadata file for doc2

        val file3 = File(tempDir, "doc3.txt")
        file3.writeText("Document 3 content")
        file3.writeMetadata(mapOf("title" to "Document 3 Title", "author" to "Doc 3 Author"))

        // Create a text library with all three documents
        val library = TextLibrary("multi-doc-library").apply {
            docs.add(TextDoc("doc1").apply {
                this.metadata.path = file1.toURI()
                this.metadata.title = "Original Doc 1 Title"
            })
            docs.add(TextDoc("doc2").apply {
                this.metadata.path = file2.toURI()
                this.metadata.title = "Doc 2 Title"
                this.metadata.author = "Doc 2 Author"
            })
            docs.add(TextDoc("doc3").apply {
                this.metadata.path = file3.toURI()
            })
        }

        // Save the library
        val embeddingsFile = File(tempDir, "embeddings-multi.json")
        TextLibrary.saveTo(library, embeddingsFile)

        // Load the library back
        val loadedLibrary = TextLibrary.loadFrom(embeddingsFile)

        // Verify all documents were loaded
        assertEquals(3, loadedLibrary.docs.size)

        // Doc 1: should have metadata from file (overriding embedded)
        val doc1 = loadedLibrary.docs.find { it.metadata.id == "doc1" }!!
        assertEquals("Document 1 Title", doc1.metadata.title)
        assertEquals("Category A", doc1.metadata.properties["category"])

        // Doc 2: should keep embedded metadata (no meta file)
        val doc2 = loadedLibrary.docs.find { it.metadata.id == "doc2" }!!
        assertEquals("Doc 2 Title", doc2.metadata.title)
        assertEquals("Doc 2 Author", doc2.metadata.author)

        // Doc 3: should have metadata from file
        val doc3 = loadedLibrary.docs.find { it.metadata.id == "doc3" }!!
        assertEquals("Document 3 Title", doc3.metadata.title)
        assertEquals("Doc 3 Author", doc3.metadata.author)

        // Verify all text content was loaded
        assertEquals("Document 1 content", doc1.all!!.text)
        assertEquals("Document 2 content", doc2.all!!.text)
        assertEquals("Document 3 content", doc3.all!!.text)
    }

    @Test
    fun `demonstrate metadata loading from meta json files`(@TempDir tempDir: File) {
        println("=".repeat(80))
        println("DEMONSTRATION: Loading document metadata from .meta.json files")
        println("=".repeat(80))

        // Step 1: Create some documents with associated .meta.json files
        println("\n1. Creating test documents with .meta.json files:")

        val doc1 = File(tempDir, "research_paper.txt")
        doc1.writeText("Abstract: This research paper explores new frontiers in AI...")
        doc1.writeMetadata(mapOf(
            "title" to "Advanced AI Research: New Frontiers",
            "author" to "Dr. Jane Smith",
            "subject" to "Artificial Intelligence",
            "keywords" to "AI, machine learning, neural networks",
            "publicationYear" to 2024,
            "publisher" to "IEEE Conference Proceedings"
        ))

        val doc2 = File(tempDir, "user_manual.txt")
        doc2.writeText("Chapter 1: Getting Started with PromptFX...")
        doc2.writeMetadata(mapOf(
            "title" to "PromptFX User Manual v2.1",
            "author" to "Technical Documentation Team",
            "version" to "2.1.0",
            "department" to "Engineering",
            "lastReviewed" to "2024-08-15"
        ))

        val doc3 = File(tempDir, "meeting_notes.txt")
        doc3.writeText("Meeting Notes - Project Review\nDate: August 2024...")
        doc3.writeMetadata(mapOf(
            "title" to "Weekly Project Review Meeting",
            "author" to "Project Manager",
            "meetingDate" to "2024-08-20",
            "attendees" to listOf("Alice", "Bob", "Charlie"),
            "category" to "meeting-notes"
        ))

        // List the created files
        println("   Created files:")
        tempDir.listFiles()?.sorted()?.forEach { file ->
            println("     - ${file.name}")
        }

        // Step 2: Create and save a text library
        println("\n2. Creating text library referencing the documents:")

        val library = TextLibrary("demo-library").apply {
            metadata.path = "Documents from various sources"

            docs.add(TextDoc("doc1").apply {
                metadata.path = doc1.toURI()
                metadata.title = "Original Title 1"  // This will be overridden
                metadata.author = "Original Author 1"  // This will be overridden
            })

            docs.add(TextDoc("doc2").apply {
                metadata.path = doc2.toURI()
                metadata.title = "Original Title 2"  // This will be overridden
                metadata.author = "Original Author 2"  // This will be overridden
            })

            docs.add(TextDoc("doc3").apply {
                metadata.path = doc3.toURI()
                // No original metadata set
            })
        }

        val embeddingsFile = File(tempDir, "embeddings2.json")
        TextLibrary.saveTo(library, embeddingsFile)

        println("   Saved library to: ${embeddingsFile.name}")
        println("   Library contains ${library.docs.size} documents")

        // Step 3: Load the library back (this is where the magic happens!)
        println("\n3. Loading library back (metadata from .meta.json files will be merged):")

        val loadedLibrary = TextLibrary.loadFrom(embeddingsFile)

        println("   Loaded library: ${loadedLibrary.metadata.id}")
        println("   Documents loaded: ${loadedLibrary.docs.size}")

        // Step 4: Show the results
        println("\n4. RESULTS - Metadata successfully loaded from .meta.json files:")
        println()

        loadedLibrary.docs.forEachIndexed { index, doc ->
            println("   Document ${index + 1}: ${doc.metadata.id}")
            println("     Title: ${doc.metadata.title}")
            println("     Author: ${doc.metadata.author}")
            println("     Additional properties:")
            doc.metadata.properties.entries.sortedBy { it.key }.forEach { (key, value) ->
                println("       - $key: $value")
            }
            println("     Content preview: ${doc.all?.text?.take(50)}...")
            println()
        }

        println("=".repeat(80))
        println("SUMMARY:")
        println("- Metadata was automatically loaded from .meta.json files")
        println("- Original embedded metadata was replaced with metadata from files")
        println("- Additional properties (like keywords, publisher, etc.) were included")
        println("- Text content was loaded normally alongside the metadata")
        println("=".repeat(80))
    }

}
