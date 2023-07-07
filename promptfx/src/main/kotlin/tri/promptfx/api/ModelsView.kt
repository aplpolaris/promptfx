package tri.promptfx.api

import tri.ai.openai.OpenAiSettings
import tri.ai.pips.AiPipelineResult
import tri.ai.pips.AiTaskResult.Companion.result
import tri.promptfx.AiTaskView
import java.time.Instant
import java.time.ZoneId

class ModelsView : AiTaskView("Models", "List all models, sorted by creation date", showInput = false) {

    override suspend fun processUserInput(): AiPipelineResult {
        val models = controller.openAiPlugin.client.client.models()
            .sortedByDescending { it.created }
            .groupBy { Instant.ofEpochSecond(it.created).monthYear() }
        return models.entries.joinToString("\n\n") { (month, models) ->
            "$month\n${models.joinToString("\n") { " - " + it.id.id }}"
        }.let {
            result(it).asPipelineResult()
        }
    }

    private fun Instant.monthYear() = atZone(ZoneId.systemDefault()).let {
        "${it.year}-${it.monthValue.toString().padStart(2, '0')}"
    }

}
