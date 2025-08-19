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
package tri.util.ui

import de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon
import de.jensd.fx.glyphs.fontawesome.FontAwesomeIconView
import javafx.beans.binding.BooleanExpression
import javafx.beans.property.Property
import javafx.beans.property.SimpleBooleanProperty
import javafx.beans.property.SimpleIntegerProperty
import javafx.beans.property.SimpleStringProperty
import javafx.beans.value.ObservableValue
import javafx.beans.value.WritableValue
import javafx.collections.ListChangeListener
import javafx.collections.ObservableList
import javafx.embed.swing.SwingFXUtils
import javafx.event.EventTarget
import javafx.scene.control.*
import javafx.scene.image.Image
import javafx.scene.input.DataFormat
import javafx.scene.input.TransferMode
import javafx.scene.paint.Color
import javafx.scene.text.Text
import javafx.scene.text.TextFlow
import javafx.stage.Modality
import javafx.stage.StageStyle
import tornadofx.*
import tri.ai.prompt.PromptDef
import tri.ai.prompt.PromptLibrary
import tri.promptfx.PromptFxConfig
import tri.promptfx.PromptFxGlobals
import tri.promptfx.PromptFxGlobals.lookupPrompt
import tri.promptfx.api.ImagesView
import tri.promptfx.promptFxFileChooser
import tri.util.loggerFor
import tri.util.warning
import java.io.File
import java.io.IOException
import javax.imageio.ImageIO

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

//region IMAGES

fun UIComponent.saveToFile(image: Image) {
    promptFxFileChooser(
        dirKey = PromptFxConfig.DIR_KEY_IMAGE,
        title = "Save to File",
        filters = arrayOf(PromptFxConfig.FF_PNG, PromptFxConfig.FF_ALL),
        mode = FileChooserMode.Save
    ) {
        it.firstOrNull()?.let {
            writeImageToFile(image, it)
            information("Image saved to file: ${it.name}", owner = primaryStage)
        }
    }
}

/** Writes an [Image] to a [File]. */
fun writeImageToFile(image: Image, file: File): Boolean = try {
    file.outputStream().use { os ->
        ImageIO.write(SwingFXUtils.fromFXImage(image, null), file.extension, os)
    }
    true
} catch (x: IOException) {
    loggerFor<ImagesView>().warning("Error saving image to file: $file", x)
    false
}

/** Copies an image to a clipboard. */
fun UIComponent.copyToClipboard(image: Image) {
    // the original image doesn't seem to copy to clipboard properly, so cycle it through [BufferedImage]
    val image2 = SwingFXUtils.fromFXImage(image, null)
    val fxImage = SwingFXUtils.toFXImage(image2, null)
    clipboard.put(DataFormat.IMAGE, fxImage)
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
fun EventTarget.templatemenubutton(template: SimpleStringProperty, isNestedByCategory: Boolean = false, promptFilter: (PromptDef) -> Boolean = { true }) =
    if (isNestedByCategory) {
        nestedlistmenubutton(
            items = {
                PromptFxGlobals.promptLibrary.list(promptFilter)
                    .groupBy { it.category ?: "Uncategorized" }
                    .mapValues { it.value.toList().map { it.bareId }.sorted() }
                    .toSortedMap()
            },
            action = { template.set(lookupPrompt(it).template) }
        )
    } else
        listmenubutton(
            items = { PromptFxGlobals.promptLibrary.list(promptFilter).map { it.id }.sorted() },
            action = { template.set(lookupPrompt(it).template) }
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

/**
 * Creates a [menubutton] with the provided nested items and action.
 * The list is dynamically updated each time the button is shown.
 */
fun EventTarget.nestedlistmenubutton(items: () -> Map<String, List<String>>, action: (String) -> Unit) =
    menubutton("", FontAwesomeIconView(FontAwesomeIcon.LIST)) {
        setOnShowing {
            this.items.clear()
            items().forEach { (category, keys) ->
                if (keys.size == 1) {
                    item("$category/${keys.first()}") {
                        action { action(keys.first()) }
                    }
                } else {
                    menu(category) {
                        keys.forEach { key ->
                            item(key) {
                                action { action(key) }
                            }
                        }
                    }
                }
            }
        }
    }

/** Slider with editable label. */
fun EventTarget.sliderwitheditablelabel(range: IntRange, property: SimpleIntegerProperty) {
    slider(range, property)
    val tokenLabel = label(property.asString())
    tokenLabel.apply {
        setOnMouseClicked {
            val textField = TextField(property.value.toString()).apply {
                prefColumnCount = 1
                setOnAction {
                    property.value = text.toIntOrNull()?.coerceIn(range) ?: property.value
                    replaceWith(tokenLabel)
                }
                focusedProperty().addListener { _, _, focused ->
                    if (!focused) {
                        property.value = text.toIntOrNull()?.coerceIn(range) ?: property.value
                        replaceWith(tokenLabel)
                    }
                }
            }
            replaceWith(textField)
            textField.requestFocus()
            textField.selectAll()
        }
    }
}

//endregion

//region DIALOGS

/**
 * Shows a dialog with the given [Image].
 * Click to close dialog.
 * Context menu provides a copy option.
 */
fun UIComponent.showImageDialog(image: Image) {
    val d = dialog(
        modality = Modality.APPLICATION_MODAL,
        stageStyle = StageStyle.UNDECORATED,
        owner = primaryStage
    ) {
        imageview(image) {
            onLeftClick { close() }
            contextmenu {
                item("Copy Image to Clipboard") {
                    action { copyToClipboard(image) }
                }
            }
        }
        padding = insets(0)
        form.padding = insets(1)
        form.background = Color.WHITE.asBackground()
    }
    // center dialog on window (dialog method doesn't do this because it adds content after centering on owner)
    d?.owner?.let {
        d.x = it.x + (it.width / 2) - (d.scene.width / 2)
        d.y = it.y + (it.height / 2) - (d.scene.height / 2)
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

fun <T : Any> booleanListBindingOr(list: ObservableList<T>, defaultValue: Boolean = false, itemToBooleanExpr: T.() -> BooleanExpression): BooleanExpression {
    val facade = SimpleBooleanProperty()
    fun rebind() {
        if (list.isEmpty()) {
            facade.unbind()
            facade.value = defaultValue
        } else {
            facade.cleanBind(list.map(itemToBooleanExpr).reduce { a, b -> a.or(b) })
        }
    }
    list.onChange { rebind() }
    rebind()
    return facade
}

//endregion

//region ListView BINDINGS

/** Binds a single selected value of a [ListView] to an existing [Property]. */
fun <X, T> ListView<X>.bindSelectionBidirectional(property: T) where T : WritableValue<X>, T : Property<X> {
    selectionModel.selectionMode = SelectionMode.SINGLE
    selectionModel.selectedItemProperty().onChange { property.value = it }
    property.onChange {
        if (it == null)
            selectionModel.clearSelection()
        else
            selectionModel.select(it)
    }
}

/** Binds multiple selected values of a [ListView] to an existing [ObservableList]. */
fun <X> ListView<X>.bindSelectionBidirectional(property: ObservableList<X>) {
    selectionModel.selectionMode = SelectionMode.MULTIPLE
    var isUpdating = false
    selectionModel.selectedItems.onChange {
        isUpdating = true
        property.setAll(it.list.toList())
        isUpdating = false
    }
    property.onChange {
        if (!isUpdating) {
            val indices = it.list.map { items.indexOf(it) }.toIntArray()
            if (indices.isEmpty())
                selectionModel.clearSelection()
            else
                selectionModel.selectIndices(indices[0], *indices.drop(1).toIntArray())
        }
    }
}

//endregion
