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
import javafx.event.EventTarget
import javafx.scene.layout.Priority
import tornadofx.*
import tri.util.fine
import tri.util.ui.booleanListBindingOr
import tri.util.ui.graphic

/** UI that shows a series of key-value pairs discovered by a "metadata guesser" and user can accept/reject them. */
class MetadataValidatorUi : UIComponent("Confirm Metadata") {

    /** Data model for the view, tracking which properties are selected by the user. */
    val model: MetadataValidatorModel by inject()

    override val root = scrollpane {
        vgrow = Priority.ALWAYS
        isFitToWidth = true
        vbox(5) {
            children.bind(model.props) {
                vbox(5) {
                    toolbar {
                        text(it.name) {
                            style = "-fx-font-weight: bold; -fx-font-size: 16"
                        }
                        spacer()
                        text(it.changeLabel) {
                            style = "-fx-font-style: italic; -fx-text-fill: light-gray"
                        }
                        button("", FontAwesomeIcon.ANGLE_LEFT.graphic) {
                            disableWhen(it.isValueCyclable.not() or it.isDeletePending)
                            action { it.previousValue() }
                        }
                        button("", FontAwesomeIcon.ANGLE_RIGHT.graphic) {
                            disableWhen(it.isValueCyclable.not() or it.isDeletePending)
                            action { it.nextValue() }
                        }
                        checkbox("Update", it.isUpdatePending) {
                            isVisible = it.isEditable
                            graphic = FontAwesomeIcon.CHECK_CIRCLE_ALT.graphic
                            isManaged = it.isEditable
                            disableWhen(it.isOriginal or it.isDeletePending)
                        }
                        checkbox("Remove", it.isDeletePending) {
                            graphic = FontAwesomeIcon.TIMES_CIRCLE_ALT.graphic
                            visibleWhen(it.isDeletable)
                            managedWhen(it.isDeletable)
                            enableWhen(it.isDeletable)
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
                    it.entries.toList().dropLast(1).forEach { text("${it.key}: ${it.value}\n") }
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
        updater(model.editingValue.value ?: model.originalValue.value ?: "no value")
    }

}

/** Model for the metadata validator view. */
class MetadataValidatorModel: ScopedInstance, Component() {

    val props = observableListOf<GmvEditablePropertyModel<Any?>>()
    val isChanged = booleanListBindingOr(props, false, GmvEditablePropertyModel<*>::isAnyChangePending)

    /** Return a map of all properties that have been selected and edited. */
    fun savedValues(): Map<String, Any> = props
        .associate { it.name to it.originalValue.value }
        .filter { it.value != null } as Map<String, Any>

    /** Merge a secondary model with the current one. Any properties with existing names will be augmenting by adding to the possible list of values. */
    fun merge(properties: List<GmvEditablePropertyModel<Any?>>, filterUnique: Boolean) {
        properties.forEach {
            val existing = props.find { p -> p.name == it.name }
            if (existing != null) {
                existing.addAlternateValues(it.getAlternateValueList(), filterUnique)
            } else {
                fine<MetadataValidatorModel>("MERGE PROPERTIES FOR ${it.name}: ${it.originalValue}, ${it.editingValue.value}")
                props.add(it)
            }
        }
    }

    /** Applies any pending changes, saving them by updating the original value, and removing any elements marked for removal. */
    fun applyPendingChanges() {
        props.forEach {
            if (it.isUpdatePending.value)
                it.applyChanges()
        }
        props.removeIf { it.isDeletePending.value }
    }

    /** Revert any pending changes. */
    fun revertPendingChanges() {
        props.forEach { it.revertChanges() }
    }
}