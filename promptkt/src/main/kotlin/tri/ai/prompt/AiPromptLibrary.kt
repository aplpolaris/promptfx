package tri.ai.prompt

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.KotlinModule
import com.fasterxml.jackson.module.kotlin.readValue
import java.io.File
import kotlin.reflect.KClass

/** Library of prompt templates. */
class AiPromptLibrary {
    var prompts = mutableMapOf<String, AiPrompt>()

    companion object {
        fun lookupPrompt(id: String): AiPrompt {
            return INSTANCE.prompts[id] ?: throw IllegalArgumentException("No prompt found with id $id")
        }

        val INSTANCE by lazy {
            AiPromptLibrary().apply {
                prompts.putAll(AiPromptLibrary::class.yaml<Map<String, AiPrompt>>("resources/prompts.yaml"))
                val file = File("prompts.yaml")
                if (file.exists())
                    prompts.putAll(MAPPER.readValue<Map<String, AiPrompt>>(file))
            }
        }

        val MAPPER = ObjectMapper(YAMLFactory()).apply {
            registerModule(KotlinModule())
            registerModule(JavaTimeModule())
        }

        inline fun <reified X> KClass<*>.yaml(resource: String) =
            java.getResourceAsStream(resource).use { MAPPER.readValue<X>(it!!) }
    }
}