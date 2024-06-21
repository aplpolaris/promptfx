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
import javafx.event.EventTarget
import javafx.scene.layout.Priority
import tornadofx.*
import tri.ai.text.chunks.TextDocMetadata
import tri.ai.text.chunks.process.GuessedMetadataObject
import tri.ai.text.chunks.process.MultipleGuessedMetadataObjects
import tri.ai.text.chunks.process.PdfMetadataGuesser.COMBINED
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
                        text(it.savedLabel) {
                            style = "-fx-font-style: italic; -fx-text-fill: light-gray"
                        }
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
                    propertyvalue(it)
                }
            }
        }
    }

    private fun EventTarget.propertyvalue(model: GmvEditablePropertyModel<*>) = textflow {
        paddingAll = 5.0
        vgrow = Priority.ALWAYS
        val updater: (Any) -> Unit = { it ->
            clear()
            when {
                it is List<*> -> {
                    it.dropLast(1).forEach { text(it.toString() + "\n") }
                    it.lastOrNull()?.let { text(it.toString()) }
                }
                it is Map<*, *> -> {
                    it.entries.dropLast(1).forEach { text("${it.key}: ${it.value}\n") }
                    it.entries.lastOrNull()?.let { text("${it.key}: ${it.value}") }
                }
                it.toString().isBlank() -> {
                    // do nothing
                }
                else -> {
                    text(it.toString().trim())
                }
            }
        }
        model.editingValue.onChange { updater(it!!) }
        updater(model.editingValue.value!!)
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
                println("${it.label}: ${it.savedValue.value}, ${it.editingValue.value}, ${it.editingSource.value}")
                initialProps.add(it)
                selectionStatus[it] = SimpleBooleanProperty(false)
            }
        }
    }
}

/** Convert TextDocMetadata to a list of GMVs. */
fun TextDocMetadata.asGmvPropList(label: String) = MultipleGuessedMetadataObjects(toGuessedMetadataObject(label), listOf()).asGmvPropList()

/**
 * Convert GMO to a list of GMVs. Initial values are set to the first object, and the remaining values are used as alternates.
 * Properties whose value in the first object is null are not included.
 */
fun MultipleGuessedMetadataObjects.asGmvPropList(): List<GmvEditablePropertyModel<Any?>> {
    fun labeledValues(op: (GuessedMetadataObject) -> Any?): List<Pair<Any?, String>> {
        val result = mutableListOf<Pair<Any?, String>>()
        result.add(op(combined) to COMBINED)
        sources.forEach {
            val value = op(it)
            if (value != null && value.toString().isNotBlank()) {
                result.add(value to it.label)
            }
        }
        return result
    }

    fun propertyModel(key: String, op: (GuessedMetadataObject) -> Any?): GmvEditablePropertyModel<Any?> =
        GmvEditablePropertyModel(key, op(combined), labeledValues(op))

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

        combined.other.forEach { (k, _) ->
            add(propertyModel(k) { it.other[k] })
        }
    }.filter {
        it.editingValue.value != null
    }
}
