/*-
 * #%L
 * tri.promptfx:promptkt
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
package tri.util.ui.starship

import javafx.beans.value.ObservableValue
import javafx.event.EventTarget
import tornadofx.asObservable
import tornadofx.bindChildren
import tornadofx.onChange
import tornadofx.vbox
import tri.ai.text.docs.FormattedText
import tri.promptfx.docs.DocumentQaView
import tri.promptfx.ui.toFxNodes
import tri.util.ui.AnimatingTextFlow
import tri.util.ui.AnimatingThumbnailBox
import tri.util.ui.DocumentThumbnail
import tri.util.ui.DocumentUtils
import tri.util.ui.starship.Chromify.chromify

/** Common Starship widget view elements. */
sealed class StarshipWidget(protected val context: StarshipWidgetLayoutContext) {
    protected fun StarshipConfigWidget.px() = context.px(this)
    protected fun StarshipConfigWidget.py() = context.py(this)
    protected fun StarshipConfigWidget.pw() = context.pw(this)
    protected fun StarshipConfigWidget.ph() = context.ph(this)
}

/** Container widget for grouping other widgets. */
class ContainerWidget(context: StarshipWidgetLayoutContext, val widgets: List<StarshipConfigWidget>) : StarshipWidget(context) {
    fun addTo(target: EventTarget) {
        target.vbox(54.0) {
            val w = widgets.first()
            layoutX = w.px()
            layoutY = w.py()
            prefWidth = w.pw()
            bindChildren(widgets.asObservable()) { w ->
                createWidget(w, context, isDynamic = true).root
            }
        }
    }
}

/** View element for large display text. */
class AnimatingTextWidget(context: StarshipWidgetLayoutContext,
                          widget: StarshipConfigWidget, value: ObservableValue<String>,
                          isDynamic: Boolean,
                          buttonText: ObservableValue<String>? = null, buttonAction: (() -> Unit)? = null): StarshipWidget(context) {
    val textFlow = AnimatingTextFlow()

    init {
        textFlow.root.isMouseTransparent = true
        if (isDynamic) {
            textFlow.root.layoutX = widget.px()
            textFlow.root.layoutY = widget.py()
            textFlow.root.prefWidth = widget.pw()
        } else {
            textFlow.root.resizeRelocate(widget.px(), widget.py(), widget.pw(), widget.ph())
        }
        textFlow.updatePrefWidth(widget.pw())
        val iconSize = widget.overlay.iconSize?.toDouble() ?: 12.0
        textFlow.updateFontSize(iconSize)
        context.chromePane.chromify(textFlow.root, iconSize * 0.75, widget.overlay.title ?: "", widget.overlay.icon, buttonText, buttonAction)
        value.onChange { textFlow.animateText(FormattedText(it ?: "").toFxNodes()) }
    }
}

/** View element for displaying a list of thumbnails. */
class ThumbnailWidget(context: StarshipWidgetLayoutContext, widget: StarshipConfigWidget): StarshipWidget(context) {
    private val THUMB_COUNT = 6
    private val THUMB_SPACING = 20.0
    private val DOC_THUMBNAIL_SIZE = ((widget.pw() - (THUMB_COUNT - 1) * THUMB_SPACING) / THUMB_COUNT).toInt()

    val thumbnailBox = AnimatingThumbnailBox { }.apply {
        id = "starship-thumbnails"
        isMouseTransparent = true
        resizeRelocate(widget.px(), widget.py(), widget.pw(), widget.ph())
        spacing = THUMB_SPACING
    }

    init {
        if (context.baseComponent is DocumentQaView) {
            context.baseComponent.snippets.onChange {
                val thumbs = context.baseComponent.snippets.map { it.document.browsable()!! }.toSet()
                    .map { DocumentThumbnail(it, DocumentUtils.documentThumbnail(it, DOC_THUMBNAIL_SIZE, true)) }
                thumbnailBox.animateThumbs(thumbs.take(6))
            }
        }

        context.results.activeStepVar.onChange {
            if (it.isNullOrBlank())
                thumbnailBox.animateThumbs(emptyList())
        }
    }
}
