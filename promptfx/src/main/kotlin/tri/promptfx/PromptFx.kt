/*-
 * #%L
 * promptfx-0.1.0-SNAPSHOT
 * %%
 * Copyright (C) 2023 Johns Hopkins University Applied Physics Laboratory
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
package tri.promptfx

import javafx.scene.Cursor
import javafx.scene.paint.Color
import javafx.scene.text.TextAlignment
import org.apache.commons.logging.LogFactory
import org.apache.commons.logging.impl.Jdk14Logger
import org.apache.pdfbox.pdmodel.font.PDSimpleFont
import tornadofx.*
import tri.promptfx.apps.DocumentQaView
import java.util.logging.Level

class PromptFx : App(PromptFxWorkspace::class, PromptFxStyles::class) {
    override fun onBeforeShow(view: UIComponent) {
        workspace.dock<DocumentQaView>()
    }
    override fun stop() {
        workspace.find<PromptFxController>().close()
        super.stop()
    }
}

fun main(args: Array<String>) {
    (LogFactory.getLog(PDSimpleFont::class.java) as? Jdk14Logger)?.apply {
        logger.level = Level.SEVERE
    }

    launch<PromptFx>(args)
}

/** Stylesheet for the application. */
class PromptFxStyles: Stylesheet() {
    companion object {
        val transparentTextArea by cssclass()
        val scrollPane by cssclass()
        val content by cssclass()
    }

    init {
        transparentTextArea {
            cursor = Cursor.TEXT
            textFill = Color.WHITE
            fontFamily = "Serif"
            textAlignment = TextAlignment.CENTER

            scrollPane {
                content {
                    backgroundColor += Color.TRANSPARENT
                    borderColor += box(Color.TRANSPARENT)
                }
            }
        }
    }
}
