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

import com.fasterxml.jackson.databind.ObjectMapper
import de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon
import de.jensd.fx.glyphs.fontawesome.FontAwesomeIconView
import javafx.collections.ObservableList
import javafx.geometry.Pos
import javafx.scene.layout.Priority
import javafx.stage.FileChooser
import kotlinx.coroutines.runBlocking
import tornadofx.*
import tri.ai.prompt.trace.AiPromptTrace
import tri.promptfx.PromptFxWorkspace

/** UI for a list of [AiPromptTrace]s. */
class PromptTraceCardList(val prompts: ObservableList<AiPromptTrace> = observableListOf()): Fragment() {

    override val root = vbox {
        spacing = 5.0
        paddingAll = 5.0
        vgrow = Priority.ALWAYS
        val header = hbox {
            alignment = Pos.CENTER_LEFT
            spacing = 5.0
            text("Results:")
            spacer()
        }
        val list = listview(prompts) {
            cellFormat {
                graphic = PromptTraceCard().apply { setTrace(it) }.root
            }
        }
        with (header) {
            button("", FontAwesomeIconView(FontAwesomeIcon.SEND)) {
                enableWhen(list.selectionModel.selectedItemProperty().isNotNull)
                action {
                    val selected = list.selectedItem
                    if (selected != null)
                        (workspace as PromptFxWorkspace).launchTemplateView(selected.promptInfo.prompt)
                }
            }
            // add save icon
            button("", FontAwesomeIconView(FontAwesomeIcon.DOWNLOAD)) {
                enableWhen(prompts.sizeProperty.greaterThan(0))
                action {
                    val promptTraces = prompts.toList()
                    val file = chooseFile("Export Prompt Traces as JSON", arrayOf(FileChooser.ExtensionFilter("JSON", "*.json")), mode = FileChooserMode.Save, owner = currentWindow)
                    if (file.isNotEmpty()) {
                        runAsync {
                            runBlocking {
                                ObjectMapper()
                                    .writerWithDefaultPrettyPrinter()
                                    .writeValue(file.first(), promptTraces)
                            }
                        }
                    }
                }
            }
        }
    }

}
