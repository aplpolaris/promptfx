/*-
 * #%L
 * tri.promptfx:promptfx
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
package tri.util.ui.starship

import com.fasterxml.jackson.dataformat.yaml.YAMLMapper
import com.fasterxml.jackson.module.kotlin.readValue
import de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon
import tri.ai.prompt.PromptDef
import tri.ai.prompt.PromptTemplate
import tri.ai.prompt.fill
import tri.promptfx.PromptFxModels
import java.io.File

/** Configurable content for Starship demo mode. */
object StarshipContentConfig {

    private val configFile = setOf(File("starship.yaml"), File("config/starship.yaml")).firstOrNull { it.exists() }
    private val configFileAlt = setOf(File("starship-custom.yaml"), File("config/starship-custom.yaml")).firstOrNull { it.exists() }
    private val config: Map<String, Any> = (configFileAlt ?: configFile)?.let { YAMLMapper().readValue(it) } ?: mapOf()

    val backgroundIcon: FontAwesomeIcon = FontAwesomeIcon.STAR_ALT
    val backgroundIconCount = 1000

    /** If true, show 3x3 grid with screens. */
    val isShowGrid = true

    /** Labels for explainer overlay. */
    @Suppress("UNCHECKED_CAST")
    val explain = config["explain"] as? List<String> ?:
        listOf(
            "AI generates a random question.",
            "Using semantic text similarity models, we look for matching paragraphs in a set of source documents.",
            "LLMs answer the question using the matching paragraphs.",
            "The answer is summarized for the target audience.",
            "The answer can be transformed in other ways depending on the use case."
        )

    /** Prompt info for secondary prompts in pipeline. */
    val promptInfo = config["prompt-info"] as? List<Any?> ?:
        listOf(
            mapOf("text-simplify-audience" to mapOf("audience" to "a general audience")),
            "document-reduce-outline",
            "document-reduce-technical-terms",
            mapOf("translate-text" to mapOf("instruct" to "a random language")),
        )

    /** Options that can be dropped into custom prompts. */
    @Suppress("UNCHECKED_CAST")
    val userOptions = config["user-options"] as? Map<String, Map<String, List<String>>> ?:
        mapOf(
            "text-simplify-audience" to mapOf("audience" to listOf("a general audience", "elementary school students", "high school students", "software engineers", "executives")),
            "translate-text" to mapOf("instruct" to listOf("a random language", "English", "Spanish", "French", "German", "Chinese", "Japanese", "Emoji", "Korean", "Russian", "Arabic", "Hindi", "Portuguese", "Italian"))
        )

    //region RANDOM QUESTION CONFIGS

    @Suppress("UNCHECKED_CAST")
    private val randomQuestion = config["random-question"] as? Map<String, Any>
    private val randomQuestionTemplate = randomQuestion?.get("template") as? String ?: "Generate a random question about LLMs. The question should be 10-20 words."
    @Suppress("UNCHECKED_CAST")
    private val randomQuestionTopic = randomQuestion?.get("topics") as? List<String> ?: listOf("LLMs", "LSTMs", "NLP", "GPT3", "memory", "hallucination", "transformers", "summarization", "retrieval-augmented generation")
    @Suppress("UNCHECKED_CAST")
    private val randomQuestionExample = randomQuestion?.get("examples") as? List<String> ?: listOf("What is the difference between LLM and GPT-3?")
    @Suppress("UNCHECKED_CAST")
    private val randomQuestionLists = randomQuestion?.get("lists") as? Map<String, List<String>> ?: mapOf()

    //endregion

    /** Generate a random question based on the current configuration. */
    suspend fun randomQuestion(): String {
        val index = randomQuestionTopic.indices.random()
        val topic = randomQuestionTopic[index]
        val example = randomQuestionExample[index % randomQuestionExample.size]
        val template = PromptTemplate(randomQuestionTemplate)
        val prompt = PromptDef(id = "", template = template.fill("topic" to topic, "example" to example))
        val fields = template.findFields().associateWith {
            val rand = if (":" in it) {
                val (key, n) = it.split(":")
                key to n.toInt()
            } else {
                it to 1
            }
            randomQuestionLists[rand.first]!!.random(rand.second)
        }
        return PromptFxModels.textCompletionModelDefault().complete(prompt.fill(fields)).firstValue
    }

    private fun List<String>.random(n: Int) = when (n) {
        1 -> random()
        2 -> "pick from ${random()} or ${random()}"
        else -> "pick from " + (1 until n).joinToString(", ") { this.random() } + ", or " + this.random()
    }

}
