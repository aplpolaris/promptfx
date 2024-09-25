package tri.promptfx.library

import tri.ai.core.TextCompletion
import tri.ai.core.templateTask
import tri.ai.embedding.EmbeddingService
import tri.ai.prompt.AiPrompt.Companion.INPUT
import tri.promptfx.ui.chunk.TextChunkViewModel
import tri.util.fine
import tri.util.ml.ClusterService

/** Analytics for generating text clusters. */
object TextClustering {

    /**
     * Generate a hierarchy of clusters with metadata and descriptions, via a chain of background tasks using [TextCompletion] and [EmbeddingService].
     * Summarizes clusters hierarchically whenever the list of chunks (or clusters) is at least size [minForRegroup].
     */
    suspend fun ClusterService.generateClusterHierarchy(
        input: List<TextChunkViewModel>,
        summaryType: ClusterSummaryType,
        itemType: String,
        categories: List<String>,
        sampleTheme: String,
        completionEngine: TextCompletion,
        embeddingService: EmbeddingService,
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
            clusters = generateClusters(clusters, prompt, completionEngine, attempts) { msg, pct ->
                progress("Level $n", pct)
            }
            progress("Level $n Cluster Embedding Calculations", 0.0)
            clusters.forEach {
                it.embedding = it.description.theme?.let { embeddingService.calculateEmbedding(it) }
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
            addHierarchyPrefixes(it, i+1, "$subprefix")
        }
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
        completionEngine: TextCompletion,
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
                generateClusterSummary(matches, prompt, completionEngine, attempts)
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
        completionEngine: TextCompletion,
        attempts: Int
    ): ClusterDescription {
        val inputText = cluster.joinToString("\n") { it.description.theme!! }
        val responses = (1..attempts).map {
            completionEngine.templateTask(
                prompt.summaryType.promptId,
                INPUT to inputText,
                "item_type" to prompt.itemType,
                "categories" to prompt.categories.joinToString("\n") { " - $it" },
                "sample_category" to prompt.categories.first(),
                "sample_theme" to prompt.sampleTheme,
                tokenLimit = 2000,
                temp = 0.5
            )
        }.map {
            val lines = it.firstValue.lines()
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

class ClusteringPrompt(
    val summaryType: ClusterSummaryType,
    val itemType: String,
    val categories: List<String>,
    val sampleTheme: String,
)

enum class ClusterSummaryType(val promptId: String) {
    CATEGORIES_AND_THEME("generate-categories-and-theme"),
    THEME_ONLY("generate-categories-theme-only"),
    CATEGORIES_ONLY("generate-categories"),
    NONE("")
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