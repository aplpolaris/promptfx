package tri.util.ui.starship

import de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon
import java.awt.Rectangle

/** Configuration for a view widget in the Starship UI. */
data class StarshipConfigWidget(
    /** Variable name to bind the widget to. */
    val varName: String,
    /** Type of widget to display. */
    val widgetType: StarshipWidgetType,
    /** Position within grid. */
    val pos: Rectangle = Rectangle(1, 1, 1, 1),
    /** Overlay settings. */
    val overlay: StarshipWidgetOverlay
)

/** Types of widgets available in Starship UI. */
enum class StarshipWidgetType {
    ANIMATING_TEXT,
    ANIMATING_TEXT_VERTICAL,
    ANIMATING_THUMBNAILS
}

/** Overlay settings for Starship UI widgets. */
data class StarshipWidgetOverlay(
    /** Number of other character used to identify the widget. */
    val step: Int,
    /** Title of the step. */
    val title: String? = null,
    /** Icon to show in the widget header. */
    val icon: FontAwesomeIcon? = null,
    /** Size of icon in widget header. */
    val iconSize: Int? = null,
    /** Explainer text to show in "help" mode. */
    val explain: String,
    /** Options to be displayed in dropdowns for the user, with keys matching elements of the associated prompt template. */
    val options: Map<String, List<String>> = mapOf()
)