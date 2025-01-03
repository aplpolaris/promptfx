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