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
package tri.promptfx.library

import de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon
import javafx.beans.property.BooleanProperty
import javafx.beans.property.SimpleBooleanProperty
import javafx.scene.layout.Priority
import tornadofx.*
import tri.ai.text.chunks.TextDocMetadata
import tri.ai.text.chunks.process.GuessedMetadataObject
import tri.ai.text.chunks.process.PdfMetadataGuesser.toGuessedMetadataObject
import tri.util.ui.graphic

/** UI that shows a series of key-value pairs discovered by a "metadata guesser" and user can accept/reject them. */
class MetadataValidatorUi : UIComponent("Confirm Metadata") {

    /** Data model for the view, tracking which properties are selected by the user. */
    val model: MetadataValidatorModel by inject()

    override val root = scrollpane {
        vgrow = Priority.ALWAYS
        isFitToWidth = true
        vbox(5) {
            children.bind(model.initialProps) {
                vbox(5) {
                    toolbar {
                        text(it.label) {
                            style = "-fx-font-weight: bold; -fx-font-size: 16"
                        }
//                        checkbox("Save") {
//                            isSelected = model.selectionStatus[it]?.value ?: false
//                            model.bindSelection(it, selectedProperty())
//                        }
                        spacer()
                        text(it.editingSource) {
                            style = "-fx-font-style: italic; -fx-text-fill: light-gray"
                        }
                        button("", FontAwesomeIcon.ANGLE_LEFT.graphic) {
                            disableWhen(it.supportsValueCycling.not())
                            action { it.previousValue() }
                        }
                        button("", FontAwesomeIcon.ANGLE_RIGHT.graphic) {
                            disableWhen(it.supportsValueCycling.not())
                            action { it.nextValue() }
                        }
                        button("", FontAwesomeIcon.SAVE.graphic) {
                            disableWhen(it.isSaved)
                            action { it.saveValue() }
                        }
                        button("", FontAwesomeIcon.MINUS_CIRCLE.graphic) {
                            action { model.removeEntry(it) }
                        }
                    }
                    textflow {
                        paddingAll = 5.0
                        vgrow = Priority.ALWAYS
                        val updater: (Any) -> Unit = { it ->
                            clear()
                            when (it) {
                                is List<*> -> it.map { text(it.toString() + "\n") }.let { it.last().text = it.last().text.trim() }
                                is Map<*, *> -> it.map { (k, v) -> text("$k: $v\n") }.let { it.last().text = it.last().text.trim() }
                                else -> text(it.toString().trim())
                            }
                        }
                        it.editingValue.onChange { updater(it!!) }
                        updater(it.editingValue.value!!)
                    }
                }
            }
        }
    }

}

/** Model for the metadata validator view. */
class MetadataValidatorModel: ScopedInstance, Component() {
    val initialProps = observableListOf<GmvEditablePropertyModel<Any?>>()
    val selectionStatus = mutableMapOf<GmvEditablePropertyModel<Any?>, BooleanProperty>()

    fun editingValues(): Map<String, Any> = initialProps
        .filter { selectionStatus[it]?.value == true && it.editingValue.value != null }
        .associate { it.label to it.editingValue.value!! }

    fun bindSelection(it: GmvEditablePropertyModel<Any?>, prop: BooleanProperty) {
        selectionStatus[it] = prop
    }

    fun removeEntry(it: GmvEditablePropertyModel<Any?>) {
        initialProps.remove(it)
        selectionStatus.remove(it)
    }

    /** Merge a secondary model with the current one. Any properties with existing names will be augmenting by adding to the possible list of values. */
    fun merge(properties: List<GmvEditablePropertyModel<Any?>>) {
        properties.forEach {
            val existing = initialProps.find { p -> p.label == it.label }
            if (existing != null) {
                existing.addAlternateValues(it.getAlternateValueList())
            } else {
                initialProps.add(it)
                selectionStatus[it] = SimpleBooleanProperty(false)
            }
        }
    }
}

/** Convert TextDocMetadata to a list of GMVs. */
fun TextDocMetadata.asGmvPropList(label: String) = listOf(toGuessedMetadataObject(label)).asGmvPropList()

/**
 * Convert GMO to a list of GMVs. Initial values are set to the first object, and the remaining values are used as alternates.
 * Properties whose value in the first object is null are not included.
 */
fun List<GuessedMetadataObject>.asGmvPropList(): List<GmvEditablePropertyModel<Any?>> {
    val first = first()

    fun labeledValues(op: (GuessedMetadataObject) -> Any?): List<Pair<Any?, String>> =
        mapNotNull { op(it) to it.label }
            .filter { it.first != null && it.first.toString().isNotBlank() }
    fun propertyModel(key: String, op: (GuessedMetadataObject) -> Any?) =
        GmvEditablePropertyModel(key, op(first), labeledValues(op))

    return mutableListOf<GmvEditablePropertyModel<Any?>>().apply {
        add(propertyModel("title") { it.title })
        add(propertyModel("subtitle") { it.subtitle })
        add(propertyModel("authors") { it.authors })
        add(propertyModel("date") { it.date })
        add(propertyModel("keywords") { it.keywords })
        add(propertyModel("abstract") { it.abstract })
        add(propertyModel("executiveSummary") { it.executiveSummary })
        add(propertyModel("sections") { it.sections })
        add(propertyModel("captions") { it.captions })
        add(propertyModel("references") { it.references })

        first.other.forEach { (k, _) ->
            add(propertyModel(k) { it.other[k] })
        }
    }.filter {
        it.editingValue.value != null
    }
}
