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

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonValue
import com.github.mustachejava.DefaultMustacheFactory
import com.github.mustachejava.MustacheResolver
import com.github.mustachejava.resolver.DefaultResolver
import java.io.StringReader
import java.io.StringWriter
import java.time.LocalDate

/** A prompt template that can be filled in with a user input. */
class AiPrompt @JsonCreator(mode = JsonCreator.Mode.DELEGATING) constructor (@JsonValue var template: String) {

    /** Fills in input field. */
    fun prompt(input: String) = template.fill(
        "input" to input,
        "today" to LocalDate.now()
    )

    /** Fills in input and instruct fields. */
    fun instruct(instruct: String, input: String) = template.fill(
        "input" to input,
        "instruct" to instruct,
        "today" to LocalDate.now()
    )

    /** Fills in arbitrary fields. */
    fun fill(vararg fields: Pair<String, String>) = template.fill(
        "today" to LocalDate.now(),
        *fields
    ).also {
        println(it)
    }

    companion object {

        //region MUSTACHE TEMPLATES

        /** Fills in mustache template. */
        private fun String.fill(vararg fields: Pair<String, Any>) =
            StringWriter().apply {
                mustacheFactory(this@fill)
                    .compile(this@fill)
                    .execute(this, fields.toMap())
            }.toString()

        private fun mustacheFactory(template: String) = DefaultMustacheFactory {
            StringReader(template)
        }

        //endregion

    }

}
