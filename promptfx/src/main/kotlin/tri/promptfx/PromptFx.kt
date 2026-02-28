/*-
 * #%L
 * tri.promptfx:promptfx
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
package tri.promptfx

import javafx.scene.Cursor
import javafx.scene.paint.Color
import javafx.scene.text.TextAlignment
import javafx.stage.Stage
import org.apache.commons.logging.LogFactory
import org.apache.commons.logging.impl.Jdk14Logger
import org.apache.pdfbox.pdmodel.font.PDSimpleFont
import tornadofx.*
import tri.promptfx.docs.DocumentQaView
import java.util.logging.Level
import kotlin.system.exitProcess

class PromptFx : App(PromptFxWorkspace::class, PromptFxStyles::class) {
    val promptFxConfig: PromptFxConfig by inject()

    override fun init() {
        promptFxConfig.isStarshipEnabled = parameters.raw.contains("starship")
    }

    override fun start(stage: Stage) {
        super.start(stage)
        // as of 0.10.0, workspace doesn't seem to be initialized properly unless this is set here. not sure what changed
        // (moved "onBeforeShow" with default view to the workspace code, since it didn't seem to be docking properly)
        scope.workspace(find<PromptFxWorkspace>())
    }

    override fun stop() {
        workspace.find<PromptFxController>().close()
        promptFxConfig.save()
        super.stop()

        // exit process after 2-second timer that runs in the background
        // to allow for any cleanup to occur
        Thread {
            Thread.sleep(2000)
            exitProcess(0)
        }.start()
    }
}

fun main(args: Array<String>) {
    (LogFactory.getLog(PDSimpleFont::class.java) as? Jdk14Logger)?.apply {
        logger.level = Level.SEVERE
    }

    // Suppress JavaFX "Unsupported JavaFX configuration" warning that appears when
    // JavaFX classes are loaded from the classpath (unnamed module) rather than the
    // module path. This is expected when running from a shaded/fat JAR.
    java.util.logging.Logger.getLogger("javafx").level = Level.SEVERE

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
