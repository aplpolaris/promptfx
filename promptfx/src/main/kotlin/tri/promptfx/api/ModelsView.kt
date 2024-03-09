/*-
 * #%L
 * tri.promptfx:promptfx
 * %%
 * Copyright (C) 2023 - 2024 Johns Hopkins University Applied Physics Laboratory
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
package tri.promptfx.api

import tri.ai.pips.AiPipelineResult
import tri.ai.pips.AiTaskResult.Companion.result
import tri.promptfx.AiTaskView
import java.time.Instant
import java.time.ZoneId

class ModelsView : AiTaskView("Models", "List all models, sorted by creation date", showInput = false) {

    init {
        hideParameters()
    }

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
