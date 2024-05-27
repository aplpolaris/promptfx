package tri.promptfx

import com.fasterxml.jackson.module.kotlin.readValue
import tri.util.ui.MAPPER
import java.io.File

/** Configs for prompt apps. */
object RuntimePromptViewConfigs {

    /** Index of configs. */
    private val index: Map<String, RuntimePromptViewConfig> = RuntimePromptViewConfigs::class.java.getResource("resources/views.yaml")!!
        .let { MAPPER.readValue(it) }
    /** Index of runtime configs. */
    private val runtimeIndex: Map<String, RuntimePromptViewConfig> = setOf(
        File("views.yaml"),
        File("config/views.yaml")
    ).firstOrNull { it.exists() }?.let { MAPPER.readValue(it) } ?: mapOf()

    /** Index of modes. Key is mode group, value key is for UI presentation/selection, value is for prompt. */
    private val modes: Map<String, Map<String, String>> = RuntimePromptViewConfigs::class.java.getResource("resources/modes.yaml")!!
        .let {
            MAPPER.readValue<Map<String, Any>>(it)
                .mapValues {
                    (it.value as? List<*>)?.associate { (it as String) to it }
                        ?: (it.value as Map<String, String>)
                }
        }
    /** Runtime index of modes. */
    private val runtimeModes: Map<String, Map<String, String>> = setOf(File("modes.yaml"), File("config/modes.yaml"))
        .firstOrNull { it.exists() }?.let {
            MAPPER.readValue<Map<String, Any>>(it)
                .mapValues {
                    (it.value as? List<*>)?.associate { (it as String) to it }
                        ?: (it.value as Map<String, String>)
                }
        } ?: mapOf()

    /** Get a config by id. */
    fun config(id: String) = runtimeIndex[id] ?: index[id]!!

    /**
     * Get value of a mode id for use in prompts.
     * If [valueId] is not present for given mode, returns [valueId].
     * If [modeId] is not present in index, throws an error.
     */
    fun modeTemplateValue(modeId: String, valueId: String) =
        (runtimeModes[modeId] ?: modes[modeId])?.getOrDefault(valueId, valueId) ?: error("Mode $modeId not found in index.")

    /** Get list of options for a mode. */
    fun modeOptionList(modeId: String) = (runtimeModes[modeId] ?: modes[modeId])?.keys?.toList() ?: error("Mode $modeId not found in index.")

    /** Get options for mode, where keys are presentation values and values are prompt values. */
    fun modeOptionMap(modeId: String) = (runtimeModes[modeId] ?: modes[modeId]) ?: error("Mode $modeId not found in index.")

}