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

import javafx.collections.ListChangeListener
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import tri.promptfx.PromptFxModels

class StarshipPipelineTest {
    @Test
    @Disabled("TBD")
    fun testExec() {
        val config = StarshipPipelineConfig(PromptFxModels.textCompletionModelDefault())
        val results = StarshipPipelineResults().apply {
            input.addListener { _, _, newValue -> println("Input: $newValue") }
            runConfig.addListener { _, _, newValue -> println("RunConfig: $newValue") }
            output.addListener { _, _, newValue -> println("Output: $newValue") }
            outputText.addListener { _, _, newValue -> println("OutputText: $newValue") }
            secondaryRunConfigs.addListener(ListChangeListener { println("SecondaryRunConfigs: ${it.list}") })
            secondaryOutputs.addListener(ListChangeListener { println("SecondaryOutputs: ${it.list}") })
            started.addListener { _, _, _ -> println("Started") }
            completed.addListener { _, _, _ -> println("Completed") }
        }
        StarshipPipeline.exec(config, results)
    }
}
