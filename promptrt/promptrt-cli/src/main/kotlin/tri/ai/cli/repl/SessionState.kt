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
package tri.ai.cli.repl

import tri.ai.cli.DocumentQaConfig
import tri.ai.cli.config.ModePreset
import tri.ai.cli.config.PromptRtConfig
import tri.ai.cli.createQaDriver
import tri.ai.core.AiModelProvider
import tri.ai.core.TextChatMessage
import tri.ai.memory.BotMemory
import tri.ai.memory.BotPersona
import tri.ai.memory.HelperPersona
import tri.ai.text.docs.LocalDocumentQaDriver
import kotlin.io.path.Path

/**
 * Mutable live state for the duration of a REPL session.
 *
 * - [applyModelOverride] overrides the model WITHOUT clearing history.
 * - [switchMode] switches the full preset AND clears history (and the model override).
 * - [reset] restores the default mode from config and clears all overrides.
 * - [effectiveModel] prefers [modelOverride] over the mode's model.
 */
class SessionState private constructor(
    var activeMode: ModePreset,
    var modelOverride: String?,
    var memoryEnabled: Boolean,
    var ragEnabled: Boolean,
    var ragPath: String?,
    var toolsEnabled: Boolean,
    var streamEnabled: Boolean,
    var jsonMode: Boolean,
    var systemPrompt: String?,
    var temperature: Double,
    var topP: Double?,
    var seed: Int?,
    val history: MutableList<TextChatMessage>
) {
    var botMemory: BotMemory? = null
    var ragDriver: LocalDocumentQaDriver? = null

    val effectiveModel: String
        get() = modelOverride ?: activeMode.resolvedModel

    fun applyModelOverride(id: String) {
        modelOverride = id
    }

    fun getOrCreateRagDriver(): LocalDocumentQaDriver? {
        val path = ragPath ?: return null
        if (ragDriver == null) {
            val root = java.io.File(path).absoluteFile
            ragDriver = createQaDriver(DocumentQaConfig(
                root = root.toPath(),
                folder = "",
                chatModel = effectiveModel,
                embeddingModel = AiModelProvider.embeddingModels().firstOrNull()?.modelId,
                temp = temperature,
                maxTokens = 2000,
                templateId = null
            ))
        }
        return ragDriver
    }

    fun getOrCreateMemory(persona: BotPersona = HelperPersona("Assistant")): BotMemory {
        if (botMemory == null) {
            val chatModel = AiModelProvider.chatModels().first { it.modelId == effectiveModel }
            val embeddingModel = AiModelProvider.embeddingModels().first()
            botMemory = BotMemory(persona, chatModel, embeddingModel)
            botMemory!!.initMemory()
        }
        return botMemory!!
    }

    fun switchMode(preset: ModePreset) {
        activeMode = preset
        modelOverride = null
        memoryEnabled = preset.memoryOn
        ragEnabled = preset.ragOn
        ragPath = preset.ragPath
        toolsEnabled = preset.toolsOn
        streamEnabled = preset.streamOn
        systemPrompt = preset.system
        history.clear()
        botMemory = null
        ragDriver?.close()
        ragDriver = null
    }

    fun reset(config: PromptRtConfig) {
        switchMode(config.resolveMode(config.defaultMode))
    }

    companion object {
        fun fromConfig(config: PromptRtConfig): SessionState {
            val mode = config.resolveMode(config.defaultMode)
            return SessionState(
                activeMode = mode,
                modelOverride = null,
                memoryEnabled = mode.memoryOn,
                ragEnabled = mode.ragOn,
                ragPath = mode.ragPath,
                toolsEnabled = mode.toolsOn,
                streamEnabled = mode.streamOn,
                jsonMode = false,
                systemPrompt = mode.system,
                temperature = 0.7,
                topP = null,
                seed = null,
                history = mutableListOf()
            )
        }
    }
}
