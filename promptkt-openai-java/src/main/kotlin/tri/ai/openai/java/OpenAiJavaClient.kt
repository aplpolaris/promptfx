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
import kotlinx.coroutines.future.await
import java.io.Closeable

/**
 * General purpose client wrapper for the OpenAI official Java SDK.
 * Provides Kotlin coroutines support for the Java SDK.
 */
class OpenAiJavaClient : Closeable {

    companion object {
        val INSTANCE = OpenAiJavaClient()
    }

    val settings = OpenAiJavaSettings()
    val client: OpenAIClient get() = settings.client

    /** Returns true if the client is configured with an API key. */
    fun isConfigured() = settings.isConfigured()

    /** List all available models. */
    suspend fun listModels() = client.models().list().autoPager().stream().toList()

    override fun close() {
        // OpenAI Java SDK clients handle cleanup automatically
    }

}
