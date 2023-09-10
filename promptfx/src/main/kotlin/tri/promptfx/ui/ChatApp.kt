package tri.promptfx.ui

import tornadofx.*

/** Basic application showing a chat window. */
class ChatApp : App(ChatAppView::class) {

    override fun start(stage: javafx.stage.Stage) {
        super.start(stage)
        stage.width = 500.0
        stage.height = 800.0
    }

}

class ChatAppView : View() {
    init {
        setInScope(OpenAiChatDriver(), scope, ChatDriver::class)
    }
    override val root = find<ChatView>().root
}

fun main(args: Array<String>) {
    launch<ChatApp>(args)
}