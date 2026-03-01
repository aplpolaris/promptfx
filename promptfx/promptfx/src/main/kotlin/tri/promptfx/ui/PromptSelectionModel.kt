/*-
 * #%L
 * tri.promptfx:promptfx
 * %%
 * Copyright (C) 2023 - 2026 Johns Hopkins University Applied Physics Laboratory
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
package tri.promptfx.ui

import javafx.beans.property.SimpleObjectProperty
import javafx.beans.property.SimpleStringProperty
import tornadofx.*
import tri.ai.prompt.PromptDef
import tri.ai.prompt.PromptTemplate
import tri.promptfx.PromptFxGlobals.lookupPrompt

/** Model for a prompt id and lookup result in prompt table. */
class PromptSelectionModel(_id: String, _prompt: String? = null) {
    val id = SimpleStringProperty(_id)
    val prompt = SimpleObjectProperty(_prompt?.let { PromptDef(_id, template = it) } ?: lookupPrompt(_id))
    val text = SimpleStringProperty(prompt.value!!.template)

    init {
        id.onChange {
            if (it == CUSTOM) {
                prompt.set(PromptDef(id = CUSTOM, template = text.value))
            } else {
                prompt.set(lookupPrompt(it!!))
                text.set(prompt.value.template)
            }
        }
        text.onChange {
            val currentTemplate = if (id.value == CUSTOM) null else lookupPrompt(id.value).template
            if (it != currentTemplate) {
                id.set(CUSTOM)
                prompt.set(PromptDef(id = CUSTOM, template = text.value))
            }
        }
    }

    /** Fill fields into prompt text (mustache template). */
    fun fill(vararg fields: Pair<String, Any>) = PromptTemplate(text.value).fill(*fields)

    /** Fill fields into prompt text (mustache template). */
    fun fill(fields: Map<String, Any>) = PromptTemplate(text.value).fill(fields)

    companion object {
        const val CUSTOM = "Custom"
    }
}
