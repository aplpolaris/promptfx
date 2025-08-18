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
package tri.ai.prompt

import com.fasterxml.jackson.annotation.JsonInclude
import com.github.mustachejava.DefaultMustacheFactory
import java.io.StringReader
import java.io.StringWriter
import java.time.LocalDate

/**
 * Wraps a mustache template string with helpful utilities for finding and filling fields.
 * Allows for some use of default/assumed field names: [TODAY], [INPUT], [INSTRUCT].
 */
@JsonInclude(JsonInclude.Include.NON_DEFAULT)
data class PromptTemplate(
    val template: String,
    val injectToday: Boolean = true
) {

    private val mustache by lazy {
        mustacheFactory(template)
            .compile(template)
    }

    //region FILLS WITH DATA

    /** Fills in mustache template. */
    fun fill(vararg fields: Pair<String, Any>) =
        fill(fields.toMap())

    /** Fills in mustache template. */
    fun fill(fields: Map<String, Any>) =
        StringWriter().apply {
            mustache.execute(this, fields.withDefaultFields())
        }.toString()

    //endregion

    //region FILLS WITH SOME DEFAULT PARAMETER NAMES ASSUMED

    /** Fills in input field. */
    fun fillInput(input: String) =
        fill(defaultInputParams(input))

    /** Fills in input and instruct fields. */
    fun fillInstruct(input: String, instruct: String) =
        fill(defaultInstructParams(input, instruct))

    //endregion

    //region UTILS

    /** Finds all fields in a template. */
    fun findFields(): List<String> {
        var templateText = template
        val foundFields = templateText.split("{{{").drop(1).map { it.substringBefore("}}}") }.toMutableSet()
        foundFields.forEach { templateText = templateText.replace("{{{$it}}}", "") }
        foundFields.addAll(templateText.split("{{").drop(1).map { it.substringBefore("}}") })
        foundFields.removeIf { it.isBlank() || it[0] in "/#^" }
        return foundFields.toList()
    }

    /** Adds default fields to user provided fields. */
    private fun Map<String, Any>.withDefaultFields() = when {
        injectToday -> this + mapOf(TODAY to LocalDate.now())
        else -> this
    }

    /** Creates a mustache factory for a template. */
    private fun mustacheFactory(template: String) = DefaultMustacheFactory {
        StringReader(template)
    }

    //endregion

    companion object {
        /** Constant for input string. */
        const val INPUT = "input"
        /** Constant for instruct string. */
        const val INSTRUCT = "instruct"
        /** Constant for current date. */
        const val TODAY = "today"

        /** Gets basic input prompt parameters. */
        fun defaultInputParams(input: String) =
            mapOf(INPUT to input, TODAY to LocalDate.now())
        /** Gets instruct parameters. */
        fun defaultInstructParams(input: String, instruct: String) =
            mapOf(INPUT to input, INSTRUCT to instruct, TODAY to LocalDate.now())
    }
}

/** Gets template from prompt object. */
fun PromptDef.template() = PromptTemplate(template, injectToday = contextInject?.today ?: true)

/** Gets and fills template from prompt object. */
fun PromptDef.fill(vararg fields: Pair<String, Any>) =
    template().fill(*fields)

/** Gets and fills template from prompt object. */
fun PromptDef.fill(fields: Map<String, Any>) =
    template().fill(fields)
