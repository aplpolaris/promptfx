/*-
 * #%L
 * tri.promptfx:promptfx
 * %%
 * Copyright (C) 2023 - 2024 Johns Hopkins University Applied Physics Laboratory
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
package tri.promptfx

/**
 * Global manager for models available within PromptFx.
 * Model availability is determined by the current [PromptFxPolicy].
 */
object PromptFxModels {
    val policy: PromptFxPolicy = PromptFxPolicyUnrestricted
//    val policy: PromptFxPolicy = PromptFxPolicyOpenAi

    fun textCompletionModels() = policy.textCompletionModels()
    fun textCompletionModelDefault() = policy.textCompletionModelDefault()
    fun embeddingModels() = policy.embeddingModels()
    fun embeddingModelDefault() = policy.embeddingModelDefault()
    fun chatModels() = policy.chatModels()
    fun chatModelDefault() = policy.chatModelDefault()
//    fun imageModels() = policy.imageModels()
//    fun imageModelDefault() = policy.imageModelDefault()
}
