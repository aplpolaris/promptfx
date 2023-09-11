/*-
 * #%L
 * promptfx-0.1.0-SNAPSHOT
 * %%
 * Copyright (C) 2023 Johns Hopkins University Applied Physics Laboratory
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
package tri.promptfx.apps

import javafx.beans.property.SimpleStringProperty
import tornadofx.text
import tri.ai.embedding.cosineSimilarity
import tri.ai.pips.AiPipelineResult
import tri.ai.pips.AiTaskResult.Companion.result
import tri.promptfx.AiTaskView
import tri.util.ui.NavigableWorkspaceViewImpl

/** Plugin for the [TextSimilarityView]. */
class TextSimilarityPlugin : NavigableWorkspaceViewImpl<TextSimilarityView>("Text", "Text Similarity", TextSimilarityView::class)

/** View designed to calculate text similarity. */
class TextSimilarityView: AiTaskView("Text Similarity",
    "Enter two texts to compare. This will also find the paragraphs in the second text that most closely match the first.") {

    private val firstText = SimpleStringProperty("")
    private val secondText = SimpleStringProperty("")

    init {
        addInputTextArea(firstText)
        input { text("Second Text:") }
        addInputTextArea(secondText)
    }

    override suspend fun processUserInput(): AiPipelineResult {
        val chunks = secondText.get().splitIntoChunks()
        val mod = controller.embeddingService.get()
        val embedList = mod.calculateEmbedding(listOf(firstText.get(), secondText.get()) + chunks)

        val score = cosineSimilarity(embedList[0], embedList[1])
        val scoreText = "Overall similarity: %.2f%%".format(score * 100)

        return if (chunks.size == 1) {
            result(scoreText, mod.modelId).asPipelineResult()
        } else {
            val scores = chunks.mapIndexed { index, line ->
                val similarity = cosineSimilarity(embedList[0], embedList[index + 2])
                line to similarity
            }.sortedByDescending { it.second }.take(2)
            val highestText = "${"Closest paragraph match: %.2f%%\n".format(scores[0].second * 100)}${scores[0].first}"
            val secondText =
                "${"Second closest paragraph match: %.2f%%\n".format(scores[1].second * 100)}${scores[1].first}"

            result("$scoreText\n\n$highestText\n\n$secondText", mod.modelId).asPipelineResult()
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
