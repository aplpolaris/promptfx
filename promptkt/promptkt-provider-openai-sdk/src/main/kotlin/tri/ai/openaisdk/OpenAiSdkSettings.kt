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
package tri.ai.openaisdk

import tri.ai.core.ApiSettings
import tri.util.info
import tri.util.warning
import java.io.File

/** Manages OpenAI SDK configuration. */
class OpenAiSdkSettings : ApiSettings {

    override var baseUrl: String? = readBaseUrl()
    override var apiKey = readApiKey()

    override fun isConfigured(): Boolean {
        val isOpenAi = baseUrl.let { it == null || it.contains("api.openai.com") }
        val isValidOpenAiKey = apiKey.startsWith("sk-") && !apiKey.trim().contains(" ")
        return isValidOpenAiKey || !isOpenAi
    }

    override fun checkApiKey() {
        if (!isConfigured())
            throw UnsupportedOperationException(
                "OpenAI API key is not configured. Please create a file named $API_KEY_FILE " +
                "in the root directory, or set an environment variable named $API_KEY_ENV."
            )
    }

    /** Read API key by first checking for [API_KEY_FILE], then environment variable [API_KEY_ENV]. */
    private fun readApiKey(): String {
        val file = File(API_KEY_FILE)
        val key = if (file.exists()) {
            file.readText().trim()
        } else {
            System.getenv(API_KEY_ENV)
        }

        return if (key.isNullOrBlank()) {
            warning<OpenAiSdkSettings>(
                "OpenAI API key not found. Please create a file named $API_KEY_FILE " +
                "in the root directory, or set an environment variable named $API_KEY_ENV."
            )
            ""
        } else {
            info<OpenAiSdkSettings>("OpenAI API key loaded successfully.")
            key
        }
    }

    /** Read optional base URL override from [BASE_URL_FILE] or [BASE_URL_ENV]. */
    private fun readBaseUrl(): String? {
        val file = File(BASE_URL_FILE)
        val url = if (file.exists()) {
            file.readText().trim().takeIf { it.isNotBlank() }
        } else {
            System.getenv(BASE_URL_ENV)
        }
        return url?.also { info<OpenAiSdkSettings>("OpenAI SDK base URL override: $it") }
    }

    companion object {
        const val API_KEY_FILE = "apikey-openai.txt"
        const val API_KEY_ENV = "OPENAI_API_KEY"
        const val BASE_URL_FILE = "openai-baseurl.txt"
        const val BASE_URL_ENV = "OPENAI_BASE_URL"
    }

}
