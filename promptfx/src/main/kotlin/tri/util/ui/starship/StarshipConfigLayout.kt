/*-
 * #%L
 * tri.promptfx:promptkt
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

