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

sealed class ReplCommand {
    // Navigation
    data class Mode(val name: String) : ReplCommand()
    data class Model(val id: String?) : ReplCommand()
    data class Provider(val name: String?) : ReplCommand()

    // Feature toggles
    data class Memory(val on: Boolean) : ReplCommand()
    data class Rag(val on: Boolean, val path: String? = null) : ReplCommand()
    data class Tools(val on: Boolean) : ReplCommand()
    data class JsonMode(val on: Boolean) : ReplCommand()

    // Sampling
    data class Temp(val value: Double) : ReplCommand()
    data class TopP(val value: Double) : ReplCommand()

    // Prompt
    data class SystemPrompt(val text: String) : ReplCommand()

    // Session
    data class Batch(val path: String) : ReplCommand()
    object Status : ReplCommand()
    object Models : ReplCommand()
    object Providers : ReplCommand()
    object Reset : ReplCommand()
    object Help : ReplCommand()
    object Quit : ReplCommand()

    // Chat input (not a slash command)
    data class Chat(val text: String) : ReplCommand()

    // Error
    data class Unknown(val input: String) : ReplCommand()
}
