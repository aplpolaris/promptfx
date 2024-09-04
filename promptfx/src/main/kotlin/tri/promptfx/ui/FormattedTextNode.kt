package tri.promptfx.ui

import javafx.scene.Node
import javafx.scene.control.Hyperlink
import javafx.scene.text.Text
import tornadofx.*

/** A text node within [FormattedText]. */
class FormattedTextNode(
    val text: String,
    val style: String? = null,
    val hyperlink: String? = null
) {

    /** Splits this node by a [Regex], keeping the same style and hyperlink. */
    fun splitOn(find: Regex, replace: (String) -> FormattedTextNode): List<FormattedTextNode> {
        if (hyperlink != null)
            return listOf(this)
        val result = mutableListOf<FormattedTextNode>()
        var index = 0
        find.findAll(text).forEach {
            val text0 = text.substring(index, it.range.first)
            if (text0.length > 1) {
                result += FormattedTextNode(text0, style, hyperlink)
            }
            result += replace(it.value)
            index = it.range.last + 1
        }
        result += FormattedTextNode(text.substring(index), style, hyperlink)
        return result
    }

    /** Convert a formatted text node to an FX node. */
    fun toFxNode(hyperlinkOp: (String) -> Unit): Node =
        when (hyperlink) {
            null -> Text(text).also {
                it.style = style
            }
            else -> Hyperlink(text).also {
                it.style = style
                it.action { hyperlinkOp(text) }
            }
        }

}