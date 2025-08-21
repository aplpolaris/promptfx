/*-
 * #%L
 * tri.promptfx:promptfx
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
package tri.promptfx

import com.fasterxml.jackson.module.kotlin.readValue
import tri.ai.prompt.PromptLibrary
import tri.promptfx.ui.RuntimePromptViewConfig
import tri.promptfx.ui.RuntimePromptViewConfigMcp
import tri.util.fine
import tri.util.ui.MAPPER
import java.io.File

/** Configs for prompt apps. */
object RuntimePromptViewConfigs {

    //region VIEW CONFIGS

    /** Index of configs. */
    private val index: Map<String, RuntimePromptViewConfig> = RuntimePromptViewConfigs::class.java.getResource("resources/views.yaml")!!
        .let { MAPPER.readValue(it) }
    /** Index of runtime configs. */
    private val runtimeIndex: Map<String, RuntimePromptViewConfig> = setOf(
        File("views.yaml"),
        File("config/views.yaml")
    ).firstOrNull { it.exists() }?.let { MAPPER.readValue(it) } ?: mapOf()

    /** All views by name. The runtime configs override the resource configs. */
    val views: Map<String, RuntimePromptViewConfig> by lazy {
        mutableMapOf<String, RuntimePromptViewConfig>().apply {
            putAll(index)
            putAll(runtimeIndex)
            runtimeIndex.keys.intersect(index.keys).forEach {
                fine<RuntimePromptViewConfigs>("Runtime config for prompt view $it overrides resource config.")
            }
        }
    }

    /** Get a list of categories. */
    fun categories() = (index.values + runtimeIndex.values).map { it.prompt.category ?: "Uncategorized" }.distinct()
    /** Get a list of configs by category. */
    fun configs(category: String) = (index.values + runtimeIndex.values).filter { it.prompt.category == category }
    /** Get a config by id. */
    fun config(id: String) = runtimeIndex[id] ?: index[id]!!

    //endregion

    //region MCP VIEWS

    /** Index of configs. */
    private val mcpIndex: Map<String, RuntimePromptViewConfigMcp> = RuntimePromptViewConfigs::class.java.getResource("resources/views-mcp.yaml")!!
        .let { MAPPER.readValue(it) }
    /** Index of runtime configs. */
    private val mcpRuntimeIndex: Map<String, RuntimePromptViewConfigMcp> = setOf(
        File("views-mcp.yaml"),
        File("config/views-mcp.yaml")
    ).firstOrNull { it.exists() }?.let { MAPPER.readValue(it) } ?: mapOf()

    val mcpViews: Map<String, RuntimePromptViewConfigMcp> by lazy {
        mutableMapOf<String, RuntimePromptViewConfigMcp>().apply {
            putAll(mcpIndex)
            putAll(mcpRuntimeIndex)
            mcpRuntimeIndex.keys.intersect(mcpIndex.keys).forEach {
                fine<RuntimePromptViewConfigs>("Runtime config for MCP prompt view $it overrides resource config.")
            }
        }
    }

    /** Get a list of categories. */
    fun mcpCategories() = (mcpIndex.values + mcpRuntimeIndex.values).map { it.category }.distinct()
    /** Get a list of configs by category. */
    fun mcpConfigs(category: String) = (mcpIndex.values + mcpRuntimeIndex.values).filter { it.category == category }
    /** Get a config by id. */
    fun mcpConfig(id: String) = mcpRuntimeIndex[id] ?: mcpIndex[id]!!

    //endregion

    //region MODES

    /** Index of modes. Key is mode group, value key is for UI presentation/selection, value is for prompt. */
    internal val modes: Map<String, Map<String, String>> = RuntimePromptViewConfigs::class.java.getResource("resources/modes.yaml")!!
        .let { MAPPER.readValue<Map<String, Any>>(it).parseModes() }
    /** Runtime index of modes. */
    internal val runtimeModes: Map<String, Map<String, String>> = setOf(File("modes.yaml"), File("config/modes.yaml"))
        .firstOrNull { it.exists() }?.let { MAPPER.readValue<Map<String, Any>>(it).parseModes() } ?: mapOf()

    private fun Map<String, Any>.parseModes() = mapValues {
        (it.value as? List<*>)?.associate { (it as String) to it }
            ?: (it.value as Map<String, String>)
    }

    /**
     * Get value of a mode id for use in prompts.
     * If [modeId] is not present in index, throws an error.
     * If [valueId] is not present for given mode, returns [valueId].
     */
    fun modeTemplateValue(modeId: String, valueId: String) =
        (runtimeModes[modeId] ?: modes[modeId])?.getOrDefault(valueId, valueId) ?: error("Mode $modeId not found in index.")

    /** Get list of options for a mode. */
    fun modeOptionList(modeId: String) = (runtimeModes[modeId] ?: modes[modeId])?.keys?.toList() ?: error("Mode $modeId not found in index.")

    /** Get options for mode, where keys are presentation values and values are prompt values. */
    fun modeOptionMap(modeId: String) = (runtimeModes[modeId] ?: modes[modeId]) ?: error("Mode $modeId not found in index.")

    //endregion

    val PROMPT_LIBRARY by lazy {
        PromptLibrary().apply {
            views.values.forEach {
                try {
                    addPrompt(it.prompt)
                } catch (e: Exception) {
                    fine<RuntimePromptViewConfigs>("Error adding prompt ${it.prompt.id} from view config: ${e.message}")
                }
            }
        }
    }

}