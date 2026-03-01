/*-
 * #%L
 * tri.promptfx:promptfx
 * %%
 * Copyright (C) 2023 - 2026 Johns Hopkins University Applied Physics Laboratory
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
import javafx.beans.property.SimpleBooleanProperty
import javafx.collections.ObservableList
import javafx.scene.Node
import javafx.scene.control.MenuButton
import javafx.scene.control.MenuItem
import javafx.scene.control.ToolBar
import tornadofx.*

/** Adds a menu with checklist of items that can be used to trigger a filter operation. Optionally supports icons. */
fun <X> MenuButton.checklistmenu(label: String, itemList: ObservableList<Pair<X, SimpleBooleanProperty>>, iconLookup: (X) -> Node? = { null }, refilter: () -> Unit) {
    menu(label) {
        fun updateMenu() {
            items.clear()
            itemList.forEach { (key, prop) ->
                checkmenuitem(key.toString().replace('_', ' '), graphic = iconLookup(key), selected = prop) { action { refilter() } }
            }
            separator()
            item("Select All") { actionselectall(itemList, refilter) }
            item("Select None") { actionselectnone(itemList, refilter) }
        }
        itemList.onChange { updateMenu() }
        updateMenu()
    }
}

/** Adds a menu with checklist of items that can be used to trigger a filter operation. Optionally supports icons. */
fun <X> ToolBar.checklistmenu(label: String, itemList: ObservableList<Pair<X, SimpleBooleanProperty>>, iconLookup: (X) -> Node? = { null }, refilter: () -> Unit) {
    menubutton(label) {
        fun updateMenu() {
            items.clear()
            itemList.forEach { (key, prop) ->
                checkmenuitem(key.toString().replace('_', ' '), graphic = iconLookup(key), selected = prop) { action { refilter() } }
            }
            separator()
            item("Select All") { actionselectall(itemList, refilter) }
            item("Select None") { actionselectnone(itemList, refilter) }
        }
        itemList.onChange { updateMenu() }
        updateMenu()
    }
}

private fun <X> MenuItem.actionselectall(itemList: ObservableList<Pair<X, SimpleBooleanProperty>>, refilter: () -> Unit) {
    action {
        itemList.forEach { it.second.set(true) }
        refilter()
    }
}

private fun <X> MenuItem.actionselectnone(itemList: ObservableList<Pair<X, SimpleBooleanProperty>>, refilter: () -> Unit) {
    action {
        itemList.forEach { it.second.set(false) }
        refilter()
    }
}

/** Adds a menu with sort options. */
fun <X, Y> MenuButton.sortmenu(label: String, model: FilterSortModel<X>, attribute: (X) -> Y) {
    menu(label) {
        item("Ascending", graphic = FontAwesomeIcon.SORT_ALPHA_ASC.graphic) {
            action { model.sortBy(label, attribute, true) }
        }
        item("Descending", graphic = FontAwesomeIcon.SORT_ALPHA_DESC.graphic) {
            action { model.sortBy(label, attribute, false) }
        }
    }
}
