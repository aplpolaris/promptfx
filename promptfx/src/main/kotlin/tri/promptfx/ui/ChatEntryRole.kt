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
package tri.promptfx.ui

import de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon
import javafx.scene.paint.Color
import tri.ai.core.MChatRole

/** Role type style settings. */
enum class ChatEntryRole(val role: MChatRole?, val glyph: FontAwesomeIcon, val glyphStyle: String, val background: Color, val text: Color, val rightAlign: Boolean) {
    USER(MChatRole.User, FontAwesomeIcon.USER, "-fx-fill: black;", Color.LIGHTGRAY, Color.BLACK, false),
    ASSISTANT(MChatRole.Assistant, FontAwesomeIcon.ROCKET, "-fx-fill: darkgreen;", Color.DARKGREEN, Color.LIGHTGREEN, true),
    SYSTEM(MChatRole.System, FontAwesomeIcon.DESKTOP, "-fx-fill: gray;", Color.LIGHTGRAY, Color.DARKGRAY, true),
    ERROR(null, FontAwesomeIcon.EXCLAMATION_TRIANGLE, "-fx-fill: red;", Color.LIGHTGRAY, Color.RED, true);
}

/** Convert [MChatRole] to a [ChatEntryRole]. */
fun MChatRole.toChatRoleStyle() = ChatEntryRole.values().firstOrNull { it.role == this } ?: ChatEntryRole.ERROR
