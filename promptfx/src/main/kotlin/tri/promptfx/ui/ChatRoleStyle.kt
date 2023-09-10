package tri.promptfx.ui

import de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon
import javafx.scene.paint.Color

/** Role type style settings. */
enum class ChatRoleStyle(val glyph: FontAwesomeIcon, val glyphStyle: String, val background: Color, val text: Color, val rightAlign: Boolean) {
    USER(FontAwesomeIcon.USER, "-fx-fill: black;", Color.LIGHTGRAY, Color.BLACK, false),
    ASSISTANT(FontAwesomeIcon.ROCKET, "-fx-fill: darkgreen;", Color.DARKGREEN, Color.LIGHTGREEN, true),
    SYSTEM(FontAwesomeIcon.DESKTOP, "-fx-fill: gray;", Color.LIGHTGRAY, Color.DARKGRAY, true),
    ERROR(FontAwesomeIcon.EXCLAMATION_TRIANGLE, "-fx-fill: red;", Color.LIGHTGRAY, Color.RED, true);
}