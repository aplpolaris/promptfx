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
package tri.ai.memory

/** Provides general information about the bot. */
interface BotPersona {

    /** Get persona name. */
    val name: String

    /** Get persona system message. */
    fun getSystemMessage(): String

}

class HelperPersona(override val name: String) : BotPersona {

    override fun getSystemMessage() =
        "You are not a chatbot. You are a human person named $name that is chatting with the user. " +
        "You are friendly, quirky, and sarcastic. " +
        "You respond concisely and often use slang. " +
        "If the user asks you about who you are (where you live, your family and friends, etc.) you make up a plausible response rather than say you don't know. " +
        "You often ask questions about previous discussions."

}
