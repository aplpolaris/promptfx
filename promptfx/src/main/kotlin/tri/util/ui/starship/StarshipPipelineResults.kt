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

import javafx.beans.property.SimpleBooleanProperty
import javafx.beans.property.SimpleObjectProperty
import javafx.beans.property.SimpleStringProperty
import tornadofx.observableListOf
import tornadofx.stringBinding
import tri.ai.prompt.trace.batch.AiPromptRunConfig
import tri.util.ui.DocumentThumbnail

/** Object collecting observable results from a pipeline execution. */
class StarshipPipelineResults {
    /** The initial string input. */
    val input = SimpleStringProperty(null)
    /** The initial run config, including prompt and model info. */
    val runConfig = SimpleObjectProperty<AiPromptRunConfig>(null)
    /** The output trace for primary prompt. */
    val output = SimpleObjectProperty<StarshipInterimResult>(null)
    /** The output text from the primary trace. */
    val outputText = output.stringBinding { it?.rawText ?: "" }
    /** Secondary run configs for additional prompts based on the first output. */
    val secondaryRunConfigs = observableListOf<AiPromptRunConfig>()
    /** Secondary outputs from additional prompts. */
    val secondaryOutputs = observableListOf<StarshipInterimResult>()

    /** Output to highlight. */
    val outputHighlight = SimpleObjectProperty<StarshipInterimResult>(null)
    /** The output text from the primary trace. */
    val outputHighlightText = outputHighlight.stringBinding { it?.rawText ?: "" }

    /** Thumbnails associated with output. */
    val thumbnails = observableListOf<DocumentThumbnail>()
    /** Flag indicating start of execution. */
    val started = SimpleBooleanProperty(false)
    /** Flag indicating active step. */
    val activeStep = SimpleObjectProperty<Int>(0)
    /** Flag indicating completion. */
    val completed = SimpleBooleanProperty(false)

    fun clearAll() {
        input.set(null)
        runConfig.set(null)
        output.set(null)
        outputHighlight.set(null)
        secondaryRunConfigs.clear()
        secondaryOutputs.clear()
        started.set(false)
        completed.set(false)
    }
}
