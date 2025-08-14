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

import com.aallam.openai.api.logging.LogLevel
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.KotlinModule
import org.junit.jupiter.api.Test

class OpenAiApiConfigTest {

    @Test
    fun testConfig() {
        val config = OpenAiApiConfig()
        config.endpoints.add(OpenAiApiEndpointConfig().apply {
            source = "Test Source"
            settings = OpenAiApiSettingsGeneric().apply {
                baseUrl = "https://api.openai.com/v1"
                apiKeyFile = "test-api-key.txt"
                apiKeyRegistry = "OPENAI_API_KEY"
                logLevel = LogLevel.None
                timeoutSeconds = 60
            }
            modelFileName = "test-models.yaml"
        })
        val str = ObjectMapper(YAMLFactory()).apply {
            registerModule(KotlinModule.Builder().build())
            registerModule(JavaTimeModule())
        }.writerWithDefaultPrettyPrinter().writeValueAsString(config)
        println(str)
    }

}
