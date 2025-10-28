package tri.util.ui.starship

import de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon

/** Configuration for UI elements of the Starship view. Works with [StarshipConfigContent] to set up the full Starship UI. */
data class StarshipConfigLayout(
    val backgroundIcon: FontAwesomeIcon = FontAwesomeIcon.STAR_ALT,
    val backgroundIconCount: Int = 1000,
    val numCols: Int = 3,
    val numRows: Int = 3,
    val isShowGrid: Boolean = true,
    val widgets: List<StarshipConfigWidget> = listOf<StarshipConfigWidget>()
)

