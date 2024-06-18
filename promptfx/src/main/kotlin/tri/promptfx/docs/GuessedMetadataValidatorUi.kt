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
package tri.promptfx.docs

import de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon
import javafx.beans.property.BooleanProperty
import javafx.geometry.Pos
import javafx.scene.control.ButtonBar
import javafx.scene.layout.Priority
import tornadofx.*
import tri.ai.text.chunks.process.GuessedMetadataObject
import tri.util.ui.graphic

/**
 * UI that shows a series of key-value pairs discovered by a "metadata guesser" and user can accept/reject them.
 */
class GuessedMetadataValidatorUi(data: List<GuessedMetadataObject>, val onClose: (GuessedMetadataValidatorModel) -> Unit):
    UIComponent("Confirm Metadata") {

    /** Data model for the view, tracking which properties are selected by the user. */
    val model = GuessedMetadataValidatorModel().apply {
        initialProps.setAll(data.asGmvPropList())
    }

    override val root = vbox(10) {
        paddingAll = 20.0
        prefWidth = 800.0
        prefHeight = 600.0
        text("Select which metadata properties to save with the article") {
            style = "-fx-font-size: 24"
        }
        listview(model.initialProps) {
            vgrow = Priority.ALWAYS
            cellFormat {
                graphic = vbox(5) {
                    maxWidth = 600.0
                    hbox(5, Pos.CENTER_LEFT) {
                        text(it.key) {
                            style = "-fx-font-weight: bold; -fx-font-size: 16"
                        }
                        button("", FontAwesomeIcon.ANGLE_LEFT.graphic) {
                            isDisable = it.valueList.size < 2
                            action {
                                it.previousValue()
                                this@listview.refresh()
                            }
                        }
                        button("", FontAwesomeIcon.ANGLE_RIGHT.graphic) {
                            isDisable = it.valueList.size < 2
                            action {
                                it.nextValue()
                                this@listview.refresh()
                            }
                        }
                        checkbox("Save") {
                            isSelected = model.selectionStatus[it]?.value ?: false
                            model.bindSelection(it, selectedProperty())
                        }
                        button("", FontAwesomeIcon.MINUS_CIRCLE.graphic) {
                            action {
                                model.removeEntry(it)
                                this@listview.selectionModel.clearSelection()
                            }
                        }
                    }
                    textflow {
                        vgrow = Priority.ALWAYS
                        isWrapText = true
                        when (val v = it.value) {
                            is List<*> ->
                                v.forEach {
                                    text(it.toString() + "\n")
                                }
                            is Map<*, *> ->
                                v.forEach { (k, v) ->
                                    text("$k: $v\n")
                                }
                            else ->
                                text(v.toString())
                        }
                    }
                }
            }
        }
        buttonbar {
            paddingAll = 10.0
            button("OK", ButtonBar.ButtonData.OK_DONE) {
                action {
                    close()
                    onClose(model)
                }
            }
            button("Cancel", ButtonBar.ButtonData.CANCEL_CLOSE) {
                action {
                    close()
                }
            }
        }
    }

}

/** Model for the metadata validator view. */
class GuessedMetadataValidatorModel: ScopedInstance, Component() {
    val initialProps = observableListOf<GmvProp>()
    val selectionStatus = mutableMapOf<GmvProp, BooleanProperty>()

    fun selectedValues(): Map<String, Any> = initialProps
        .filter { selectionStatus[it]?.value == true }
        .associate { it.key to it.value }
        .filterValues { it != null } as Map<String, Any>

    fun bindSelection(it: GmvProp, prop: BooleanProperty) {
        selectionStatus[it] = prop
    }

    fun removeEntry(it: GmvProp) {
        initialProps.remove(it)
        selectionStatus.remove(it)
    }

}

/** A single GMV property, with three possible value types. */
class GmvProp(
    val key: String,
    val strValue: String? = null,
    val listValue: List<String>? = null,
    val mapValue: Map<String, String>? = null,
    val altValues: List<Any>? = null
) {
    var value = strValue ?: listValue ?: mapValue
    val valueList = (listOf(value) + (altValues ?: listOf())).toSet().toList()

    fun isNullAndEmpty() = strValue == null && listValue.isNullOrEmpty() && mapValue.isNullOrEmpty()

    fun nextValue() {
        val index = valueList.indexOf(value)
        value = valueList[(index + 1) % valueList.size]
    }

    fun previousValue() {
        val index = valueList.indexOf(value)
        value = if (index == -1)
            valueList[0]
        else
            valueList[(index - 1 + valueList.size) % valueList.size]
    }
}

/** Convert GMO to a list of GMVs. */
fun List<GuessedMetadataObject>.asGmvPropList(): List<GmvProp> {
    val first = first()
    val remaining = drop(1)
    return mutableListOf<GmvProp>().apply {
        add(GmvProp("title", first.title, altValues = remaining.mapNotNull { it.title }))
        add(GmvProp("subtitle", first.subtitle, altValues = remaining.mapNotNull { it.subtitle }))
        add(GmvProp("authors", null, first.authors, altValues = remaining.mapNotNull { it.authors }.filter { it.isNotEmpty() }))
        add(GmvProp("date", first.date, altValues = remaining.mapNotNull { it.subtitle }))
        add(GmvProp("keywords", null, first.keywords, altValues = remaining.mapNotNull { it.keywords }.filter { it.isNotEmpty() }))
        add(GmvProp("abstract", first.abstract, altValues = remaining.mapNotNull { it.abstract }))
        add(GmvProp("executiveSummary", first.executiveSummary, altValues = remaining.mapNotNull { it.executiveSummary }))
        add(GmvProp("sections", null, first.sections, altValues = remaining.mapNotNull { it.sections }.filter { it.isNotEmpty() }))
        add(GmvProp("captions", null, first.captions, altValues = remaining.mapNotNull { it.captions }.filter { it.isNotEmpty() }))
        add(GmvProp("references", null, first.references, altValues = remaining.mapNotNull { it.references }.filter { it.isNotEmpty() }))
        first.other.forEach { (k, v) ->
            add(GmvProp(k, v as? String, v as? List<String>, v as? Map<String, String>,
                altValues = remaining.mapNotNull { it.other[k] }))
        }
        removeIf { it.isNullAndEmpty() }
    }
}
