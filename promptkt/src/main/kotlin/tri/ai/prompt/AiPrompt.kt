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

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonValue
import com.github.mustachejava.DefaultMustacheFactory
import tri.util.fine
import java.io.StringReader
import java.io.StringWriter
import java.time.LocalDate

/** A prompt template that can be filled in with user input. */
class AiPrompt @JsonCreator(mode = JsonCreator.Mode.DELEGATING) constructor (@JsonValue var template: String) {


    /** Fills in input field. */
    fun prompt(input: String) =
        template.fill(promptParams(input))

    /** Gets basic input prompt parameters. */
    fun promptParams(input: String) =
        mapOf("input" to input, "today" to LocalDate.now())

    /** Fills in input and instruct fields. */
    fun instruct(instruct: String, input: String) =
        template.fill(instructParams(instruct, input))

    /** Gets instruct parameters. */
    fun instructParams(instruct: String, input: String) =
        mapOf("input" to input, "instruct" to instruct, "today" to LocalDate.now())

    /** Fills in arbitrary fields. */
    fun fill(fields: Map<String, Any>) = template.fill(
        mapOf("today" to LocalDate.now()) + fields
    ).also {
        fine<AiPrompt>(it)
    }

    /** Fills in arbitrary fields. */
    fun fill(vararg fields: Pair<String, Any>) = fill(fields.toMap())

    /** Get list of fields in the template. */
    fun fields() = fieldsInTemplate(template)

    companion object {

        /** Finds all fields in a template. */
        fun fieldsInTemplate(template: String): List<String> {
            var templateText = template
            val foundFields = templateText.split("{{{").drop(1).map { it.substringBefore("}}}") }.toMutableSet()
            foundFields.forEach { templateText = templateText.replace("{{{$it}}}", "") }
            foundFields.addAll(templateText.split("{{").drop(1).map { it.substringBefore("}}") })
            foundFields.removeIf { it.isBlank() || it[0] in "/#^" }
            return foundFields.toList()
        }

        //region MUSTACHE TEMPLATES

        /** Fills in mustache template. */
        fun String.fill(vararg fields: Pair<String, Any>) =
            fill(fields.toMap())

        /** Fills in mustache template. */
        fun String.fill(fields: Map<String, Any>) =
            StringWriter().apply {
                mustacheFactory(this@fill)
                    .compile(this@fill)
                    .execute(this, fields)
            }.toString()

        /** Creates a mustache factory for a template. */
        private fun mustacheFactory(template: String) = DefaultMustacheFactory {
            StringReader(template)
        }

        //endregion

    }

}
