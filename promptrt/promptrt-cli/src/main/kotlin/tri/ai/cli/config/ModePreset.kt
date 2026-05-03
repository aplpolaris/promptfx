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

data class ModePreset(
    val name: String = "plain",
    val model: String? = null,
    val provider: String? = null,
    val memory: Boolean? = null,
    val rag: Boolean? = null,
    val ragPath: String? = null,
    val tools: Boolean? = null,
    val system: String? = null
) {
    fun mergedOnto(base: ModePreset): ModePreset = ModePreset(
        name = name,
        model = model ?: base.model,
        provider = provider ?: base.provider,
        memory = memory ?: base.memory,
        rag = rag ?: base.rag,
        ragPath = ragPath ?: base.ragPath,
        tools = tools ?: base.tools,
        system = system ?: base.system
    )

    // Resolved (non-null) accessors — only valid after mergedOnto(PLAIN)
    val resolvedModel get() = model ?: BuiltInModes.PLAIN.model!!
    val resolvedProvider get() = provider ?: BuiltInModes.PLAIN.provider!!
    val memoryOn get() = memory ?: false
    val ragOn get() = rag ?: false
    val toolsOn get() = tools ?: false
}

object BuiltInModes {
    val PLAIN = ModePreset(
        name = "plain",
        model = "gpt-4o-mini",
        provider = "OpenAI",
        memory = false,
        rag = false,
        tools = false,
        system = null
    )
    val MEMORY = ModePreset(name = "memory", model = "gpt-4o-mini", memory = true)
    val RAG    = ModePreset(name = "rag",    model = "gpt-4o",      rag = true)
    val AGENT  = ModePreset(name = "agent",  model = "gpt-4o",      memory = true, tools = true,
                             system = "You are a helpful assistant with access to tools.")

    val all: Map<String, ModePreset> = mapOf("plain" to PLAIN, "memory" to MEMORY, "rag" to RAG, "agent" to AGENT)
        .mapValues { (_, v) -> v.mergedOnto(PLAIN) }
}
