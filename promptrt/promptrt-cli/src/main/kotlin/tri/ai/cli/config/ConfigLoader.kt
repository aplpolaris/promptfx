/*-
 * #%L
 * tri.promptfx:promptrt
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
package tri.ai.cli.config

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.PropertyNamingStrategies
import com.fasterxml.jackson.dataformat.yaml.YAMLMapper
import com.fasterxml.jackson.module.kotlin.kotlinModule
import com.fasterxml.jackson.module.kotlin.readValue
import java.io.File

object ConfigLoader {
    private val mapper = YAMLMapper().apply {
        registerModule(kotlinModule())
        configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
        // snake_case YAML keys map to camelCase Kotlin fields
        setPropertyNamingStrategy(PropertyNamingStrategies.SNAKE_CASE)
    }

    fun fromYaml(yaml: String): PromptRtConfig = mapper.readValue(yaml)

    fun load(file: File): PromptRtConfig =
        if (file.exists()) fromYaml(file.readText()) else PromptRtConfig()

    fun loadDefault(): PromptRtConfig =
        load(File(System.getProperty("user.home"), ".promptrt/config.yaml"))
}
