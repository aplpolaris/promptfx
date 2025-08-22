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

import com.fasterxml.jackson.module.kotlin.readValue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.io.TempDir
import java.io.File
import java.nio.file.Path

class WebCrawlerTest {

    @Test
    fun testMetadataSerializationFormat(@TempDir tempDir: Path) {
        // Test that we can create and serialize metadata
        val metadata = WebScrapedDocumentMetadata(
            url = "https://example.com/test",
            title = "Test Page",
            isArticle = true,
            links = listOf("https://example.com/link1", "https://example.com/link2"),
            textLength = 1234
        )
        
        // Write metadata to file
        val metadataFile = tempDir.resolve("test.metadata.json").toFile()
        WebCrawler.JSON_MAPPER.writerWithDefaultPrettyPrinter()
            .writeValue(metadataFile, metadata)
        
        // Verify file was created
        assertTrue(metadataFile.exists())
        
        // Read it back and verify content
        val readMetadata = WebCrawler.JSON_MAPPER.readValue<WebScrapedDocumentMetadata>(metadataFile)
        assertEquals("https://example.com/test", readMetadata.url)
        assertEquals("Test Page", readMetadata.title)
        assertTrue(readMetadata.isArticle)
        assertEquals(2, readMetadata.links.size)
        assertEquals(1234, readMetadata.textLength)
        assertEquals(2, readMetadata.linkCount)
        assertNotNull(readMetadata.scrapedAt)
        
        println("Generated metadata JSON:")
        println(metadataFile.readText())
    }
    
    @Test
    fun testWebCrawlContentToMetadata() {
        // Test the conversion from WebCrawlContent to metadata
        val url = "https://example.com/test-article"
        val title = "Example Article"
        val text = "This is some example article text that demonstrates the functionality."
        val links = listOf("https://example.com/related1", "https://example.com/related2")
        
        // Create a WebCrawlContent object (this would normally be created by scrapeText)
        val mockDocument = org.jsoup.Jsoup.parse("<html><head><title>$title</title></head><body><article>$text</article></body></html>")
        val content = WebCrawlContent(
            url = url,
            docNode = mockDocument,
            title = title,
            textElement = mockDocument.body(),
            textArticle = true,
            text = text,
            links = links
        )
        
        // Convert to metadata
        val metadata = WebScrapedDocumentMetadata(
            url = content.url,
            title = content.title,
            isArticle = content.textArticle,
            links = content.links,
            textLength = content.text.length
        )
        
        // Verify the metadata
        assertEquals(url, metadata.url)
        assertEquals(title, metadata.title)
        assertTrue(metadata.isArticle)
        assertEquals(links.size, metadata.linkCount)
        assertEquals(text.length, metadata.textLength)
        assertEquals(links, metadata.links)
    }
    
    @Test
    fun testFileNamingConvention(@TempDir tempDir: Path) {
        // Test that the file naming convention works correctly for both text and metadata files
        val content = WebCrawlContent(
            url = "https://example.com/my-special-article",
            docNode = org.jsoup.Jsoup.parse("<html><head><title>My Special Article</title></head><body>Content</body></html>"),
            title = "My Special Article",
            textElement = org.jsoup.Jsoup.parse("<body>Content</body>").body(),
            textArticle = true,
            text = "This is the article content",
            links = emptyList()
        )
        
        // Simulate the file creation process
        val baseFileName = content.title.replace("[^a-zA-Z0-9.-]".toRegex(), "_") + ".txt"
        val metadataFileName = baseFileName.substringBeforeLast(".txt") + ".metadata.json"
        
        // Create the files
        val textFile = tempDir.resolve(baseFileName).toFile()
        val metadataFile = tempDir.resolve(metadataFileName).toFile()
        
        textFile.writeText(content.text)
        
        val metadata = WebScrapedDocumentMetadata(
            url = content.url,
            title = content.title,
            isArticle = content.textArticle,
            links = content.links,
            textLength = content.text.length
        )
        WebCrawler.JSON_MAPPER.writerWithDefaultPrettyPrinter()
            .writeValue(metadataFile, metadata)
        
        // Verify both files exist and have expected names
        assertTrue(textFile.exists())
        assertTrue(metadataFile.exists())
        assertEquals("My_Special_Article.txt", textFile.name)
        assertEquals("My_Special_Article.metadata.json", metadataFile.name)
        
        // Verify content
        assertEquals(content.text, textFile.readText())
        val readMetadata = WebCrawler.JSON_MAPPER.readValue<WebScrapedDocumentMetadata>(metadataFile)
        assertEquals(content.url, readMetadata.url)
    }
}