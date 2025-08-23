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
import tri.util.io.LocalFileManager.metadataFile
import tri.util.io.LocalFileManager.writeMetadata
import tri.util.io.WebCrawler.toMetadata
import java.io.File
import java.nio.file.Path

class WebCrawlerTest {

    @Test
    fun testMetadataSerializationFormat(@TempDir tempDir: Path) {
        // Test that we can create and serialize metadata
        val metadata = mapOf(
            "web.url" to "https://example.com/test",
            "web.title" to "Test Page",
            "web.isArticle" to true,
            "web.links" to listOf("https://example.com/link1", "https://example.com/link2"),
            "web.textLength" to 1234
        )
        
        // Write metadata to file
        val metadataFile = tempDir.resolve("test.metadata.json").toFile()
        WebCrawler.JSON_MAPPER.writerWithDefaultPrettyPrinter()
            .writeValue(metadataFile, metadata)
        
        // Verify file was created
        assertTrue(metadataFile.exists())
        println("Generated metadata JSON:")
        println(metadataFile.readText())
        
        // Read it back and verify content
        val readMetadata = WebCrawler.JSON_MAPPER.readValue<Map<String, Any>>(metadataFile)
        assertEquals("https://example.com/test", readMetadata["web.url"])
        assertEquals("Test Page", readMetadata["web.title"])
        assertTrue(readMetadata["web.isArticle"] as Boolean)
        assertEquals(2, (readMetadata["web.links"] as List<*>).size)
        assertEquals(1234, readMetadata["web.textLength"])
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
        val metadata = content.toMetadata()
        
        // Verify the metadata
        assertEquals(url, metadata["web.url"])
        assertEquals(title, metadata["web.title"])
        assertTrue(metadata["web.isArticle"] as Boolean)
        assertEquals(links, metadata["web.links"])
        assertEquals(text.length, metadata["web.length"])
        assertNotNull(metadata["web.scrapedAt"])
        assertNotNull(metadata["file.modificationDate"])
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
        val metadataFileName = baseFileName.substringBeforeLast(".txt") + ".meta.json"

        // Create the files
        val textFile = tempDir.resolve(baseFileName).toFile()
        textFile.writeText(content.text)
        val metadataFile = tempDir.resolve(metadataFileName).toFile()
        WebCrawler.JSON_MAPPER.writerWithDefaultPrettyPrinter()
            .writeValue(metadataFile, content.toMetadata())

        // Verify both files exist and have expected names
        assertTrue(textFile.exists())
        assertTrue(metadataFile.exists())
        assertEquals("My_Special_Article.txt", textFile.name)
        assertEquals("My_Special_Article.meta.json", metadataFile.name)
        
        // Verify content
        assertEquals(content.text, textFile.readText())
        val readMetadata = WebCrawler.JSON_MAPPER.readValue<Map<String, Any>>(metadataFile)
        assertEquals(content.url, readMetadata["web.url"])
    }
    
    @Test  
    fun testIntegrationWithMockData(@TempDir tempDir: Path) {
        // Test the complete workflow with mock crawl results
        val mockCrawlResult = mapOf(
            "https://example.com/page1" to WebCrawlContent(
                url = "https://example.com/page1",
                docNode = org.jsoup.Jsoup.parse("<html><head><title>Test Page 1</title></head><body><article><p>First test page</p><a href='https://example.com/related1'>Related Link</a></article></body></html>"),
                title = "Test Page 1", 
                textElement = org.jsoup.Jsoup.parse("<article><p>First test page</p></article>").select("article").first()!!,
                textArticle = true,
                text = "First test page content with some links",
                links = listOf("https://example.com/related1", "https://example.com/related2")
            ),
            "https://example.com/page2" to WebCrawlContent(
                url = "https://example.com/page2", 
                docNode = org.jsoup.Jsoup.parse("<html><head><title>Test Page 2</title></head><body><div><p>Second test page</p></div></body></html>"),
                title = "Test Page 2",
                textElement = org.jsoup.Jsoup.parse("<div><p>Second test page</p></div>").select("div").first()!!,
                textArticle = false,
                text = "Second test page content without article structure",
                links = listOf("https://example.com/external")
            )
        )
        
        // Simulate the file writing logic from crawlWebsite method
        mockCrawlResult.forEach { (_, content) ->
            val baseFileName = content.title.replace("[^a-zA-Z0-9.-]".toRegex(), "_") + ".txt"

            val file = File(tempDir.toFile(), baseFileName)
            file.writeText(content.text)
            file.writeMetadata(content.toMetadata())
        }
        
        // Verify all expected files were created
        val files = tempDir.toFile().listFiles()!!.sortedBy { it.name }
        println(files.joinToString("\n"))
        assertEquals(4, files.size)  // 2 text files + 2 metadata files
        
        // Verify text files
        val textFiles = files.filter { it.name.endsWith(".txt") }
        assertEquals(2, textFiles.size)
        assertTrue(textFiles.any { it.name == "Test_Page_1.txt" })
        assertTrue(textFiles.any { it.name == "Test_Page_2.txt" })
        
        // Verify metadata files  
        val metadataFiles = files.filter { it.name.endsWith(".meta.json") }
        assertEquals(2, metadataFiles.size)
        assertTrue(metadataFiles.any { it.name == "Test_Page_1.meta.json" })
        assertTrue(metadataFiles.any { it.name == "Test_Page_2.meta.json" })
        
        // Verify content of one metadata file
        val page1Metadata = WebCrawler.JSON_MAPPER.readValue<Map<String, Any>>(
            metadataFiles.first { it.name == "Test_Page_1.meta.json" }
        )
        assertEquals("https://example.com/page1", page1Metadata["web.url"])
        assertEquals("Test Page 1", page1Metadata["web.title"])
        assertTrue(page1Metadata["web.isArticle"] as Boolean)
        assertEquals(2, (page1Metadata["web.links"] as List<*>).size)
        assertEquals(39, page1Metadata["web.length"])  // Length of "First test page content with some links"
        assertNotNull(page1Metadata["web.scrapedAt"])
        assertNotNull(page1Metadata["file.modificationDate"])

        println("Integration test created files:")
        files.forEach { println("- ${it.name}") }
    }
}