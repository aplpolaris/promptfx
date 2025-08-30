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

import tri.util.info
import java.util.ServiceLoader
import kotlin.io.path.Path
import kotlin.io.path.listDirectoryEntries

object PromptFxPlugins {

    private const val PLUGIN_DIR = "config/plugins/"

    /** Load plugins from config/plugins/ folder. */
    fun <C> loadPlugins(type: Class<C>): List<C> {
        val pluginFiles = listOf(Path(PLUGIN_DIR))
            .flatMap { it.listDirectoryEntries("*.jar") }
        if (pluginFiles.isNotEmpty()) {
            val urls = pluginFiles.map { it.toUri().toURL() }.toTypedArray()
            info<PromptFxPlugins>("Loading plugins from $PLUGIN_DIR - " + pluginFiles.joinToString(", ") { it.fileName.toString() })
            val loader = java.net.URLClassLoader(urls, type.classLoader)
            val serviceLoader = ServiceLoader.load(type, loader)
            return mutableListOf<C>().apply {
                serviceLoader.forEach { add(it) }
                info<PromptFxPlugins>("Loaded $size ${type.simpleName} plugins from config/plugins/ jar files.")
            }
        } else {
            info<PromptFxPlugins>("No ${type.simpleName} plugin jar files found in $PLUGIN_DIR.")
        }
        return listOf()
    }
}
