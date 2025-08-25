package tri.ai.pips.core

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.KotlinModule

val MAPPER: ObjectMapper = ObjectMapper().registerModule(KotlinModule.Builder().build())