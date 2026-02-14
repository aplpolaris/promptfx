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

/**
 * Supports configuration of a random question generator.
 * Templates use [TOPIC_KEY] and [EXAMPLE_KEY] fields, which may be paired together to provide a specific example question about that topic.
 * Any other key in [lists] may be used in the template as a field to be filled with random values from the corresponding list, with e.g. {{tools:3}} denoting a random set of three tools.
 * This allows for "pick from A or B", "pick from A, B, or C", etc. prompt elements.
 */
class StarshipConfigQuestion {
    val template: String = DEFAULT_TEMPLATE
    val topics: List<String> = DEFAULT_TOPICS
    val examples: List<String> = DEFAULT_EXAMPLES
    val lists: Map<String, List<String>> = DEFAULT_LISTS

    companion object {
        internal const val DEFAULT_TEMPLATE = "Generate a random question about LLMs. The question should be 10-20 words."
        internal const val TOPIC_KEY = "topic"
        internal const val EXAMPLE_KEY = "example"
        private val DEFAULT_TOPICS = listOf("LLMs", "LSTMs", "NLP", "GPT3", "memory", "hallucination", "transformers", "summarization", "retrieval-augmented generation")
        private val DEFAULT_EXAMPLES = listOf("What is the different between an LLM and GPT3?")
        private val DEFAULT_LISTS = mapOf<String, List<String>>()
    }
}
