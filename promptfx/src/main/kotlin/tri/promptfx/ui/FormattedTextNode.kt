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
