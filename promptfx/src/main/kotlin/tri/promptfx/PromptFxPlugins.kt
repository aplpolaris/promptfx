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
package tri.promptfx

import tri.util.fine
import tri.util.info
import java.util.ServiceLoader
import kotlin.io.path.Path
import kotlin.io.path.listDirectoryEntries

/** Information about a plugin and its source. */
data class PluginInfo<T>(
    val plugin: T,
    val source: PluginSource
)

/** Source type for a plugin. */
enum class PluginSource {
    BUILT_IN, // Plugin from the main application JAR
    EXTERNAL_JAR // Plugin loaded from config/plugins/ JAR files
}

object PromptFxPlugins {

    private const val PLUGIN_DIR = "config/plugins/"

    /** Load all plugins including built-in and external plugins with source information. */
    fun <C> loadAllPluginsWithSource(type: Class<C>): List<PluginInfo<C>> {
        val builtInPlugins = loadBuiltInPlugins(type)
        val builtInPluginTypes = builtInPlugins.map { it.plugin!!::class.java }
        val externalPlugins = loadExternalPlugins(type).filter { it.plugin!!::class.java !in builtInPluginTypes }
        fine<PromptFxPlugins>("Found ${builtInPlugins.size} built-in and ${externalPlugins.size} external ${type.simpleName} plugins.")
        return builtInPlugins + externalPlugins
    }

    /** Load plugins from the main application classpath. */
    fun <C> loadBuiltInPlugins(type: Class<C>): List<PluginInfo<C>> {
        val serviceLoader = ServiceLoader.load(type)
        return mutableListOf<PluginInfo<C>>().apply {
            serviceLoader.forEach { add(PluginInfo(it, PluginSource.BUILT_IN)) }
        }
    }

    /** Load plugins from config/plugins/ folder. */
    fun <C> loadExternalPlugins(type: Class<C>): List<PluginInfo<C>> {
        val pluginFiles = listOf(Path(PLUGIN_DIR))
            .flatMap { it.listDirectoryEntries("*.jar") }
        if (pluginFiles.isNotEmpty()) {
            val urls = pluginFiles.map { it.toUri().toURL() }.toTypedArray()
            info<PromptFxPlugins>("Loading plugins from $PLUGIN_DIR")
            pluginFiles.forEach { info<PromptFxPlugins>("  - ${it.fileName}") }
            val loader = java.net.URLClassLoader(urls, type.classLoader)
            val serviceLoader = ServiceLoader.load(type, loader)
            return mutableListOf<PluginInfo<C>>().apply {
                serviceLoader.forEach { add(PluginInfo(it, PluginSource.EXTERNAL_JAR)) }
            }
        } else {
            info<PromptFxPlugins>("No ${type.simpleName} plugin jar files found in $PLUGIN_DIR.")
        }
        return listOf()
    }

}
