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
package tri.promptfx.ui

import de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon
import de.jensd.fx.glyphs.fontawesome.FontAwesomeIconView
import javafx.beans.property.SimpleStringProperty
import javafx.event.EventTarget
import javafx.scene.layout.Priority
import tornadofx.*
import tri.ai.prompt.AiPrompt
import tri.ai.prompt.AiPrompt.Companion.fill
import tri.ai.prompt.AiPromptLibrary
import tri.promptfx.PromptFxWorkspace
import tri.util.ui.templatemenubutton

/** View for selecting and editing a prompt. */
class EditablePromptUi(private val promptFilter: (Map.Entry<String, AiPrompt>) -> Boolean, val instruction: String): Fragment() {

    private val prompts
        get() = AiPromptLibrary.INSTANCE.prompts.filter(promptFilter).keys
    val templateText = SimpleStringProperty("")

    init {
        prompts.firstOrNull()?.let {
            templateText.set(AiPromptLibrary.lookupPrompt(it).template)
        }
    }

    /** UI for editing prompts with a given prefix. */
    constructor(prefix: String, instruction: String) : this({ it.key.startsWith(prefix) }, instruction)

    /** Fills the template with the provided values. */
    fun fill(vararg values: Pair<String, Any>) = templateText.value.fill(*values)

    override val root = vbox {
        toolbar {
            text(instruction)
            spacer()
            templatemenubutton(templateText, promptFilter)
            button("", FontAwesomeIconView(FontAwesomeIcon.SEND)) {
                tooltip("Try out the current prompt in the Prompt Template view.")
                action { find<PromptFxWorkspace>().launchTemplateView(templateText.value) }
            }
        }
        textarea(templateText) {
            promptText = "Enter a prompt here"
            vgrow = Priority.ALWAYS
            isWrapText = true
            style = "-fx-font-size: 18px;"
        }
    }

}

/** Add a prompt field to the UI. */
fun EventTarget.editablepromptui(promptFilter: (Map.Entry<String, AiPrompt>) -> Boolean, instruction: String): EditablePromptUi {
    val ui = EditablePromptUi(promptFilter, instruction)
    plusAssign(ui)
    return ui
}

/** Add a prompt field to the UI. */
fun EventTarget.editablepromptui(prefix: String, instruction: String): EditablePromptUi {
    val ui = EditablePromptUi(prefix, instruction)
    plusAssign(ui)
    return ui
}
