/*-
 * #%L
 * tri.promptfx:promptfx
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
package tri.ai.rest

import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import kotlinx.serialization.json.Json
import kotlinx.serialization.decodeFromString

class DocumentQaRestServerTest {

    @TempDir
    lateinit var tempDir: File
    
    private lateinit var server: DocumentQaRestServer
    private val port = 8081 // Use different port than default to avoid conflicts
    
    @BeforeEach
    fun setup() {
        // Create a test document
        val testFile = File(tempDir, "test.txt")
        testFile.writeText("This is a test document about artificial intelligence and machine learning.")
        
        server = DocumentQaRestServer(tempDir, port)
        // Start server in background
        Thread {
            server.start(wait = false)
        }.start()
        
        // Wait a bit for server to start
        Thread.sleep(2000)
    }
    
    @AfterEach
    fun teardown() {
        server.stop()
    }
    
    @Test
    fun testServerStartsAndRespondsToRoot() {
        val url = URL("http://localhost:$port/")
        val connection = url.openConnection() as HttpURLConnection
        connection.requestMethod = "GET"
        
        val responseCode = connection.responseCode
        assert(responseCode == 200) { "Expected 200 but got $responseCode" }
        
        val response = connection.inputStream.bufferedReader().readText()
        assert(response.contains("PromptFx Document Q&A API")) { "Response should contain service name" }
    }
    
    @Test
    fun testFoldersEndpoint() {
        val url = URL("http://localhost:$port/folders")
        val connection = url.openConnection() as HttpURLConnection
        connection.requestMethod = "GET"
        
        val responseCode = connection.responseCode
        assert(responseCode == 200) { "Expected 200 but got $responseCode" }
        
        val response = connection.inputStream.bufferedReader().readText()
        val foldersResponse = Json.decodeFromString<FoldersResponse>(response)
        
        // Should have empty folders list since we're using root directly
        assert(foldersResponse.folders.isEmpty()) { "Should have no subfolders in temp directory" }
    }
}