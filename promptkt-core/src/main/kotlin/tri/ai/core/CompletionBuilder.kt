/*-
 * #%L
 * tri.promptfx:promptkt
 * %%
 * Copyright (C) 2023 - 2025 Johns Hopkins University Applied Physics Laboratory
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
package tri.ai.core

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.JsonMappingException
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.KotlinModule
import com.fasterxml.jackson.module.kotlin.readValue
import tri.ai.prompt.PromptDef
import tri.ai.prompt.PromptTemplate
import tri.ai.prompt.PromptTemplate.Companion.defaultInputParams
import tri.ai.prompt.PromptTemplate.Companion.defaultInstructParams
import tri.ai.prompt.template
import tri.ai.prompt.trace.AiPromptTrace
import tri.ai.prompt.trace.PromptInfo
import tri.util.fine
import tri.util.warning

/** Builder object for [TextCompletion] and [TextChat] tasks. */
class CompletionBuilder {
    var id: String? = null
    var template: PromptTemplate? = null
    var params = mutableMapOf<String, Any>()
    var tokens: Int? = null
    var variation: MChatVariation = MChatVariation()
    var stop: List<String>? = null
    var numResponses: Int? = null
    var requestJson: Boolean? = null

    //region BUILDERS

    fun id(id: String) = apply { this.id = id }
    fun text(text: String) = apply { template = PromptTemplate(text) }
    fun template(template: String) = apply { this.template = PromptTemplate(template) }
    fun template(template: PromptTemplate) = apply { this.template = template }
    fun prompt(prompt: PromptDef) = apply { template = prompt.template() }
    fun input(input: String) = apply { params.putAll(defaultInputParams(input)) }
    fun instruct(input: String, instruct: String) = apply { params.putAll(defaultInstructParams(input = input, instruct = instruct)) }
    fun param(key: String, value: Any) = apply { params[key] = value }
    fun params(map: Map<String, Any>) = apply { params.putAll(map) }
    fun params(vararg pairs: Pair<String, Any>) = apply { params.putAll(mapOf(*pairs)) }
    fun paramsInput(input: String) = apply { params.putAll(defaultInputParams(input)) }
    fun paramsInstruct(input: String, instruct: String) = apply { params.putAll(defaultInstructParams(input = input, instruct = instruct)) }
    fun tokens(tokens: Int?) = apply { this.tokens = tokens }
    fun variation(variation: MChatVariation) = apply { this.variation = variation }
    fun temperature(temp: Double) = apply { this.variation = this.variation.copy(temperature = temp) }
    fun stop(stop: List<String>?) = apply { this.stop = stop }
    fun stop(vararg stop: String) = apply { this.stop = stop.toList() }
    fun numResponses(numResponses: Int?) = apply { this.numResponses = numResponses }
    fun requestJson(requestJson: Boolean?) = apply { this.requestJson = requestJson }

    //endregion

    /** Executes a [TextChat] task with the provided parameters. */
    suspend fun execute(chat: TextChat) =
        chat.validate().chat(
            messages = listOf(TextChatMessage.user(template!!.fill(params))),
            variation = variation,
            tokens = tokens,
            stop = stop,
            numResponses = numResponses,
            requestJson = requestJson
        ).copy(promptInfo = PromptInfo(template!!.template, params.toMap()))

    /** Executes a [TextCompletion] task with the provided parameters. */
    suspend fun execute(completion: TextCompletion) =
        completion.validate().complete(
            text = template!!.fill(params),
            variation = variation,
            tokens = tokens,
            stop = stop,
            numResponses = numResponses,
        ).copy(promptInfo = PromptInfo(template!!.template, params.toMap()))

    /** Validates the completion object before execution. */
    private fun TextCompletion.validate(): TextCompletion {
        require(template != null) { "Template/text must be set before execution." }
        if (requestJson != null)
            warning<CompletionBuilder>("requestJson is not supported with TextCompletion, ignoring.")
        return this
    }

    /** Validates the chat object before execution. */
    private fun TextChat.validate(): TextChat {
        require(template != null) { "Template/text must be set before execution." }
        return this
    }

    companion object {
        val JSON_MAPPER = ObjectMapper()
            .registerModule(JavaTimeModule())
            .registerModule(KotlinModule.Builder().build())
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)!!
    }
}
