package tri.promptfx.docs

import tri.ai.core.CompletionBuilder
import tri.ai.core.EmbeddingModel
import tri.ai.core.TextChat
import tri.ai.core.TextCompletion
import tri.ai.prompt.PromptDef
import tri.ai.prompt.PromptTemplate
import tri.promptfx.PromptFxGlobals.lookupPrompt
import tri.promptfx.ui.chunk.TextChunkViewModel
import tri.util.fine
import tri.util.ml.ClusterService

/** Analytics for generating text clusters. */
object TextClustering {

    /**
     * Generate a hierarchy of clusters with metadata and descriptions, via a chain of background tasks using [TextChat] and [EmbeddingModel].
     * Summarizes clusters hierarchically whenever the list of chunks (or clusters) is at least size [minForRegroup].
     */
    suspend fun ClusterService.generateClusterHierarchy(
        input: List<TextChunkViewModel>,
        summaryType: ClusterSummaryType,
        itemType: String,
        categories: List<String>,
        sampleTheme: String,
        chatEngine: TextChat,
        embeddingModel: EmbeddingModel,
        minForRegroup: Int = 20,
        attempts: Int = 3,
        progress: (String, Double) -> Unit
    ): List<EmbeddingCluster> {
        var i = 1
        var clusters = input.map {
            EmbeddingCluster("Chunk ${i++}", ClusterDescription(it.text), listOf(), it, it.embedding!!)
        }
        var n = 1
        do {
            val prompt = ClusteringPrompt(summaryType, itemType, categories, sampleTheme)
            clusters = generateClusters(clusters, prompt, chatEngine, attempts) { msg, pct ->
                progress("Level $n", pct)
            }
            progress("Level $n Cluster Embedding Calculations", 0.0)
            clusters.forEach {
                it.embedding = it.description.theme?.let { embeddingModel.calculateEmbedding(it) }
            }
            n++
        } while (clusters.all { it.description.theme != null } && clusters.size > minForRegroup)

        // add prefixes to hierarchy of clusters
        clusters.forEachIndexed { i, it -> addHierarchyPrefixes(it, i+1) }

        return clusters
    }

    private fun addHierarchyPrefixes(cluster: EmbeddingCluster, n: Int, prefix: String = "") {
        val subprefix = if (prefix.isBlank()) "$n" else "$prefix.$n"
        cluster.items.forEachIndexed { i, it ->
            addHierarchyPrefixes(it, i+1, subprefix)
        }
        if (cluster.baseChunk != null)
            cluster.name = "Chunk $subprefix"
        else
            cluster.name = "Cluster $subprefix"
    }

    /**
     * Generate cluster for given list of chunks.
     * Uses the [ClusterService] to collect chunks into clusters, and then uses [TextCompletion] to provide a theme and categories for each cluster.
     * The [progress] callback is called with a value between 0.0 and 1.0 to indicate progress.
     */
    suspend fun ClusterService.generateClusters(
        input: List<EmbeddingCluster>,
        prompt: ClusteringPrompt,
        chatEngine: TextChat,
        attempts: Int,
        progress: (String, Double) -> Unit
    ): List<EmbeddingCluster> {
        var pct = 0.0
        progress("Computing clusters", pct)
        val clustered = cluster(input) { it.embedding!! }
        progress("Computing cluster summaries", pct)
        val clusterCount = clustered.size
        val result = clustered.mapIndexed { i, matches ->
            val description = if (prompt.summaryType == ClusterSummaryType.NONE)
                ClusterDescription()
            else
                generateClusterSummary(matches, prompt, chatEngine, attempts)
            pct += 1.0/clusterCount
            progress("Computing cluster summaries", pct)
            EmbeddingCluster("${i+1}", description, matches, null, null)
        }
        progress("Computing clusters completed", 1.0)
        return result
    }

    /**
     * Generate a summary of a given cluster, using [TextCompletion] and an appropriate prompt.
     * Each cluster is associated with a theme and one or more categories.
     * Use [attempts] to run multiple text completions and combine the results.
     */
    suspend fun generateClusterSummary(
        cluster: List<EmbeddingCluster>,
        prompt: ClusteringPrompt,
        chatEngine: TextChat,
        attempts: Int
    ): ClusterDescription {
        val inputText = cluster.joinToString("\n") { it.description.theme!! }
        val responses = (1..attempts).map {
            CompletionBuilder()
                .tokens(2000)
                .temperature(0.5)
                .template(prompt.summaryType.prompt.template!!)
                .params(
                    PromptTemplate.INPUT to inputText,
                    "item_type" to prompt.itemType,
                    "categories" to prompt.categories.joinToString("\n") { " - $it" },
                    "sample_category" to prompt.categories.first(),
                    "sample_theme" to prompt.sampleTheme
                )
                .execute(chatEngine)
        }.map {
            val lines = it.firstValue.textContent().lines()
            val foundCategory = lines.findLine("category")?.parseList() ?: listOf()
            val foundTheme = lines.findLine("theme") ?: ""
            foundTheme to foundCategory
        }
        val themeOptions = responses.map { it.first }.filter { it.isNotBlank() }
        val categoryOptions = responses.map { it.second }.filter { it.isNotEmpty() }
        val joinedCategories = categoryOptions.flatten().distinct()
        fine<TextClustering>("Generated ${responses.size} summaries for cluster of ${cluster.size} items")
        fine<TextClustering>("  using theme: ${themeOptions.firstOrNull()} (first of ${themeOptions.size} generated results)")
        fine<TextClustering>("  using categories: $joinedCategories (combined from ${categoryOptions.size} generated results)")
        return ClusterDescription(themeOptions.firstOrNull() ?: "No theme", joinedCategories)
    }

    private fun List<String>.findLine(prefix: String) =
        firstOrNull {
            it.substringBefore(":").lowercase() == prefix.lowercase()
        }?.substringAfter(":")?.trim()
    private fun String.parseList() =
        trim().removePrefix("[").removeSuffix("]").trim()
            .split(",", "/").map { it.trim() }

}

/** Prompt for generating a cluster summary. */
class ClusteringPrompt(
    val summaryType: ClusterSummaryType,
    val itemType: String,
    val categories: List<String>,
    val sampleTheme: String,
)

/** Types of summaries that can be generated for a cluster. */
enum class ClusterSummaryType(val promptId: String) {
    CATEGORIES_AND_THEME("generate-taxonomy/categories-and-theme"),
    THEME_ONLY("generate-taxonomy/theme"),
    CATEGORIES_ONLY("generate-taxonomy/categories"),
    NONE("");

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

    val prompt: PromptDef
        get() = lookupPrompt(promptId)
}

/** Consolidates information about a cluster. */
data class EmbeddingCluster(
    var name: String,
    var description: ClusterDescription,
    val items: List<EmbeddingCluster>,
    val baseChunk: TextChunkViewModel?,
    var embedding: List<Double>?
) {
    constructor(it: TextChunkViewModel) : this("", ClusterDescription(it.text), listOf(), it, it.embedding!!)

    override fun toString() = "$name ${description.categories} | Theme: ${description.theme}\n  Items: ${items.map { it.name.ifBlank { it.description.theme } }}"
}

/** Description of a cluster. */
data class ClusterDescription(
    val theme: String? = null,
    val categories: List<String>? = null
)
