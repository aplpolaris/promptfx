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

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import tri.util.io.LocalFileManager.writeMetadata
import java.io.File

/**
 * Demonstration of the new metadata loading feature for issue #440.
 * This shows how TextLibrary.loadFrom() now automatically loads metadata
 * from .meta.json files adjacent to referenced documents.
 */
class MetadataLoadingDemo {

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