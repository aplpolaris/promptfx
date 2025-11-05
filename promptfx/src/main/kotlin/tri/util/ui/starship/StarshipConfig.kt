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
package tri.util.ui.starship

import com.fasterxml.jackson.module.kotlin.readValue
import tri.ai.pips.api.PPlan
import tri.util.YAML_MAPPER

/** Global Starship configuration, including the pipeline and the view config. */
class StarshipConfig() {
    var question = StarshipConfigQuestion()
    var pipeline = PPlan(null, listOf())
    var layout = StarshipConfigLayout()

    companion object {
        // TODO - update yaml formats to allow loading a runtime config
//        private val configFile = setOf(File("starship.yaml"), File("config/starship.yaml")).firstOrNull { it.exists() }
//        private val configFileAlt = setOf(File("starship-custom.yaml"), File("config/starship-custom.yaml")).firstOrNull { it.exists() }
//        private val config: Map<String, Any> = (configFileAlt ?: configFile)?.let { YAMLMapper().readValue(it) } ?: mapOf()

        /** Reads a Starship config from YAML. */
        fun readYaml(yaml: String): StarshipConfig =
            YAML_MAPPER.readValue(yaml)

        /** Reads default Starship config. */
        fun readDefaultYaml(): StarshipConfig =
            readYaml(StarshipConfig::class.java.getResource("resources/default-starship-config.yaml")!!.readText())
    }
}
