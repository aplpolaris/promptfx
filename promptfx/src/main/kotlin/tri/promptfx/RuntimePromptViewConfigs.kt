package tri.promptfx

import com.fasterxml.jackson.module.kotlin.readValue
import tri.util.ui.MAPPER

/** Configs for prompt apps. */
object RuntimePromptViewConfigs {

    /** Index of configs. */
    private val index: Map<String, RuntimePromptViewConfig> = RuntimePromptViewConfigs::class.java.getResource("resources/views.yaml")!!
        .let { MAPPER.readValue(it) }

    /** Index of modes. Key is mode group, value key is for UI presentation/selection, value is for prompt. */
    val modes: Map<String, Map<String, String>> = RuntimePromptViewConfigs::class.java.getResource("resources/modes.yaml")!!
        .let {
            MAPPER.readValue<Map<String, Any>>(it)
                .mapValues {
                    (it.value as? List<*>)?.associate { (it as String) to it }
                        ?: (it.value as Map<String, String>)
                }
        }

    /** Get a config by id. */
    fun config(id: String) = index[id]!!

    /**
     * Get value of a mode id for use in prompts.
     * If [valueId] is not present for given mode, returns [valueId].
     * If [modeId] is not present in index, throws an error.
     */
    fun modeTemplateValue(modeId: String, valueId: String) =
        modes[modeId]?.getOrDefault(valueId, valueId) ?: error("Mode $modeId not found in index.")

}