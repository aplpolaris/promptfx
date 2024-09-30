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
package tri.promptfx.ui.trace

import javafx.beans.property.SimpleObjectProperty
import javafx.beans.property.SimpleStringProperty
import tornadofx.Fragment
import tornadofx.label
import tornadofx.onLeftClick
import tornadofx.vbox
import tri.ai.prompt.trace.AiPromptTraceSupport

/** A card that displays the trace of a prompt. */
class PromptTraceCard : Fragment() {

    val result = SimpleObjectProperty<Any>("")

    private var trace: AiPromptTraceSupport<*>? = null

    fun setTrace(trace: AiPromptTraceSupport<*>) {
        result.value = trace.values?.firstOrNull() ?: "No result"
        this.trace = trace
    }

    override val root = vbox {
        label(result)

        onLeftClick {
            trace?.let {
                find<PromptTraceDetailsUi>().apply {
                    setTrace(it)
                    openModal()
                }
            }
        }
    }
}

