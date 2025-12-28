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
package tri.ai.mcp

import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.fail
import org.junit.jupiter.api.Test

class McpProviderStdioTest {

    @Test
    fun testInvalidCommand() {
        // Test that we get an appropriate error when trying to connect to a nonexistent command
        assertThrows(Exception::class.java) {
            runTest {
                val provider = McpProviderStdio("nonexistent-command-12345")
                provider.close()
            }
        }
    }

    @Test
    fun testWithArgs() {
        runTest {
            // Test that args are passed correctly (will fail since echo doesn't respond properly)
            try {
                val provider = McpProviderStdio("echo", listOf("test"))
                
                // This should fail when trying to communicate
                try {
                    provider.listPrompts()
                    fail("Should have thrown an exception")
                } catch (e: McpException) {
                    println("testWithArgs error (expected): ${e.message}")
                }

                provider.close()
            } catch (e: Exception) {
                // Command might not exist on all systems
                println("testWithArgs - command not found (expected on some systems)")
            }
        }
    }
}
