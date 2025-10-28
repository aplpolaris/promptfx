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

import de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon
import java.awt.Rectangle

/** Configuration for a view widget in the Starship UI. */
data class StarshipConfigWidget(
    /** Variable name to bind the widget to, with update received when [tri.ai.core.tool.ExecContext] updates its vars. */
    val varRef: String,
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
    val step: Int? = null,
    /** Title of the step. */
    val title: String? = null,
    /** Icon to show in the widget header. */
    val icon: FontAwesomeIcon? = null,
    /** Size of icon in widget header. */
    val iconSize: Int? = null,
    /** Explainer text to show in "help" mode. */
    val explain: String? = null,
    /** Options to be displayed in dropdowns for the user, with keys matching elements of the associated prompt template. */
    val options: Map<String, List<String>> = mapOf()
)
