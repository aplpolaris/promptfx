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
package tri.ai.embedding

import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File

class LocalFolderEmbeddingIndexUriConsistencyTest {

    @Test
    fun `test URI consistency prevents duplicate document processing`(@TempDir tempDir: File) {
        runTest {
            // Create a file with special characters
            val fileName = "document – test.txt"
            val testFile = File(tempDir, fileName)
            testFile.writeText("This is a test document with special characters.")

            val embeddingStrategy = EmbeddingStrategy(MockEmbeddingModel(), MockEmbeddingModel())
            val index = LocalFolderEmbeddingIndex(tempDir, embeddingStrategy)

            // First indexing - should add the document
            index.reindexNew()
            val docsAfterFirst = index.calculateAndGetDocs()
            assertEquals(1, docsAfterFirst.size, "Should have one document after first indexing")

            val firstDocUri = docsAfterFirst.first().metadata.path
            assertNotNull(firstDocUri, "Document should have a URI")

            // Second indexing - should NOT add the document again if URIs are consistent
            index.reindexNew()
            val docsAfterSecond = index.calculateAndGetDocs()
            assertEquals(1, docsAfterSecond.size, "Should still have one document after second indexing")

            val secondDocUri = docsAfterSecond.first().metadata.path
            assertEquals(firstDocUri, secondDocUri, "URI should be identical across indexing operations")

            // Verify the URI points to the same file
            val fileFromUri = File(firstDocUri!!)
            assertTrue(fileFromUri.exists(), "File should exist at URI location")
            assertEquals(testFile.readText(), fileFromUri.readText(), "Content should match")
        }
    }

    @Test
    fun `test embedded index save and reload maintains URI consistency`(@TempDir tempDir: File) {
        runTest {
            // Create multiple files with various special characters
            val fileNames = listOf(
                "simple.txt",
                "with spaces.txt",
                "with – dash.txt",
                "with ñ accent.txt"
            )

            fileNames.forEach { fileName ->
                File(tempDir, fileName).writeText("Content of $fileName")
            }

            val embeddingStrategy = EmbeddingStrategy(MockEmbeddingModel(), MockEmbeddingModel())
            val index = LocalFolderEmbeddingIndex(tempDir, embeddingStrategy)

            // Initial indexing
            index.reindexNew()
            val docsBeforeSave = index.calculateAndGetDocs()
            assertEquals(fileNames.size, docsBeforeSave.size)

            // Save the index
            index.saveIndex()

            // Create a new index instance (simulates restart)
            val newIndex = LocalFolderEmbeddingIndex(tempDir, embeddingStrategy)
            val docsAfterLoad = newIndex.calculateAndGetDocs() // This loads from saved index
            assertEquals(fileNames.size, docsAfterLoad.size)

            // URIs should be identical before and after save/load
            val urisBeforeSave = docsBeforeSave.map { it.metadata.path }.toSet()
            val urisAfterLoad = docsAfterLoad.map { it.metadata.path }.toSet()

            assertEquals(urisBeforeSave, urisAfterLoad, "URIs should be identical after save/load cycle")

            // Re-index after load - should not create duplicates
            newIndex.reindexNew()
            val docsAfterReindex = newIndex.calculateAndGetDocs()
            assertEquals(fileNames.size, docsAfterReindex.size, "Should not create duplicate documents")
        }
    }

    @Test
    fun `test path fixing does not break URI consistency`(@TempDir tempDir: File) {
        runTest {
            // This test simulates the scenario where files might be moved/copied
            val fileName = "test – document.txt"
            val originalFile = File(tempDir, fileName)
            originalFile.writeText("Original content")

            val embeddingStrategy = EmbeddingStrategy(MockEmbeddingModel(), MockEmbeddingModel())
            val index = LocalFolderEmbeddingIndex(tempDir, embeddingStrategy)

            // Index initially
            index.reindexNew()
            val originalDocs = index.calculateAndGetDocs()
            assertEquals(1, originalDocs.size)

            val originalUri = originalDocs.first().metadata.path

            // Save index
            index.saveIndex()

            // The key test: create new index and load from existing index file
            // This triggers the path fixing logic in TextLibrary.loadFrom()
            val newIndex = LocalFolderEmbeddingIndex(tempDir, embeddingStrategy)
            val loadedDocs = newIndex.calculateAndGetDocs()

            // URI should remain the same even after path fixing
            val loadedUri = loadedDocs.first().metadata.path
            assertEquals(originalUri, loadedUri, "Path fixing should not change URI when file exists")

            // Subsequent reindex should not create duplicates
            newIndex.reindexNew()
            val reindexedDocs = newIndex.calculateAndGetDocs()
            assertEquals(1, reindexedDocs.size, "Should not create duplicates after path fixing")
        }
    }
}
