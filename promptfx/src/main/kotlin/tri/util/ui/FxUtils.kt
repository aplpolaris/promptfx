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
package tri.util.ui

import de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon
import de.jensd.fx.glyphs.fontawesome.FontAwesomeIconView
import javafx.beans.property.Property
import javafx.beans.property.SimpleObjectProperty
import javafx.beans.property.SimpleStringProperty
import javafx.beans.value.ChangeListener
import javafx.beans.value.ObservableValue
import javafx.collections.FXCollections
import javafx.collections.ListChangeListener
import javafx.collections.ObservableList
import javafx.event.EventTarget
import javafx.scene.control.Hyperlink
import javafx.scene.control.TextInputControl
import javafx.scene.input.TransferMode
import javafx.scene.paint.Color
import javafx.scene.text.Text
import javafx.scene.text.TextFlow
import javafx.stage.Window
import tornadofx.*
import tri.ai.prompt.AiPrompt
import tri.ai.prompt.AiPromptLibrary
import java.io.File

//region FILE I/O

/** Configures a [TextInputControl] to accept dropped files and set its text to the content of the first file. */
fun TextInputControl.enableDroppingFileContent() {
    // enable dropping file content
    setOnDragOver { it.acceptTransferModes(*TransferMode.COPY_OR_MOVE) }
    setOnDragDropped {
        if (it.dragboard.hasFiles()) {
            textProperty().set(it.dragboard.files.first().readText())
        }
        it.isDropCompleted = true
        it.consume()
    }
}

//endregion

//region Text and TextFlow UTILS

fun TextFlow.plainText() = children.joinToString("") {
    (it as? Text)?.text ?:
    (it as? Hyperlink)?.text ?: ""
}

//endregion

//region ICONS

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

val FontAwesomeIconView.burgundy
    get() = apply {
        fill = Color(128.0/255, 0.0, 32.0/255, 1.0)
    }

val FontAwesomeIconView.forestGreen
    get() = apply {
        fill = Color(34.0/255, 139.0/255, 34.0/255, 1.0)
    }

//endregion

//region UI BUILDERS

/**
 * Creates a [menubutton] to select a template
 */
fun EventTarget.templatemenubutton(template: SimpleStringProperty, promptFilter: (Map.Entry<String, AiPrompt>) -> Boolean = { true }) =
    listmenubutton(
        items = { AiPromptLibrary.INSTANCE.prompts.filter(promptFilter).keys },
        action = { template.set(AiPromptLibrary.lookupPrompt(it).template) }
    )

/**
 * Creates a [menubutton] with the provided items and action.
 * The list is dynamically updated each time the button is shown.
 */
fun EventTarget.listmenubutton(items: () -> Collection<String>, action: (String) -> Unit) =
    menubutton("", FontAwesomeIconView(FontAwesomeIcon.LIST)) {
        setOnShowing {
            this.items.clear()
            items().forEach { key ->
                item(key) {
                    action { action(key) }
                }
            }
        }
    }

//endregion

//region PROPERTY BINDINGS

/**
 * Create an observable list backed by a mutable property and a [List] or [ObservableList] property therein.
 * @param obj the property to listen to
 * @param op a function to extract the list from the property
 */
fun <X, Y> createListBinding(obj: ObservableValue<X>, op: (X?) -> List<Y>): ObservableList<Y> =
    createListBinding(obj, op) { _, it -> it }

/**
 * Create an observable list backed by a mutable property and a [List] or [ObservableList] property therein.
 * @param obj the property to listen to
 * @param op a function to extract the list from the property
 * @param transform a function to transform the list elements
 */
fun <X, Y, Z> createListBinding(obj: ObservableValue<X>, op: (X?) -> List<Y>, transform: (X, Y) -> Z): ObservableList<Z> {
    var listeningList = op(obj.value) as? ObservableList<Y>
    val resultList = observableListOf(listeningList?.map { transform(obj.value, it) } ?: listOf())
    val listener = ListChangeListener<Y> { resultList.setAll(it.list.map { transform(obj.value, it) }) }
    listeningList?.addListener(listener)
    obj.onChange {
        listeningList?.removeListener(listener)
        if (it == null) {
            resultList.setAll()
            listeningList = null
        } else {
            val nueList = op(it)
            resultList.setAll(nueList.map { transform(obj.value, it) })
            listeningList = nueList as? ObservableList<Y>
            listeningList?.addListener(listener)
        }
    }
    return resultList
}

//endregion
