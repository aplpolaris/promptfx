package tri.util.ui.starship

import com.fasterxml.jackson.module.kotlin.readValue
import tri.ai.core.agent.YAML_MAPPER
import tri.ai.pips.api.PPlan

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