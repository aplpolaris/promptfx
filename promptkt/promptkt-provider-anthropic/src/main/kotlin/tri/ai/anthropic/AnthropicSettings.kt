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
package tri.ai.anthropic

import io.ktor.client.HttpClient
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logging
import io.ktor.client.plugins.logging.SIMPLE
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.Json
import tri.ai.core.ApiSettings
import tri.util.warning
import java.io.File

/** Manages Anthropic API key and Ktor HTTP client. */
@OptIn(ExperimentalSerializationApi::class)
class AnthropicSettings : ApiSettings {

    override var baseUrl = BASE_URL
        set(value) {
            field = value
            buildClient()
        }

    override var apiKey = readApiKey()
        set(value) {
            field = value
            buildClient()
        }

    var timeoutSeconds = 60
        set(value) {
            field = value
            buildClient()
        }

    /** The HTTP client used to make requests. */
    var client: HttpClient = buildClient()

    override fun isConfigured(): Boolean {
        return apiKey.isNotBlank() && !apiKey.trim().contains(" ")
    }

    override fun checkApiKey() {
        if (!isConfigured())
            throw UnsupportedOperationException("Anthropic API key is not configured properly.")
    }

    @Throws(IllegalStateException::class)
    private fun buildClient() = HttpClient {
        install(ContentNegotiation) {
            json(Json {
                isLenient = true
                ignoreUnknownKeys = true
                explicitNulls = false
            })
        }
        install(Logging) {
            logger = io.ktor.client.plugins.logging.Logger.SIMPLE
            level = LogLevel.NONE
        }
        install(HttpTimeout) {
            socketTimeoutMillis = timeoutSeconds * 1000L
            connectTimeoutMillis = timeoutSeconds * 1000L
            requestTimeoutMillis = timeoutSeconds * 1000L
        }
        defaultRequest {
            url(baseUrl)
            headers.append("x-api-key", apiKey)
            headers.append("anthropic-version", ANTHROPIC_VERSION)
            contentType(ContentType.Application.Json)
        }
    }.also { client = it }

    /** Read API key from [API_KEY_FILE] file or [API_KEY_ENV] environment variable. */
    private fun readApiKey(): String {
        val file = File(API_KEY_FILE)
        val key = if (file.exists()) file.readText() else System.getenv(API_KEY_ENV)
        return if (key.isNullOrBlank()) {
            warning<AnthropicSettings>("Anthropic API key not found. Please create a file named $API_KEY_FILE in the root directory, or set an environment variable named $API_KEY_ENV.")
            ""
        } else
            key.trim()
    }

    companion object {
        const val API_KEY_FILE = "apikey-anthropic.txt"
        const val API_KEY_ENV = "ANTHROPIC_API_KEY"
        const val BASE_URL = "https://api.anthropic.com/v1/"
        const val ANTHROPIC_VERSION = "2023-06-01"
    }
}
