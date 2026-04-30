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

import com.openai.client.OpenAIClient
import com.openai.client.okhttp.OpenAIOkHttpClient
import java.io.Closeable

/**
 * Client for the OpenAI API using the official openai-java SDK.
 * See https://github.com/openai/openai-java
 */
class OpenAiSdkClient : Closeable {

    val settings = OpenAiSdkSettings()
    private var client: OpenAIClient? = null

    init {
        if (settings.isConfigured()) {
            val builder = OpenAIOkHttpClient.builder()
                .apiKey(settings.apiKey)
            settings.baseUrl?.let { builder.baseUrl(it) }
            client = builder.build()
        }
    }

    fun isConfigured() = settings.isConfigured() && client != null

    /** Returns the underlying SDK client, throwing if not initialized. */
    fun getClient(): OpenAIClient = client ?: throw IllegalStateException("OpenAI SDK client not initialized. Check that the API key is configured.")

    override fun close() {
        client?.close()
    }

    companion object {
        val INSTANCE by lazy { OpenAiSdkClient() }
    }

}
