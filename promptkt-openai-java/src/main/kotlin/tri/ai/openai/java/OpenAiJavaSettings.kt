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
package tri.ai.openai.java

import com.openai.client.OpenAIClient
import com.openai.client.okhttp.OpenAIOkHttpClient
import tri.util.info
import tri.util.warning
import java.io.File

/** Manages OpenAI API key and client using the official Java SDK. */
class OpenAiJavaSettings {

    companion object {
        const val API_KEY_FILE = "apikey-openai.txt"
        const val API_KEY_ENV = "OPENAI_API_KEY"
        const val BASE_URL_ENV = "OPENAI_BASE_URL"
    }

    var apiKey = readApiKey()
        set(value) {
            field = value
            buildClient()
        }

    var baseUrl: String? = System.getenv(BASE_URL_ENV)
        set(value) {
            field = value
            buildClient()
        }

    /** The OpenAI client used to make requests. */
    var client: OpenAIClient = buildClient()

    /** Returns true if the client is configured with an API key. */
    fun isConfigured() = apiKey.isNotBlank()

    /** Read API key by first checking for [API_KEY_FILE], and then checking user environment variable [API_KEY_ENV]. */
    private fun readApiKey(): String {
        val file = File(API_KEY_FILE)

        val key = if (file.exists()) {
            info<OpenAiJavaSettings>("OpenAI API key found in $API_KEY_FILE")
            file.readText().trim()
        } else
            System.getenv(API_KEY_ENV)

        return if (key.isNullOrBlank()) {
            warning<OpenAiJavaSettings>("OpenAI API key not found. Please create a file named $API_KEY_FILE in the root directory, or set an environment variable named $API_KEY_ENV.")
            ""
        } else {
            info<OpenAiJavaSettings>("OpenAI API key configured")
            key
        }
    }

    private fun buildClient(): OpenAIClient {
        return if (apiKey.isBlank()) {
            // Return a dummy client if no API key is configured
            OpenAIOkHttpClient.builder().apiKey("sk-dummy").build()
        } else {
            val builder = OpenAIOkHttpClient.builder()
                .apiKey(apiKey)
            
            baseUrl?.let { builder.baseUrl(it) }
            
            builder.build()
        }.also { client = it }
    }

}
