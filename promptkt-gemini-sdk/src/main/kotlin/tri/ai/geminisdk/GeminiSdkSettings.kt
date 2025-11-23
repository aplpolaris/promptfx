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
package tri.ai.geminisdk

import tri.util.info
import tri.util.warning
import java.io.File

/** Manages Gemini SDK configuration. */
class GeminiSdkSettings {

    companion object {
        const val API_KEY_FILE = "apikey-gemini.txt"
        const val API_KEY_ENV = "GEMINI_API_KEY"
        const val PROJECT_ID_FILE = "gemini-project-id.txt"
        const val PROJECT_ID_ENV = "GEMINI_PROJECT_ID"
        const val LOCATION_FILE = "gemini-location.txt"
        const val LOCATION_ENV = "GEMINI_LOCATION"
        const val DEFAULT_LOCATION = "us-central1"
    }

    var apiKey = readApiKey()
    var projectId = readProjectId()
    var location = readLocation()
    var useVertexAI = readUseVertexAI()

    /** Returns true if the client is configured with required settings. */
    fun isConfigured() = apiKey.isNotBlank()

    /** Read API key by first checking for [API_KEY_FILE], and then checking user environment variable [API_KEY_ENV]. */
    private fun readApiKey(): String {
        val file = File(API_KEY_FILE)
        val key = if (file.exists()) {
            file.readText().trim()
        } else
            System.getenv(API_KEY_ENV)

        return if (key.isNullOrBlank()) {
            warning<GeminiSdkSettings>("Gemini API key not found. Please create a file named $API_KEY_FILE in the root directory, or set an environment variable named $API_KEY_ENV.")
            ""
        } else {
            info<GeminiSdkSettings>("Gemini API key loaded successfully.")
            key
        }
    }

    /** Read project ID by first checking for [PROJECT_ID_FILE], and then checking user environment variable [PROJECT_ID_ENV]. */
    private fun readProjectId(): String {
        val file = File(PROJECT_ID_FILE)
        val id = if (file.exists()) {
            file.readText().trim()
        } else
            System.getenv(PROJECT_ID_ENV)

        return if (id.isNullOrBlank()) {
            warning<GeminiSdkSettings>("Gemini project ID not found. Please create a file named $PROJECT_ID_FILE in the root directory, or set an environment variable named $PROJECT_ID_ENV.")
            ""
        } else {
            info<GeminiSdkSettings>("Gemini project ID loaded successfully: $id")
            id
        }
    }

    /** Read location by first checking for [LOCATION_FILE], and then checking user environment variable [LOCATION_ENV], with default [DEFAULT_LOCATION]. */
    private fun readLocation(): String {
        val file = File(LOCATION_FILE)
        val loc = if (file.exists()) {
            file.readText().trim()
        } else
            System.getenv(LOCATION_ENV)

        return if (loc.isNullOrBlank()) {
            info<GeminiSdkSettings>("Using default Gemini location: $DEFAULT_LOCATION")
            DEFAULT_LOCATION
        } else {
            info<GeminiSdkSettings>("Gemini location loaded: $loc")
            loc
        }
    }

    /** Determine whether to use Vertex AI or Gemini Developer API based on project ID availability. */
    private fun readUseVertexAI(): Boolean {
        return projectId.isNotBlank()
    }

}
