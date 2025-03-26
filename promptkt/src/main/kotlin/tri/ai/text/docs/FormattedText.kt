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
package tri.ai.text.docs

import com.fasterxml.jackson.annotation.JsonIgnore

/** Basic markup text that may contain formatted elements and links. */
class FormattedText(val nodes: List<FormattedTextNode>) {
    constructor(text: String) : this(listOf(FormattedTextNode(text)))

    @get:JsonIgnore
    var hyperlinkOp: (String) -> Unit = { }

    override fun toString() = nodes.joinToString("") { it.text }
}

/** A node within [FormattedText]. */
data class FormattedTextNode(
    val text: String,
    val style: String? = null,
    val hyperlink: String? = null,
    val tooltip: String? = null
)

//region CONVERSIONS

/** Convert a [FormattedText] to HTML string. */
fun FormattedText.toHtml(): String {
    val text = StringBuilder("<html>\n")
    nodes.forEach {
        val style = it.style ?: ""
        val bold = style.contains("-fx-font-weight: bold")
        val italic = style.contains("-fx-font-style: italic")
        val color = if (style.contains("-fx-fill:"))
            style.substringAfter("-fx-fill:").let { it.substringBefore(";", it).trim() }
        else null
        val prefix = (if (bold) "<b>" else "") + (if (italic) "<i>" else "") +
                (if (color != null) "<font color=\"$color\">" else "")
        val textWithBreaks = it.text.replace("\n", "<br>")
        val suffix = (if (color != null) "</font>" else "") +
                (if (italic) "</i>" else "") + (if (bold) "</b>" else "")
        text.append(prefix)
        if (it.hyperlink != null)
            text.append("<a href=\"$textWithBreaks\">${it.hyperlink}</a>")
        else
            text.append(textWithBreaks)
        text.append(suffix)
    }
    return text.toString()
}

//endregion

//region SPLITTING UTILS

/** Splits this node by a [Regex], keeping the same style and hyperlink. */
fun FormattedTextNode.splitOn(find: Regex, replace: (String) -> FormattedTextNode): List<FormattedTextNode> {
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

/** Splits all text elements on a given search string. */
fun MutableList<FormattedTextNode>.splitOn(find: String, replace: (String) -> FormattedTextNode) =
    splitOn(Regex.fromLiteral(find), replace)

/** Splits all text elements on a given search string. */
fun MutableList<FormattedTextNode>.splitOn(find: Regex, replace: (String) -> FormattedTextNode) =
    toList().forEach {
        val newNodes = it.splitOn(find, replace)
        if (newNodes != listOf(it)) {
            addAll(indexOf(it), newNodes)
            remove(it)
        }
    }

//endregion