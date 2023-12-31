/*-
 * #%L
 * promptfx-0.1.0-SNAPSHOT
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
package tri.util.ui

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.KotlinModule
import de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon
import de.jensd.fx.glyphs.fontawesome.FontAwesomeIconView
import javafx.beans.property.Property
import javafx.beans.property.SimpleBooleanProperty
import javafx.beans.property.SimpleObjectProperty
import javafx.beans.property.SimpleStringProperty
import javafx.beans.value.ObservableStringValue
import javafx.event.EventTarget
import javafx.scene.control.Hyperlink
import javafx.scene.control.Slider
import javafx.scene.layout.HBox
import javafx.scene.paint.Color
import javafx.scene.text.Text
import javafx.scene.text.TextFlow
import javafx.stage.Window
import tornadofx.*
import tri.promptfx.PromptFxWorkspace
import java.io.File

fun TextFlow.plainText() = children.joinToString("") {
    (it as? Text)?.text ?:
    (it as? Hyperlink)?.text ?: ""
}

internal fun SimpleObjectProperty<File>.chooseFolder(owner: Window?) {
    chooseDirectory(
        title = "Select Document Folder",
        initialDirectory = value?.findDirectory(),
        owner = owner
    )?.let {
        set(it)
    }
}

/** Return this if its a directory, otherwise try to find a parent directory. If none, return null. */
private fun File.findDirectory(): File? = if (isDirectory) this else parentFile?.findDirectory()

fun icon(icon: FontAwesomeIcon) = FontAwesomeIconView(icon)

val FontAwesomeIcon.graphic
    get() = icon(this)

val FontAwesomeIconView.gray
    get() = apply {
        fill = Color.GRAY
    }

val FontAwesomeIconView.navy
    get() = apply {
        fill = Color.NAVY
    }

fun EventTarget.slider(range: ClosedRange<Double>, value: Property<Number>, op: Slider.() -> Unit = {}) =
    slider(range, 0.0, null, op).apply {
        valueProperty().bindBidirectional(value)
    }

fun EventTarget.slider(range: IntRange, value: Property<Number>, op: Slider.() -> Unit = {}) =
    slider(range, 0, null, op).apply {
        valueProperty().bindBidirectional(value)
    }

val MAPPER = ObjectMapper(YAMLFactory()).apply {
    registerModule(KotlinModule())
    registerModule(JavaTimeModule())
}

fun ResourceLookup.yaml(resource: String): Map<*, *> =
    stream(resource).use { MAPPER.readValue(it, Map::class.java) }

/**
 * Adds a combobox for selecting a prompt, a text for seeing the prompt,
 * and an option to send the prompt to the template view.
 */
fun EventTarget.promptfield(
    fieldName: String = "Template",
    promptId: SimpleStringProperty,
    promptIdList: List<String>,
    promptText: ObservableStringValue,
    workspace: Workspace
) {
    val promptFieldVisible = SimpleBooleanProperty(false)
    field(fieldName) {
        (inputContainer as? HBox)?.spacing = 5.0
        combobox(promptId, promptIdList) {
            maxWidth = 200.0
        }
        togglebutton(text = "") {
            graphic = FontAwesomeIconView(FontAwesomeIcon.EYE)
            isSelected = false
            tooltip("Toggle visibility of the prompt text.")
            action { promptFieldVisible.set(!promptFieldVisible.value) }
        }
        button(text = "", graphic = FontAwesomeIconView(FontAwesomeIcon.SEND)) {
            tooltip("Copy this prompt to the Prompt Template view under Tools and open that view.")
            action { (workspace as PromptFxWorkspace).launchTemplateView(promptText.value) }
        }
    }
    field(null, forceLabelIndent = true) {
        text(promptText).apply {
            wrappingWidth = 300.0
            promptText.onChange { tooltip(it) }
        }
        visibleProperty().bindBidirectional(promptFieldVisible)
        managedProperty().bindBidirectional(promptFieldVisible)
    }
}
