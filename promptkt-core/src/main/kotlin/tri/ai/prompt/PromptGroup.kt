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

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonInclude

/**
 * Represents a group of prompts with associated metadata.
 * A prompt group should be a set of related prompts with a common category or theme.
 * Groups can be easily defined within JSON or YAML files.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
data class PromptGroup(
    val groupId: String = "Uncategorized",
    val version: String? = null,
    val defaults: PromptGroupDefaults = PromptGroupDefaults(),
    val prompts: List<PromptDef> = emptyList()
)

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
data class PromptGroupDefaults(
    var category: String? = null,
    val tags: List<String> = emptyList()
)

//region RESOLVING DEFAULTS

/** Resolve all prompts in the group, applying defaults from the group where needed. */
fun PromptGroup.resolved(): PromptGroup {
    defaults.category = defaults.category ?: groupId
    val resolvedPrompts = prompts.map { it.resolved(this) }
    return copy(prompts = resolvedPrompts)
}

/** Resolve a single prompt definition, applying defaults from the group and inferring name/version/category from the id. */
fun PromptDef.resolved(group: PromptGroup): PromptDef {
    val resolvedCategory = category ?: id.substringBefore('/', "").ifBlank { null } ?: group.defaults.category ?: "Uncategorized"
    val resolvedName = name ?: id.substringAfter('/').substringBefore('@')
    val resolvedVersion = version ?: id.substringAfter('@', "").ifBlank { null } ?: group.version ?: "0.0.1"
    val resolvedTags = (group.defaults.tags + tags).distinct()
    val resolvedArgs = args.ifEmpty { generateArgs() }
    return copy(category = resolvedCategory, name = resolvedName, tags = resolvedTags, args = resolvedArgs, version = resolvedVersion)
}

/** Generates arguments from the template. */
fun PromptDef.generateArgs(): List<PromptArgDef> {
    val fields = PromptTemplate(template!!).findFields()
    return fields.map { PromptArgDef(it) }
}

//endregion
