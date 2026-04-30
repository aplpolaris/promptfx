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
package tri.ai.openai

import com.aallam.openai.api.file.FileSource
import kotlinx.coroutines.test.runTest
import kotlinx.io.files.Path
import kotlinx.io.files.SystemFileSystem
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import java.io.File

class OpenAiFilesTest {

    val adapter = OpenAiAdapter.INSTANCE

    @Test
    @Tag("openai")
    fun testListFiles() = runTest {
        val files = adapter.listFiles()
        println("Files count: ${files.size}")
        files.forEach { println("  ${it.filename} (${it.id})") }
        assertNotNull(files)
    }

    @Test
    @Tag("openai")
    fun testUploadGetDeleteFile() = runTest {
        // create a temporary file to upload
        val tempFile = File.createTempFile("promptfx-test-", ".txt").also {
            it.writeText("Hello from promptfx Files API test.")
            it.deleteOnExit()
        }

        // upload
        val source = FileSource(Path(tempFile.absolutePath), SystemFileSystem)
        val uploaded = adapter.uploadFile(source, "assistants")
        println("Uploaded: ${uploaded.filename} id=${uploaded.id}")
        assertNotNull(uploaded.id)
        assertEquals(tempFile.name, uploaded.filename)

        // retrieve metadata
        val retrieved = adapter.getFile(uploaded.id.id)
        println("Retrieved: ${retrieved?.filename} id=${retrieved?.id}")
        assertEquals(uploaded.id, retrieved?.id)

        // delete
        val deleted = adapter.deleteFile(uploaded.id.id)
        println("Deleted: $deleted")
        assertTrue(deleted)
    }

    @Test
    @Tag("openai")
    fun `test upload and download batch file`() = runTest {
        val content = """{"x":"Hello from promptfx download test."}"""
        val tempFile = File.createTempFile("promptfx-dl-test-", ".jsonl").also {
            it.writeText(content)
            it.deleteOnExit()
        }

        val source = FileSource(Path(tempFile.absolutePath), SystemFileSystem)
        val uploaded = adapter.uploadFile(source, "batch")
        println("Uploaded for download test: ${uploaded.filename} id=${uploaded.id}")

        try {
            val bytes = adapter.downloadFile(uploaded.id.id)
            println("Downloaded ${bytes.size} bytes")
            assertEquals(content, bytes.decodeToString())
        } finally {
            adapter.deleteFile(uploaded.id.id)
        }
    }

}
