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
package tri.ai.prompt.trace

import com.fasterxml.jackson.annotation.JsonInclude
import tri.ai.core.MultimodalChatMessage
import tri.ai.core.TextChatMessage

/** Container for the outputs of an AI task, holding a list of typed [AiOutput] values. */
@JsonInclude(JsonInclude.Include.NON_DEFAULT)
data class AiOutputInfo(
    var outputs: List<AiOutput>
) {
    /** Convert output using a provided function. */
    fun map(transform: (AiOutput) -> AiOutput) =
        AiOutputInfo(outputs.map(transform))

    /** Convert list of output to a single output. */
    fun reduce(transform: (List<AiOutput>) -> AiOutput) =
        AiOutputInfo(listOf(transform(outputs)))

    companion object {
        fun output(output: AiOutput) = AiOutputInfo(listOf(output))
        fun output(outputs: List<AiOutput>) = AiOutputInfo(outputs)

        fun text(text: String) = output(AiOutput.Text(text))
        fun text(texts: List<String>) = output(texts.map { AiOutput.Text(it) })

        fun message(message: TextChatMessage) = output(AiOutput.ChatMessage(message))
        fun messages(messages: List<TextChatMessage>) = output(messages.map { AiOutput.ChatMessage(it) })

        fun multimodalMessage(message: MultimodalChatMessage) = output(AiOutput.MultimodalMessage(message))
        fun multimodalMessages(messages: List<MultimodalChatMessage>) = output(messages.map { AiOutput.MultimodalMessage(it) })

        /** Return an output where the entire list is stored as a single [AiOutput.Other] output. */
        fun <T: Any> listSingleOutput(items: List<T>) = output(AiOutput.Other(items))

        /** Accepts any object type, automatically selecting the appropriate [AiOutput] subtype. */
        fun other(content: Any) = when (content) {
            is AiOutput -> output(content)
            is String -> text(content)
            is TextChatMessage -> message(content)
            is MultimodalChatMessage -> multimodalMessage(content)
            is List<*> -> error("use `listSingleOutput` for lists, or map to multiple outputs")
            else -> AiOutputInfo(listOf(AiOutput.Other(content)))
        }
    }
}
