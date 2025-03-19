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
package tri.ai.openai

import com.aallam.openai.api.http.Timeout
import com.aallam.openai.api.logging.LogLevel
import com.aallam.openai.client.LoggingConfig
import com.aallam.openai.client.OpenAI
import com.aallam.openai.client.OpenAIConfig
import com.aallam.openai.client.OpenAIHost
import java.io.File
import java.util.logging.Logger
import kotlin.time.Duration.Companion.seconds

/** Manages OpenAI API key and client. */
class OpenAiSettings {

    companion object {
        const val API_KEY_FILE = "apikey.txt"
        const val API_KEY_ENV = "OPENAI_API_KEY"
    }

    var baseUrl: String? = null
        set(value) {
            field = value
            buildClient()
        }

    var apiKey = readApiKey()
        set(value) {
            field = value
            buildClient()
        }

    var logLevel = LogLevel.None
        set(value) {
            field = value
            buildClient()
        }

    var timeoutSeconds = 60
        set(value) {
            field = value
            buildClient()
        }

    var client: OpenAI
        private set

    init {
        client = buildClient()
    }

    /** Read API key by first checking for [API_KEY_FILE], and then checking user environment variable [API_KEY_ENV]. */
    private fun readApiKey(): String {
        val file = File(API_KEY_FILE)

        val key = if (file.exists()) {
            file.readText()
        } else
            System.getenv(API_KEY_ENV)

        return if (key.isNullOrBlank()) {
            Logger.getLogger(OpenAiSettings::class.java.name).warning(
                "No API key found. Please create a file named $API_KEY_FILE in the root directory, or set an environment variable named $API_KEY_ENV."
            )
            ""
        } else
            key
    }

    @Throws(IllegalStateException::class)
    private fun buildClient(): OpenAI {
        client = OpenAI(
            OpenAIConfig(
                host = if (baseUrl == null) OpenAIHost.OpenAI else OpenAIHost(baseUrl!!),
                token = apiKey,
                logging = LoggingConfig(logLevel),
                timeout = Timeout(socket = timeoutSeconds.seconds)
            )
        )
        return client
    }

}
