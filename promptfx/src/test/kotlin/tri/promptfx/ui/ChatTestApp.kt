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

import tornadofx.*

/** Basic application showing a chat window. */
class ChatTestApp : App(ChatTestAppView::class) {

    override fun start(stage: javafx.stage.Stage) {
        super.start(stage)
        stage.width = 500.0
        stage.height = 800.0
    }

    companion object {
        @JvmStatic
        fun main(args: Array<String>) {
            launch<ChatTestApp>(args)
        }
    }
}

class ChatTestAppView : View() {
    private val driver = OpenAiChatDriver().apply {
        userName = "Joe"
        systemName = "GPT"
        chatHistorySize = 5
    }

    init {
        setInScope(driver, scope, ChatDriver::class)
    }

    override val root = vbox {
        val chatPanel = find<ChatFragment>()
        button {
            button("click to test chat") {
                action {
                    chatPanel.addChat(ChatEntry(user = "test", message = "this is a test", ChatEntryRole.SYSTEM))
                }
            }
        }
        add(chatPanel)
    }
}
