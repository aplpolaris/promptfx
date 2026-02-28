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
package tri.ai.core

import tri.util.info
import java.io.File
import java.net.MalformedURLException
import java.net.URL
import java.net.URLClassLoader
import java.util.*

/** Provides a set of plugins at runtime. */
interface TextPlugin {

    /** Check if the plugin API has been configured, e.g. with proper API key. */
    fun isApiConfigured(): Boolean

    /** Model source for this plugin. */
    fun modelSource(): String
    /** Provide a list of model information. */
    fun modelInfo(): List<ModelInfo>

    /** Provide a list of embedding models. */
    fun embeddingModels(): List<EmbeddingModel>

    /** Provide a list of chat engines. */
    fun chatModels(): List<TextChat>

    /** Provide a list of multimodal models. */
    fun multimodalModels(): List<MultimodalChat>

    /** Provide a list of text completion engines. */
    fun textCompletionModels(): List<TextCompletion>

    /** Provide a list of vision language models.
     * @deprecated Use [multimodalModels] instead. */
    @Deprecated("Use multimodalModels() instead", ReplaceWith("multimodalModels()"))
    fun visionLanguageModels(): List<VisionLanguageChat> = emptyList()

    /** Provide a list of image generators. */
    fun imageGeneratorModels(): List<ImageGenerator>

    /** Closes resources associated with the plugin. */
    fun close()

    companion object {
        /** [ClassLoader] that should be used to load plugins. Can override to change plugin loading behavior. */
        var customPluginLoader: ClassLoader? = null

        private val pluginLoader: ClassLoader by lazy {
            customPluginLoader ?: pluginsDirClassLoader() ?: Thread.currentThread().contextClassLoader
        }
        private val plugins: ServiceLoader<TextPlugin> by lazy {
            ServiceLoader.load(TextPlugin::class.java, pluginLoader)
        }

        val defaultPlugin by lazy { 
            plugins.firstOrNull { it.modelSource() == "OpenAI" } 
                ?: plugins.firstOrNull() 
                ?: throw IllegalStateException("No text plugins found")
        }
        val orderedPlugins by lazy { 
            val default = defaultPlugin
            listOf(default) + (plugins.toList() - default)
        }

        /** Get all model sources. */
        fun sources() = orderedPlugins.map { it.modelSource() }
        /** Get all registered model info. */
        fun modelInfo() = orderedPlugins.flatMap { it.modelInfo() }

        /** Get registered embedding models. */
        fun embeddingModels() = orderedPlugins.flatMap { it.embeddingModels() }
        /** Get registered text completion models. */
        fun textCompletionModels() = orderedPlugins.flatMap { it.textCompletionModels() }
        /** Get registered chat models. */
        fun chatModels() = orderedPlugins.flatMap { it.chatModels() }
        /** Get registered multimodal models. */
        fun multimodalModels() = orderedPlugins.flatMap { it.multimodalModels() }
        /** Get registered vision language models.
         * @deprecated Use [multimodalModels] instead. */
        @Deprecated("Use multimodalModels() instead", ReplaceWith("multimodalModels()"))
        fun visionLanguageModels() = orderedPlugins.flatMap { it.visionLanguageModels() }
        /** Get registered image models. */
        fun imageGeneratorModels() = orderedPlugins.flatMap { it.imageGeneratorModels() }

        /** Get an embedding model by id. Throws an exception if not found. */
        fun embeddingModel(modelId: String) =
            embeddingModels().first { it.matchesModelId(modelId) }
        /** Get a text completion model by id. Throws an exception if not found. */
        fun textCompletionModel(modelId: String) =
            textCompletionModels().first { it.matchesModelId(modelId) }
        /** Get a chat model by id. Throws an exception if not found. */
        fun chatModel(modelId: String) =
            chatModels().first { it.matchesModelId(modelId) }
        /** Get a multimodal model by id. Throws an exception if not found. */
        fun multimodalModel(modelId: String) =
            multimodalModels().first { it.matchesModelId(modelId) }
        /** Get a vision language model by id. Throws an exception if not found.
         * @deprecated Use [multimodalModel] instead. */
        @Deprecated("Use multimodalModel() instead", ReplaceWith("multimodalModel(modelId)"))
        fun visionLanguageModel(modelId: String) =
            visionLanguageModels().first { it.matchesModelId(modelId) }
        /** Get an image model by id. Throws an exception if not found. */
        fun imageGeneratorModel(modelId: String) =
            imageGeneratorModels().first { it.matchesModelId(modelId) }

        /**
         * Parse a model identifier string, which may be in the form "modelId [source]" or just "modelId".
         * Returns a pair of (modelId, modelSource), where modelSource may be empty.
         */
        fun parseModelId(id: String): Pair<String, String> {
            val sourceMatch = Regex("""^(.*)\s+\[(.+)]$""").matchEntire(id)
            return if (sourceMatch != null) {
                sourceMatch.groupValues[1] to sourceMatch.groupValues[2]
            } else {
                id to ""
            }
        }

        private fun AiModel.matchesModelId(id: String): Boolean {
            val (parsedId, parsedSource) = parseModelId(id)
            return modelId == parsedId && (parsedSource.isEmpty() || modelSource == parsedSource)
        }

        /**
         * Return a [ClassLoader] that looks for files in the "config/plugins"
         * directory, in addition to the normal system class loader.
         * @return singleton instance of plugins class loader
         */
        private fun pluginsDirClassLoader(): ClassLoader? {
            val jars = File("config/plugins/")
                .listFiles { _: File?, name: String -> name.endsWith(".jar") }
            return if (jars != null) {
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
