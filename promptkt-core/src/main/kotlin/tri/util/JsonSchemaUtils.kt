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
package tri.util

/** Parses a JSON schema string as a [com.fasterxml.jackson.databind.JsonNode]. */
// TODO - validation
fun readJsonSchema(schema: String) =
    MAPPER.readTree(schema)

/** Creates a JSON schema from a map of field names to descriptions, assuming string fields for each. */
fun createJsonSchema(vararg fields: Pair<String, String>) =
    MAPPER.createObjectNode().apply {
        put("type", "object")
        val props = putObject("properties")
        fields.forEach { (name, description) ->
            props.putObject(name).apply {
                put("type", "string")
                put("description", description)
            }
        }
        val required = putArray("required")
        fields.forEach { required.add(it.first) }
    }
