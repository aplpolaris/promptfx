/*-
 * #%L
 * promptkt-0.1.0-SNAPSHOT
 * %%
 * Copyright (C) 2023 - 2024 Johns Hopkins University Applied Physics Laboratory
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
package tri.ai.core

import tri.ai.embedding.EmbeddingService
import tri.ai.openai.OpenAiTextPlugin
import tri.util.info
import java.io.File
import java.net.MalformedURLException
import java.net.URL
import java.net.URLClassLoader
import java.util.*

/** Provides a set of plugins at runtime. */
interface TextPlugin {

    /** Provide a list of chat engines. */
    fun chatModels(): List<TextChat>

    /** Provide a list of text completion engines. */
    fun textCompletionModels(): List<TextCompletion>

    /** Provide a list of embedding models. */
    fun embeddingModels(): List<EmbeddingService>

    /** Closes resources associated with the plugin. */
    fun close()

    companion object {
        private val plugins: ServiceLoader<TextPlugin> by lazy {
            ServiceLoader.load(TextPlugin::class.java, pluginsDirClassLoader())
        }
        val defaultPlugin = plugins.first { it is OpenAiTextPlugin } as OpenAiTextPlugin
        val orderedPlugins = listOf(defaultPlugin) + (plugins - defaultPlugin)

        fun textCompletionModels() = orderedPlugins.flatMap { it.textCompletionModels() }
        fun embeddingModels() = orderedPlugins.flatMap { it.embeddingModels() }
        fun chatModels() = orderedPlugins.flatMap { it.chatModels() }

        /**
         * Return a [ClassLoader] that looks for files in the "config/modules"
         * directory, in addition to the normal system class loader.
         * @return singleton instance of plugins class loader
         */
        private fun pluginsDirClassLoader(): ClassLoader? {
            // look for jar plugins
            val jars = File("config/modules/")
                .listFiles { _: File?, name: String -> name.endsWith(".jar") }
            return if (jars != null) {
                info<TextPlugin>("Discovered module jars: \n - ${jars.joinToString("\n - ")}")
                // create urls for jars
                val urls = mutableListOf<URL>()
                for (f in jars) {
                    try {
                        urls.add(f.toURI().toURL())
                    } catch (ex: MalformedURLException) {
                        // log
                    }
                }
                return URLClassLoader(urls.toTypedArray<URL>(), ClassLoader.getSystemClassLoader())
            } else {
                ClassLoader.getSystemClassLoader()
            }

            //</editor-fold>
        }
    }

}
