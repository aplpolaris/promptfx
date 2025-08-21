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

import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonInclude

/** A prompt template that can be filled in with user input. */
@JsonInclude(JsonInclude.Include.NON_DEFAULT)
data class PromptDef(
    /** Canonical id, optionally versioned: `group/name[@MAJOR.MINOR.PATCH]`. */
    val id: String,

    /** Category, optional if derived from id prefix. */
    val category: String? = null,
    /** Tags for discoverability and bucketing. */
    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    val tags: List<String> = listOf(),

    /** Human-readable name; defaults to last path segment. */
    val name: String? = null,
    /** Display title. */
    val title: String? = null,
    /** Display description. */
    val description: String? = null,
    /** Version. */
    val version: String? = null,

    /** Arguments contract. */
    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    val args: List<PromptArgDef> = listOf(),

    /** Mustache template string. */
    val template: String? = null,
    /** Rendering context/hints. */
    val contextInject: ContextConfig? = null

    // TODO - consider inclusion of additional context: rendering engine, ...
    // TODO - consider inclusion of additional tracking information: aliases, deprecated, ...
) {
    @get:JsonIgnore
    val bareId: String by lazy { id.substringBefore('@') }

    /** Best non-null title for user. */
    fun title() = title ?: name ?: id.substringAfter('/').substringBefore('@').ifBlank { id }.ifBlank { "Untitled Prompt" }

    /** Create a copy where values are overridden if provided/customized in the other prompt definition object. */
    fun copyWithOverrides(other: PromptDef): PromptDef {
        return PromptDef(
            id = other.id,
            category = other.category ?: category,
            tags = other.tags.ifEmpty { tags },
            name = other.name ?: name,
            title = other.title ?: title,
            description = other.description ?: description,
            version = other.version ?: version,
            args = other.args.ifEmpty { args },
            template = other.template ?: template,
            contextInject = other.contextInject ?: contextInject
        )
    }
}

@JsonInclude(JsonInclude.Include.NON_DEFAULT)
data class PromptArgDef(
    /** Name of the argument. */
    val name: String,
    /** Description of the argument. */
    val description: String? = null,
    /** Whether the argument is required. */
    val required: Boolean = false,
    /** Type of argument. */
    val type: PromptArgType = PromptArgType.string,

    /** Default value for the argument. */
    val defaultValue: String? = null,
    /** Allowed values for enumeration types. */
    val allowedValues: List<String> = listOf()
)

enum class PromptArgType { string, integer, number, boolean, json, enumeration }

@JsonInclude(JsonInclude.Include.NON_DEFAULT)
data class ContextConfig(
    /** If true, set {{today}} to current ISO-8601 date. */
    var today: Boolean = true
)