package tri.ai.core

import tri.ai.embedding.EmbeddingService
import tri.ai.openai.OpenAiTextPlugin
import java.util.*

/** Provides a set of plugins at runtime. */
interface TextPlugin {

    /** Provide a list of chat engines. */
    fun chatModels(): List<TextChat>

    /** Provide a list of text completion engines. */
    fun textCompletionModels(): List<TextCompletion>

    /** Provide a list of embedding models. */
    fun embeddingModels(): List<EmbeddingService>

    companion object {
        private val plugins: ServiceLoader<TextPlugin> by lazy { ServiceLoader.load(TextPlugin::class.java) }
        val defaultPlugin = plugins.first { it is OpenAiTextPlugin } as OpenAiTextPlugin
        val orderedPlugins = listOf(defaultPlugin) + (plugins - defaultPlugin)

        fun textCompletionModels() = orderedPlugins.flatMap { it.textCompletionModels() }
        fun embeddingModels() = orderedPlugins.flatMap { it.embeddingModels() }
        fun chatModels() = orderedPlugins.flatMap { it.chatModels() }
    }

}