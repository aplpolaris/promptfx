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
package tri.util.json

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.ObjectWriter
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.KotlinModule

/** Shared JSON mapper configured with Kotlin and Java time modules. */
val jsonMapper = ObjectMapper()
    .registerModule(JavaTimeModule())
    .registerModule(KotlinModule.Builder().build())
    .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)!!

/** Shared YAML mapper configured with Kotlin and Java time modules. */
val yamlMapper = ObjectMapper(YAMLFactory())
    .registerModule(JavaTimeModule())
    .registerModule(KotlinModule.Builder().build())
    .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)!!

/** Shared JSON writer with pretty printing. */
val jsonWriter: ObjectWriter = jsonMapper.writerWithDefaultPrettyPrinter()

/** Shared YAML writer with pretty printing. */
val yamlWriter: ObjectWriter = yamlMapper.writerWithDefaultPrettyPrinter()
