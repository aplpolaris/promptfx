package tri.promptfx.apps

import javafx.beans.property.SimpleStringProperty
import tornadofx.*
import tri.ai.pips.AiPipelineResult
import tri.ai.embedding.cosineSimilarity
import tri.promptfx.AiTaskView
import tri.ai.openai.EMBEDDING_ADA
import tri.ai.pips.AiTaskResult.Companion.result
import tri.util.ui.NavigableWorkspaceViewImpl

class TextSimilarityView: AiTaskView("Text Similarity",
    "Enter two texts to compare. This will also find the paragraphs in the second text that most closely match the first.") {

    private val modelId = EMBEDDING_ADA

    private val firstText = SimpleStringProperty("")
    private val secondText = SimpleStringProperty("")

    init {
        addInputTextArea(firstText)
        input { text("Second Text:") }
        addInputTextArea(secondText)
    }

    override suspend fun processUserInput(): AiPipelineResult {
        val chunks = secondText.get().splitIntoChunks()
        val embeddings = controller.openAiPlugin.client.quickEmbedding(modelId, listOf(firstText.get(), secondText.get()) + chunks)
        val embedList = embeddings.value!!

        val score = cosineSimilarity(embedList[0], embedList[1])
        val scoreText = "Overall similarity: %.2f%%".format(score * 100)

        return if (chunks.size == 1) {
            result(scoreText, modelId).asPipelineResult()
        } else {
            val scores = chunks.mapIndexed { index, line ->
                val similarity = cosineSimilarity(embedList[0], embedList[index + 2])
                line to similarity
            }.sortedByDescending { it.second }.take(2)
            val highestText = "${"Closest paragraph match: %.2f%%\n".format(scores[0].second * 100)}${scores[0].first}"
            val secondText =
                "${"Second closest paragraph match: %.2f%%\n".format(scores[1].second * 100)}${scores[1].first}"

            result("$scoreText\n\n$highestText\n\n$secondText", modelId).asPipelineResult()
        }
    }

}

private fun String.splitIntoChunks(): List<String> {
    val chunks = mutableListOf<String>()
    var currentChunk = ""
    for (line in lines()) {
        if (currentChunk.length + line.length > 2048) {
            chunks.add(currentChunk)
            currentChunk = ""
        }
        currentChunk += line
    }
    chunks.add(currentChunk)
    return chunks.filter { it.isNotBlank() }
}

class TextSimilarityPlugin : NavigableWorkspaceViewImpl<TextSimilarityView>("Text", "Text Similarity", TextSimilarityView::class)
