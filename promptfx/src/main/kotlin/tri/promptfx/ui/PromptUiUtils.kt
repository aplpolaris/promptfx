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
import javafx.beans.property.SimpleBooleanProperty
import javafx.event.EventTarget
import javafx.scene.layout.HBox
import javafx.scene.Node
import javafx.scene.control.ContentDisplay
import javafx.scene.control.Hyperlink
import javafx.scene.layout.Priority
import javafx.scene.text.Text
import tornadofx.*
import tri.ai.text.docs.FormattedText
import tri.ai.text.docs.FormattedTextNode
import tri.promptfx.PromptFxWorkspace

/**
 * Adds a combobox for selecting a prompt, a text for seeing the prompt,
 * and an option to send the prompt to the template view.
 */
fun EventTarget.promptfield(
    fieldName: String = "Template",
    prompt: PromptSelectionModel,
    promptIdList: List<String> = listOf(prompt.id.value),
    workspace: Workspace
) {
    val promptFieldVisible = SimpleBooleanProperty(false)
    field(fieldName) {
        (inputContainer as? HBox)?.spacing = 5.0
        combobox(prompt.id, promptIdList + PromptSelectionModel.CUSTOM) {
            maxWidth = 200.0
            cellFormat(workspace.scope) {
                text = it.substringBefore("@")
                tooltip(it)
            }
        }
        togglebutton(text = "") {
            graphic = FontAwesomeIconView(FontAwesomeIcon.EYE)
            isSelected = false
            tooltip("Toggle visibility of the prompt text.")
            action { promptFieldVisible.set(!promptFieldVisible.value) }
        }
        menubutton(text = "", graphic = FontAwesomeIconView(FontAwesomeIcon.SEND)) {
            tooltip("Template and library options for this prompt.")
            item("Send to Prompt Template View", graphic = FontAwesomeIconView(FontAwesomeIcon.SEND)) {
                action { find<PromptFxWorkspace>().launchTemplateView(prompt.text.value) }
            }
            item("View in Prompt Library", graphic = FontAwesomeIconView(FontAwesomeIcon.BOOK)) {
                action { find<PromptFxWorkspace>().launchLibraryView(prompt.text.value) }
            }
        }
    }
    field(null, forceLabelIndent = true) {
        add(EditableTextArea(prompt.text).root)
        visibleProperty().bindBidirectional(promptFieldVisible)
        managedProperty().bindBidirectional(promptFieldVisible)
    }
}

/** Convert a [FormattedText] to JavaFx [Node]s. */
fun FormattedText.toFxNodes() =
    nodes.map { it.toFxNode(hyperlinkOp) }

/** Convert a [FormattedTextNode] to an FX [Node]. */
fun FormattedTextNode.toFxNode(hyperlinkOp: (String) -> Unit): Node =
    when (hyperlink) {
        null -> Text(text).also {
            it.style = style
            tooltip?.let { t -> it.fancytooltip(t) }
        }
        else -> Hyperlink(text).also {
            it.style = style
            tooltip?.let { t -> it.fancytooltip(t) }
            it.action { hyperlinkOp(text) }
        }
    }

private fun Node.fancytooltip(tip: String) = tooltip(
    graphic = vbox(10.0) {
        if ("\n\n" in tip) {
            this.text(tip.substringBefore("\n\n")) {
                style = "-fx-font-weight: normal; -fx-font-size: 14; -fx-fill: #eee"
                wrappingWidth = 300.0
            }
            this.text(tip.substringAfter("\n\n")) {
                style = "-fx-font-style: italic; -fx-font-size: 14; -fx-fill: white"
                wrappingWidth = 300.0
            }
        } else {
            this.text(tip)
        }
        vgrow = Priority.NEVER
    }
) {
    contentDisplay = ContentDisplay.GRAPHIC_ONLY
}