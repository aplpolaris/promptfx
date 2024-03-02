package tri.util.ui

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.KotlinModule
import tornadofx.ResourceLookup

val MAPPER = ObjectMapper(YAMLFactory()).apply {
    registerModule(KotlinModule())
    registerModule(JavaTimeModule())
}

fun ResourceLookup.yaml(resource: String): Map<*, *> =
    stream(resource).use { MAPPER.readValue(it, Map::class.java) }
