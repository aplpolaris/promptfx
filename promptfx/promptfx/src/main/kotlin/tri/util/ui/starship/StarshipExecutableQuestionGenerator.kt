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
package tri.util.ui.starship

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.TextNode
import tri.ai.core.TextChat
import tri.ai.core.TextChatMessage
import tri.ai.core.tool.ExecContext
import tri.ai.core.tool.Executable
import tri.ai.prompt.PromptDef
import tri.ai.prompt.PromptTemplate
import tri.ai.prompt.fill
import tri.util.json.readJsonSchema
import tri.util.ui.starship.StarshipConfigQuestion.Companion.EXAMPLE_KEY
import tri.util.ui.starship.StarshipConfigQuestion.Companion.TOPIC_KEY
import kotlin.collections.random

/** Generates a random question for use in the Starship view.*/
class StarshipExecutableQuestionGenerator(val config: StarshipConfigQuestion, val chat: TextChat) : Executable {
    override val name = "starship/random-question"
    override val description = "Generates a random question."
    override val version = "0.0.1"
    override val inputSchema = readJsonSchema(INPUT_SCHEMA)
    override val outputSchema = readJsonSchema(OUTPUT_SCHEMA)

    override suspend fun execute(input: JsonNode, context: ExecContext): JsonNode {
        val question = randomQuestion()
        val response = chat.chat(listOf(TextChatMessage.user(question)))
            .firstValue.textContent()
        return TextNode.valueOf(response)
    }

    /** Generate a random question based on the current configuration. */
    fun randomQuestion(): String {
        val index = config.topics.indices.random()
        val topic = config.topics[index]
        val example = config.examples[index % config.examples.size]
        val template = PromptTemplate(config.template)
        val prompt = PromptDef(id = "", template = template.fill(TOPIC_KEY to topic, EXAMPLE_KEY to example))
        val fields = PromptTemplate(prompt.template!!).findFields().associateWith {
            val rand = if (":" in it) {
                val (key, n) = it.split(":")
                key to n.toInt()
            } else {
                it to 1
            }
            config.lists[rand.first]!!.randomOrString(rand.second)
        }
        return prompt.fill(fields)
    }

    private fun List<String>.randomOrString(n: Int) = when (n) {
        1 -> random()
        2 -> "pick from ${random()} or ${random()}"
        else -> "pick from " + (1 until n).joinToString(", ") { this.random() } + ", or " + this.random()
    }

    companion object {
        private const val INPUT_SCHEMA = """{"type":"object"}"""
        private const val OUTPUT_SCHEMA = """{"type":"string"}"""
    }
}

