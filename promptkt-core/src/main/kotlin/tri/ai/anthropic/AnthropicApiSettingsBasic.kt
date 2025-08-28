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

import io.ktor.client.*
import io.ktor.client.engine.okhttp.*
import com.fasterxml.jackson.databind.DeserializationFeature
import java.io.File
import java.util.logging.Logger

/** Manages Anthropic API key and client. */
class AnthropicApiSettingsBasic : AnthropicApiSettings {

    companion object {
        const val API_KEY_FILE = "anthropic-apikey.txt"
        const val API_KEY_ENV = "ANTHROPIC_API_KEY"
    }

    override var baseUrl: String? = null
        set(value) {
            field = value
            buildClient()
        }

    override var apiKey = readApiKey()
        set(value) {
            field = value
            buildClient()
        }

    var timeoutMillis = 60000L
        set(value) {
            field = value
            buildClient()
        }

    var client: HttpClient
        private set

    init {
        client = buildClient()
    }

    /** Read API key by first checking for [API_KEY_FILE], and then checking user environment variable [API_KEY_ENV]. */
    private fun readApiKey(): String {
        val file = File(API_KEY_FILE)

        val key = if (file.exists()) {
            file.readText().trim()
        } else
            System.getenv(API_KEY_ENV)

        return if (key.isNullOrBlank()) {
            Logger.getLogger(AnthropicApiSettings::class.java.name).warning(
                "No Anthropic API key found. Please create a file named $API_KEY_FILE in the root directory, or set an environment variable named $API_KEY_ENV."
            )
            ""
        } else
            key.trim()
    }

    /** Checks for an Anthropic API key. */
    override fun checkApiKey() {
        val isValidKey = apiKey.isNotBlank() && apiKey.startsWith("sk-ant-")
        if (!isValidKey)
            throw UnsupportedOperationException("Invalid Anthropic API key. Please set a valid Anthropic API key starting with 'sk-ant-'.")
    }

    @Throws(IllegalStateException::class)
    override fun buildClient(): HttpClient {
        client = HttpClient(OkHttp) {
            engine {
                config {
                    readTimeout(timeoutMillis, java.util.concurrent.TimeUnit.MILLISECONDS)
                }
            }
        }
        return client
    }

}