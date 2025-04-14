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
package tri.ai.openai.api

import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.module.kotlin.readValue
import tri.ai.core.MAPPER
import tri.ai.core.ModelIndex
import java.io.File

/**
 * Global configuration of OpenAI-compatible API endpoints.
 * Allows management if individual settings and keys for all endpoints.
 */
class OpenAiApiConfig {
    /** List of OpenAI-compatible API settings. */
    var endpoints = mutableListOf<OpenAiApiEndpointConfig>()

    companion object {
        /**
         * Default configuration of OpenAI-compatible API endpoints.
         * Provides a default set of configurations within the code base, with the option to overwrite with a runtime config.
         */
        val INSTANCE = OpenAiApiConfig().apply {
            val defaultConfig: OpenAiApiConfig = MAPPER.readValue(OpenAiApiConfig::class.java.getResourceAsStream("resources/$RESOURCE_NAME")!!)
            val runtimeConfig = setOf(File(RESOURCE_NAME), File("config/$RESOURCE_NAME"))
                .firstOrNull { it.exists() }?.let {
                    MAPPER.readValue<OpenAiApiConfig>(it)
                } ?: OpenAiApiConfig()
            val runtimeSources = runtimeConfig.endpoints.map { it.source }.toSet()
            endpoints.addAll(defaultConfig.endpoints.filter { it.source !in runtimeSources })
            endpoints.addAll(runtimeConfig.endpoints)
        }
        private const val RESOURCE_NAME = "openai-api-config.yaml"
    }
}

/** Configuration for a single OpenAI-compatible API endpoint. */
class OpenAiApiEndpointConfig {
    var source: String = "OpenAI-Compatible API"
    var settings: OpenAiApiSettingsGeneric = OpenAiApiSettingsGeneric()
    var modelFileName: String = ""

    @get:JsonIgnore
    val index
        get() = ModelIndex(modelFileName)
}
