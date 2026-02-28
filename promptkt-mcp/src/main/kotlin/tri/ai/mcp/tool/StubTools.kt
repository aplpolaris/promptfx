/*-
 * #%L
 * tri.promptfx:promptkt
 * %%
 * Copyright (C) 2023 - 2026 Johns Hopkins University Applied Physics Laboratory
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
package tri.ai.mcp.tool

import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.module.kotlin.convertValue
import com.fasterxml.jackson.module.kotlin.readValue
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.buildJsonObject
import tri.ai.core.tool.ExecContext
import tri.ai.core.tool.Executable
import tri.ai.mcp.JsonSerializers.toJsonElement
import tri.ai.mcp.JsonSerializers.toJsonNode
import tri.util.json.jsonMapper

/** A tool that returns a fixed result, useful for testing. */
data class StubTool(
    override val name: String,
    override val description: String,
    override val version: String = "1.0.0",
    override val inputSchema: JsonNode, // required for Claude Desktop to work
    override val outputSchema: JsonNode // required for Claude Desktop to work
) : Executable {

    constructor(
        name: String,
        description: String,
        version: String = "1.0.0",
        inputSchema: Any,
        outputSchema: Any,
        @JsonProperty("hardCodedOutput") hardCodedOutput: Any
    ) : this(
        name, description, version,
        jsonMapper.convertValue(inputSchema),
        jsonMapper.convertValue(outputSchema)
    ) {
        this.hardCodedOutput = (hardCodedOutput as? JsonElement) ?: hardCodedOutput.toJsonElement()
    }

    private var hardCodedOutput: JsonElement = buildJsonObject { }

    @get:JsonIgnore
//    @get:JsonProperty("hardCodedOutput")
    private var hardCodedNode: JsonNode
        get() = hardCodedOutput.toJsonNode()
        set(value) {
            hardCodedOutput = value.toJsonElement()
        }

    override suspend fun execute(input: JsonNode, context: ExecContext) =
        hardCodedOutput.toJsonNode()

    companion object {
        /** Loads some stub tools from resources/stub-tools.json */
        fun loadFromResources(): List<StubTool> {
            val resource = this::class.java.getResource("resources/stub-tools.json")!!
            val loadedTools = jsonMapper.readValue<Map<String, List<StubTool>>>(resource)
            return loadedTools.values.flatten()
        }
    }

}
