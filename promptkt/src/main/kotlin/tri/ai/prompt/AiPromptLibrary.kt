/*-
 * #%L
 * tri.promptfx:promptkt
 * %%
 * Copyright (C) 2023 - 2024 Johns Hopkins University Applied Physics Laboratory
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

        /** Get list of prompts with given prefix. */
        fun withPrefix(prefix: String) =
            INSTANCE.prompts.keys.filter { it.startsWith(prefix) }

        /** Get prompt with given id. */
        fun lookupPrompt(id: String): AiPrompt {
            return INSTANCE.prompts[id] ?: throw IllegalArgumentException("No prompt found with id $id")
        }

        //region RUNTIME PROMPT FILE

        /** The file used to store prompts created at runtime. */
        val RUNTIME_PROMPTS_FILE = File("prompts.yaml")

        /** The instance of the prompt library with only the prompts configurable at runtime. */
        val RUNTIME_INSTANCE by lazy {
            AiPromptLibrary().apply {
                if (RUNTIME_PROMPTS_FILE.exists())
                    prompts.putAll(read(RUNTIME_PROMPTS_FILE))
            }
        }

        /** The instance of the prompt library with both preconfigured and runtime prompts. */
        val INSTANCE by lazy {
            AiPromptLibrary().apply {
                prompts.putAll(AiPromptLibrary::class.yaml<Map<String, AiPrompt>>("resources/prompts.yaml"))
                prompts.putAll(RUNTIME_INSTANCE.prompts)
            }
        }

        fun createRuntimePromptsFile() {
            val file = RUNTIME_PROMPTS_FILE
            file.parentFile?.mkdirs()
            file.createNewFile()
            file.writeText("""
                # This file is used to store prompts that are created at runtime.
                #
                # Usage:
                #  - preconfigured prompt file in "prompts.yaml" in promptkt
                #  - custom prompt file in "prompts.yaml" in the current directory
                #
                # Syntax:
                #  - mustache templates: https://mustache.github.io/mustache.5.html
                #  - double braces to insert text {{...}}
                #  - triple braces to insert text without escaping HTML {{{...}}}
                #
                # Keywords:
                #  - many templates expect {{input}} and/or {{instruct}}
                #  - {{today}} is always replaced with the current date
                #
                ---
                # This is a sample prompt. You can delete it.
                my-custom-prompt: |
                  cheese -> fromage
                  boy -> garcon
                  {{input}} ->                   
            """.trimIndent())
        }

        fun refreshRuntimePrompts() {
            RUNTIME_INSTANCE.prompts.clear()
            if (RUNTIME_PROMPTS_FILE.exists())
                RUNTIME_INSTANCE.prompts.putAll(read(RUNTIME_PROMPTS_FILE))
            INSTANCE.prompts.clear()
            INSTANCE.prompts.putAll(AiPromptLibrary::class.yaml<Map<String, AiPrompt>>("resources/prompts.yaml"))
            INSTANCE.prompts.putAll(RUNTIME_INSTANCE.prompts)
        }

        //endregion

        //region PROMPT LOADING

        /** Load prompts from given URL. */
        fun read(url: String) =
            MAPPER.readValue<Map<String, AiPrompt>>(url)

        /** Load prompts from given file. */
        fun read(file: File) =
            MAPPER.readValue<Map<String, AiPrompt>>(file)

        /** ObjectMapper for loading prompts. */
        val MAPPER = ObjectMapper(YAMLFactory()).apply {
            registerModule(KotlinModule.Builder().build())
            registerModule(JavaTimeModule())
        }

        /** Load a resource as YAML from the classpath. */
        inline fun <reified X> KClass<*>.yaml(resource: String) =
            java.getResourceAsStream(resource).use { MAPPER.readValue<X>(it!!) }

        //endregion

    }
}
