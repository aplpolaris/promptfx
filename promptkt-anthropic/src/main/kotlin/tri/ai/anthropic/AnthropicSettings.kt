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
package tri.ai.anthropic

import com.anthropic.client.AnthropicClient
import com.anthropic.client.okhttp.AnthropicOkHttpClient
import tri.util.info
import tri.util.warning
import java.io.File

/** Manages Anthropic API key and client. */
class AnthropicSettings {

    companion object {
        const val API_KEY_FILE = "apikey-anthropic.txt"
        const val API_KEY_ENV = "ANTHROPIC_API_KEY"
        const val BASE_URL = "https://api.anthropic.com"
    }

    var baseUrl = BASE_URL
        set(value) {
            field = value
            buildClient()
        }

    var apiKey = readApiKey()
        set(value) {
            field = value
            buildClient()
        }

    /** The Anthropic client used to make requests. */
    var client: AnthropicClient = buildClient()

    /** Read API key by first checking for [API_KEY_FILE], and then checking user environment variable [API_KEY_ENV]. */
    private fun readApiKey(): String {
        val file = File(API_KEY_FILE)

        val key = if (file.exists()) {
            file.readText().trim()
        } else
            System.getenv(API_KEY_ENV)

        return if (key.isNullOrBlank()) {
            warning<AnthropicSettings>("Anthropic API key not found. Please create a file named $API_KEY_FILE in the root directory, or set an environment variable named $API_KEY_ENV.")
            ""
        } else {
            info<AnthropicSettings>("Anthropic API key loaded successfully.")
            key
        }
    }

    @Throws(IllegalStateException::class)
    private fun buildClient(): AnthropicClient {
        return if (apiKey.isNotBlank()) {
            AnthropicOkHttpClient.builder()
                .apiKey(apiKey)
                .baseUrl(baseUrl)
                .build()
        } else {
            // Return a dummy client if no API key is configured
            AnthropicOkHttpClient.builder()
                .apiKey("dummy-key")
                .baseUrl(baseUrl)
                .build()
        }.also { client = it }
    }

}
