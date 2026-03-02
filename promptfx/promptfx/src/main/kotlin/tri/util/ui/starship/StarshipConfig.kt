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
package tri.util.ui.starship

import com.fasterxml.jackson.module.kotlin.readValue
import tri.ai.pips.api.PPlan
import tri.util.info
import tri.util.warning
import tri.util.json.yamlMapper
import java.io.File

/** Global Starship configuration, including the pipeline and the view config. */
class StarshipConfig() {
    var question = StarshipConfigQuestion()
    var pipeline = PPlan(null, listOf())
    var layout = StarshipConfigLayout()

    companion object {
        private val RUNTIME_CONFIG_FILES = listOf(
            File("starship-custom.yaml"), File("config/starship-custom.yaml"),
            File("starship.yaml"), File("config/starship.yaml")
        )

        /** Reads a Starship config from YAML. */
        fun readYaml(yaml: String): StarshipConfig =
            yamlMapper.readValue(yaml)

        /** Reads default Starship config. */
        fun readDefaultYaml(): StarshipConfig =
            readYaml(StarshipConfig::class.java.getResource("resources/default-starship-config.yaml")!!.readText())

        /**
         * Reads a Starship config from a runtime file if one exists, otherwise falls back to the default config.
         * Checks for `starship-custom.yaml` first, then `starship.yaml`, in the current directory and `config/` subdirectory.
         */
        fun readRuntimeYaml(): StarshipConfig {
            val configFile = RUNTIME_CONFIG_FILES.firstOrNull { it.exists() }
            if (configFile != null) {
                info<StarshipConfig>("Loading Starship config from runtime file: ${configFile.path}")
                try {
                    val config = readYaml(configFile.readText())
                    config.validate()
                    return config
                } catch (e: Exception) {
                    warning<StarshipConfig>("Failed to load runtime Starship config from ${configFile.path}: ${e.message}. Falling back to default.")
                }
            }
            return readDefaultYaml()
        }

        /** Validates that a [StarshipConfig] has the required configuration pieces, logging warnings for any missing pieces. */
        private fun StarshipConfig.validate() {
            if (pipeline.steps.isEmpty())
                warning<StarshipConfig>("Starship pipeline has no steps configured. The demo will not function correctly.")
            if (layout.widgets.isEmpty())
                warning<StarshipConfig>("Starship layout has no widgets configured. The demo display will be empty.")
            if (question.template.isBlank())
                warning<StarshipConfig>("Starship question template is blank. Random question generation may fail.")
        }
    }
}
