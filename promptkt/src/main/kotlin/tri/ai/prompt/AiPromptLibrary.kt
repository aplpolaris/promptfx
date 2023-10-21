/*-
 * #%L
 * promptkt-0.1.0-SNAPSHOT
 * %%
 * Copyright (C) 2023 Johns Hopkins University Applied Physics Laboratory
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
package tri.ai.prompt

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.KotlinModule
import com.fasterxml.jackson.module.kotlin.readValue
import java.io.File
import kotlin.reflect.KClass

/** Library of prompt templates. */
class AiPromptLibrary {
    var prompts = mutableMapOf<String, AiPrompt>()

    companion object {
        fun lookupPrompt(id: String): AiPrompt {
            return INSTANCE.prompts[id] ?: throw IllegalArgumentException("No prompt found with id $id")
        }

        val RUNTIME_PROMPTS_FILE = File("prompts.yaml")

        val RUNTIME_INSTANCE by lazy {
            AiPromptLibrary().apply {
                if (RUNTIME_PROMPTS_FILE.exists())
                    prompts.putAll(MAPPER.readValue<Map<String, AiPrompt>>(RUNTIME_PROMPTS_FILE))
            }
        }

        val INSTANCE by lazy {
            AiPromptLibrary().apply {
                prompts.putAll(AiPromptLibrary::class.yaml<Map<String, AiPrompt>>("resources/prompts.yaml"))
                prompts.putAll(RUNTIME_INSTANCE.prompts)
            }
        }

        val MAPPER = ObjectMapper(YAMLFactory()).apply {
            registerModule(KotlinModule.Builder().build())
            registerModule(JavaTimeModule())
        }

        inline fun <reified X> KClass<*>.yaml(resource: String) =
            java.getResourceAsStream(resource).use { MAPPER.readValue<X>(it!!) }
    }
}
