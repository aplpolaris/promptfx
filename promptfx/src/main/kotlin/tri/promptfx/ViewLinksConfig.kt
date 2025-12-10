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
import tri.util.ui.MAPPER
import java.io.File

/** Configuration for view hyperlinks. */
object ViewLinksConfig {

    /** Links indexed by category (e.g., "API", "MCP"). */
    var links: Map<String, List<LinkGroup>> = mapOf()
        private set

    init {
        reload()
    }

    /** Reload links from resources and runtime files. */
    fun reload() {
        val builtInLinks = loadBuiltInLinks()
        val runtimeLinks = loadRuntimeLinks()
        
        // Merge links, with runtime links taking precedence
        links = (builtInLinks.keys + runtimeLinks.keys).associateWith { category ->
            val builtin = builtInLinks[category] ?: emptyList()
            val runtime = runtimeLinks[category] ?: emptyList()
            builtin + runtime
        }
    }

    /** Load built-in links from resources. */
    private fun loadBuiltInLinks(): Map<String, List<LinkGroup>> {
        return try {
            ViewLinksConfig::class.java.getResource("/tri/promptfx/resources/views-links.yaml")
                ?.let { MAPPER.readValue(it) } ?: emptyMap()
        } catch (e: Exception) {
            tri.util.warning<ViewLinksConfig>("Failed to load built-in view links: ${e.message}")
            emptyMap()
        }
    }

    /** Load runtime links from config files. */
    private fun loadRuntimeLinks(): Map<String, List<LinkGroup>> {
        val runtimeFiles = setOf(
            File("views-links.yaml"),
            File("config/views-links.yaml")
        )
        return runtimeFiles
            .filter { it.exists() }
            .flatMap { file ->
                try {
                    val loaded: Map<String, List<LinkGroup>> = MAPPER.readValue(file)
                    loaded.entries
                } catch (e: Exception) {
                    tri.util.warning<ViewLinksConfig>("Failed to load runtime view links from ${file.path}: ${e.message}")
                    emptyList()
                }
            }
            .groupBy({ it.key }, { it.value })
            .mapValues { it.value.flatten() }
    }
}

/** A group of related links. */
data class LinkGroup(
    val group: String,
    val links: List<HyperLink>
)

/** A single hyperlink. */
data class HyperLink(
    val label: String,
    val url: String
)
