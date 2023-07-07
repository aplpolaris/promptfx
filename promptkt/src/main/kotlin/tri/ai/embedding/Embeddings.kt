package tri.ai.embedding

import kotlin.math.pow

fun cosineSimilarity(resp1: List<Double>, resp2: List<Double>): Double {
    val dotProduct = resp1.zip(resp2).sumOf { it.first * it.second }
    val magnitude1 = resp1.sumOf { it * it }.pow(0.5)
    val magnitude2 = resp2.sumOf { it * it }.pow(0.5)
    return dotProduct / (magnitude1 * magnitude2)
}

fun List<Float>.dot(resp2: List<Float>) =
    zip(resp2).sumOf { it.first * it.second.toDouble() }
