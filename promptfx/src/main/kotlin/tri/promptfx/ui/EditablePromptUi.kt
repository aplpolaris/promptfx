/*-
 * #%L
 * promptfx-0.1.14-SNAPSHOT
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
package tri.promptfx.ui

import de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon
import de.jensd.fx.glyphs.fontawesome.FontAwesomeIconView
import javafx.beans.property.SimpleStringProperty
import javafx.geometry.Pos
import javafx.scene.layout.Priority
import tornadofx.*
import tri.ai.prompt.AiPromptLibrary
import tri.promptfx.PromptFxWorkspace

/** View for selecting and editing a prompt. */
class EditablePromptUi(val prefix: String, val instruction: String): Fragment() {

    private val prompts
        get() = AiPromptLibrary.withPrefix(prefix)
    val templateText = SimpleStringProperty(prompts.firstOrNull()?.let { AiPromptLibrary.lookupPrompt(it).template } ?: "")

    override val root = vbox {
        hbox {
            alignment = Pos.CENTER_LEFT
            spacing = 5.0
            padding = insets(5.0, 0.0, 5.0, 0.0)
            text(instruction)
            spacer()
            menubutton("", FontAwesomeIconView(FontAwesomeIcon.LIST)) {
                prompts.forEach { key ->
                    item(key) {
                        action { templateText.set(AiPromptLibrary.lookupPrompt(key).template) }
                    }
                }
            }
            button("", FontAwesomeIconView(FontAwesomeIcon.SEND)) {
                tooltip("Copy the current prompt to the Prompt Template view under Tools and open that view.")
                action { (workspace as PromptFxWorkspace).launchTemplateView(templateText.value) }
            }
        }
        textarea(templateText) {
            vgrow = Priority.ALWAYS
            isWrapText = true
            style = "-fx-font-size: 18px;"
        }
    }

}
