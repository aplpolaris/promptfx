/*-
 * #%L
 * tri.promptfx:promptfx
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
package tri.promptfx

import com.fasterxml.jackson.module.kotlin.readValue
import tri.ai.prompt.PromptLibrary
import tri.promptfx.ui.RuntimePromptViewConfig
import tri.util.fine
import tri.util.ui.MAPPER
import tri.util.ui.NavigableWorkspaceView
import java.io.File

/** Configs for prompt apps. */
object RuntimePromptViewConfigs {

    /** Indexed cache of views configured within codebase and at runtime. */
    var viewConfigs: List<SourcedViewConfig> = listOf()
        private set
    /** Views indexed by id, with runtime configs taking precedence over built-in configs and plugins. */
    var viewIndex: Map<String, SourcedViewConfig> = mapOf()
        private set

    /** Prompts arising from views. */
    var promptLibrary = PromptLibrary()
        private set


    /** Check if the given view config is overwritten. */
    fun isOverwritten(viewConfig: SourcedViewConfig) =
        viewIndex[viewConfig.viewId] != viewConfig

    /** Reload all view configurations from their sources. */
    fun reload() {
        viewConfigs = loadViewCache()
        viewIndex = viewConfigs.byPrecedence()
        promptLibrary = PromptLibrary().apply {
            viewIndex.values.mapNotNull { it.config }.forEach {
                try {
                    addPrompt(it.prompt)
                } catch (e: Exception) {
                    fine<RuntimePromptViewConfigs>("Error adding prompt ${it.prompt.id} from view config: ${e.message}")
                }
            }
        }
    }

    init {
        reload()
    }

    //region VIEW CACHE LOADER

    /** Loads view cache from all sources. */
    private fun loadViewCache() = mutableListOf<SourcedViewConfig>().apply {
        addAll(loadPluginViews())
        addAll(loadBuiltInConfigs())
        addAll(loadRuntimeConfigs())
    }

    /** Pulls views into an index by precedence. */
    private fun List<SourcedViewConfig>.byPrecedence() = mutableMapOf<String, SourcedViewConfig>().also { map ->
        filter { it.source == RuntimeViewSource.BUILT_IN_PLUGIN }.forEach { map[it.viewId] = it }
        filter { it.source == RuntimeViewSource.BUILT_IN_CONFIG }.forEach { map[it.viewId] = it }
        filter { it.source == RuntimeViewSource.RUNTIME_PLUGIN }.forEach { map[it.viewId] = it }
        filter { it.source == RuntimeViewSource.RUNTIME_CONFIG }.forEach { map[it.viewId] = it }
    }

    /** Loads all views that are configured as part of view plugins. */
    private fun loadPluginViews(): List<SourcedViewConfig> =
        NavigableWorkspaceView.allViewPluginsWithSource.map { pluginInfo ->
            val source = when (pluginInfo.source) {
                PluginSource.EXTERNAL_JAR -> RuntimeViewSource.RUNTIME_PLUGIN
                PluginSource.BUILT_IN -> RuntimeViewSource.BUILT_IN_PLUGIN
            }
            SourcedViewConfig(viewGroup = pluginInfo.plugin.category, viewId = pluginInfo.plugin.name, view = pluginInfo.plugin, config = null, source = source)
        }

    /** Loads a set of view configs from class resource file. */
    private fun loadBuiltInConfigs(): List<SourcedViewConfig> {
        val foundIndex: Map<String, RuntimePromptViewConfig> = RuntimePromptViewConfigs::class.java.getResource("resources/views.yaml")!!
            .let { MAPPER.readValue(it) }
        return foundIndex.values.map {
            SourcedViewConfig(viewGroup = it.prompt.category!!, viewId = it.prompt.title(), null, it, RuntimeViewSource.BUILT_IN_CONFIG)
        }
    }

    /** Loads view configs from runtime files. */
    private fun loadRuntimeConfigs(): List<SourcedViewConfig> {
        val foundIndex: List<Map<String, RuntimePromptViewConfig>> = setOf(
            File("views.yaml"),
            File("config/views.yaml")
        ).filter { it.exists() }.map { MAPPER.readValue(it) }
        return foundIndex.flatMap { it.values }.map {
            SourcedViewConfig(viewGroup = it.prompt.category!!, viewId = it.prompt.title(), null,it, RuntimeViewSource.RUNTIME_CONFIG)
        }
    }

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
}

/** Tracks the YAML configuration of the view along with the source. */
class SourcedViewConfig(
    val viewGroup: String,
    val viewId: String,
    val view: NavigableWorkspaceView?,
    val config: RuntimePromptViewConfig?,
    val source: RuntimeViewSource,
) {
    init {
        require (source == RuntimeViewSource.RUNTIME_PLUGIN || source == RuntimeViewSource.BUILT_IN_PLUGIN || config != null) {
            "Config must be provided for view $viewId if source is not VIEW_PLUGIN or VIEW_PLUGIN_BUILTIN."
        }
    }
}

/** Source of a [SourcedViewConfig]. */
enum class RuntimeViewSource {
    /** View is coded with the application and loaded from the main application JAR. */
    BUILT_IN_PLUGIN,
    /** View is coded with the application and loaded as an external plugin. */
    RUNTIME_PLUGIN,
    /** VIew that is configured via YAML. */
    BUILT_IN_CONFIG,
    /** View is configured at runtime via `config/views.yaml` or another runtime file. */
    RUNTIME_CONFIG,
    /** View that is provided by the user. */
    USER_PROVIDED
}
