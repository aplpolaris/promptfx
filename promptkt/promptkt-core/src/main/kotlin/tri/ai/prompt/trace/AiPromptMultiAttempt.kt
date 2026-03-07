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

import com.fasterxml.jackson.databind.node.ArrayNode
import tri.ai.core.CompletionBuilder
import tri.ai.core.TextChat
import tri.util.json.jsonMapper

/** Strategy for merging results from multiple JSON list output attempts. */
enum class JsonListMergeStrategy {
    /** Return all unique items across all successful attempts. */
    UNION,
    /** Return only items appearing in ALL successful attempts. */
    INTERSECTION,
    /** Return items appearing in the most attempts, sorted by frequency descending. */
    TOP_REPEATED
}

/**
 * Attempts to extract a list of strings from this output's text content.
 * Handles clean JSON arrays, JSON objects whose first array-valued field is used,
 * and markdown code blocks wrapping either of the above.
 */
fun AiOutput.tryJsonStringList(): List<String>? {
    // access fields directly to avoid the exception thrown by textContent() when no content exists
    val raw = text
        ?: message?.content
        ?: multimodalMessage?.content?.firstNotNullOfOrNull { it.text }
        ?: other?.toString()
        ?: return null
    // strip markdown code fences if present
    val text = raw.trim().let {
        if (it.startsWith("```")) {
            it.lines()
                .drop(1)
                .dropLastWhile { line -> line.trimStart().startsWith("```") }
                .joinToString("\n")
                .trim()
        } else {
            it
        }
    }
    return try {
        val node = jsonMapper.readTree(text)
        when {
            node.isArray -> (node as ArrayNode).map { it.asText() }
            node.isObject -> node.fields().asSequence()
                .map { it.value }
                .firstOrNull { it.isArray }
                ?.let { (it as ArrayNode).map { el -> el.asText() } }
            else -> null
        }
    } catch (_: Exception) {
        null
    }
}

/**
 * Merges a list of string lists from multiple LLM attempts using the specified [strategy].
 * Items are compared case-insensitively; the original casing from the first occurrence is preserved.
 *
 * @param lists string lists returned by each attempt
 * @param strategy the merge strategy to apply
 * @param minAttemptFraction for [JsonListMergeStrategy.TOP_REPEATED]: minimum fraction of attempts
 *   an item must appear in to be included in the result (default 0.5, i.e. a simple majority)
 */
fun mergeJsonLists(
    lists: List<List<String>>,
    strategy: JsonListMergeStrategy,
    minAttemptFraction: Double = 0.5
): List<String> {
    if (lists.isEmpty()) return emptyList()

    fun normalize(s: String) = s.trim().lowercase()

    return when (strategy) {
        JsonListMergeStrategy.UNION -> {
            val seen = mutableSetOf<String>()
            lists.flatten().filter { seen.add(normalize(it)) }
        }

        JsonListMergeStrategy.INTERSECTION -> {
            val normalizedSets = lists.map { list -> list.map { normalize(it) }.toSet() }
            val common = normalizedSets.reduce { acc, set -> acc.intersect(set) }
            lists.first().filter { normalize(it) in common }
        }

        JsonListMergeStrategy.TOP_REPEATED -> {
            // toInt() truncates (floor), so 3 * 0.5 = 1, meaning a simple majority for >=2 attempts
            val minCount = maxOf(1, (lists.size * minAttemptFraction).toInt())
            // count how many distinct attempts each item appears in
            val attemptCount = mutableMapOf<String, Int>()
            for (list in lists) {
                val seenInAttempt = mutableSetOf<String>()
                for (item in list) {
                    val key = normalize(item)
                    if (seenInAttempt.add(key)) {
                        attemptCount[key] = (attemptCount[key] ?: 0) + 1
                    }
                }
            }
            // preserve original case from the first occurrence
            val firstOccurrence = mutableMapOf<String, String>()
            for (list in lists) {
                for (item in list) {
                    firstOccurrence.putIfAbsent(normalize(item), item)
                }
            }
            attemptCount.entries
                .filter { it.value >= minCount }
                .sortedByDescending { it.value }
                .map { firstOccurrence[it.key]!! }
        }
    }
}

/**
 * Executes a prompt [attempts] times, parses each result as a JSON list of strings,
 * and merges them using [mergeStrategy].
 *
 * The builder's `requestJson` flag is set to `true` automatically so the model is asked
 * to produce JSON output.  All other builder settings (template, params, tokens, etc.)
 * are used as-is for every attempt.
 *
 * @param chat the chat model to execute against
 * @param attempts number of independent LLM calls to make (default 3)
 * @param mergeStrategy strategy used to combine items from all attempts (default [JsonListMergeStrategy.TOP_REPEATED])
 * @param minAttemptFraction for [JsonListMergeStrategy.TOP_REPEATED]: minimum fraction of attempts
 *   an item must appear in (default 0.5); ignored for other strategies
 * @return a merged [AiPromptTrace] whose single output contains the merged `List<String>`,
 *   or an error trace if all attempts fail or no valid JSON lists are found
 */
suspend fun CompletionBuilder.executeMultiAttemptJsonList(
    chat: TextChat,
    attempts: Int = 3,
    mergeStrategy: JsonListMergeStrategy = JsonListMergeStrategy.TOP_REPEATED,
    minAttemptFraction: Double = 0.5
): AiPromptTrace {
    // ensure JSON output is requested
    requestJson(true)

    val startTime = System.currentTimeMillis()
    val traces = (1..attempts).map { execute(chat) }
    val totalTime = System.currentTimeMillis() - startTime

    val totalQueryTokens = traces.sumOf { it.exec.queryTokens ?: 0 }.takeIf { it > 0 }
    val totalResponseTokens = traces.sumOf { it.exec.responseTokens ?: 0 }.takeIf { it > 0 }

    val baseExecInfo = AiExecInfo(
        attempts = attempts,
        responseTimeMillisTotal = totalTime,
        queryTokens = totalQueryTokens,
        responseTokens = totalResponseTokens
    )

    val successful = traces.filter { it.exec.succeeded() }
    if (successful.isEmpty()) {
        val last = traces.last()
        return AiPromptTrace(
            last.prompt, last.model,
            baseExecInfo.copy(error = last.exec.error, throwable = last.exec.throwable)
        )
    }

    val parsedLists = successful.mapNotNull { trace ->
        trace.values?.firstOrNull()?.tryJsonStringList()
    }
    if (parsedLists.isEmpty()) {
        val last = successful.last()
        return AiPromptTrace(
            last.prompt, last.model,
            baseExecInfo.copy(error = "No valid JSON list found in any of $attempts attempt(s)")
        )
    }

    val merged = mergeJsonLists(parsedLists, mergeStrategy, minAttemptFraction)
    val last = successful.last()
    return AiPromptTrace(last.prompt, last.model, baseExecInfo, AiOutputInfo.listSingleOutput(merged))
}
