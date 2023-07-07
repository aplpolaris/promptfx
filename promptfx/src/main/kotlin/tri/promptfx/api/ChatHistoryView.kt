package tri.promptfx

import com.aallam.openai.api.BetaOpenAI
import com.aallam.openai.api.chat.ChatMessage
import com.aallam.openai.api.chat.ChatRole
import de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon
import de.jensd.fx.glyphs.fontawesome.FontAwesomeIconView
import javafx.beans.property.SimpleObjectProperty
import javafx.beans.property.SimpleStringProperty
import javafx.scene.layout.Priority
import tornadofx.*

@OptIn(BetaOpenAI::class)
class ChatHistoryView : Fragment() {

    val components = observableListOf<ChatLineModel>().apply {
        add(ChatLineModel(ChatRole.User, ""))
    }

    override val root = vbox {
        spacing = 10.0
        listview(components) {
            vgrow = Priority.ALWAYS
            cellFormat {
                graphic = hbox {
                    spacing = 10.0
                    combobox(it.roleProperty, listOf(ChatRole.User, ChatRole.Assistant, ChatRole.System)) {
                        cellFormat { text = it.role }
                    }
                    textfield(it.textProperty) {
                        hgrow = Priority.ALWAYS
                    }
                    button("", FontAwesomeIconView(FontAwesomeIcon.MINUS_CIRCLE)) {
                        action { components.remove(it) }
                    }
                }
            }
        }
        hbox {
            spacing = 10.0
            button("Add message", FontAwesomeIconView(FontAwesomeIcon.PLUS_CIRCLE)) {
                action {
                    components.add(ChatLineModel())
                }
            }
        }
    }

    fun chatMessages() = components.map {
        ChatMessage(it.role, it.text)
    }

}

@OptIn(BetaOpenAI::class)
class ChatLineModel(role: ChatRole = ChatRole.User, text: String = "") : ViewModel() {
    val roleProperty = SimpleObjectProperty(role)
    val role by roleProperty

    val textProperty = SimpleStringProperty(text)
    var text by textProperty
}