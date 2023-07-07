package tri.promptfx.apps

import com.aallam.openai.api.logging.LogLevel
import javafx.application.Platform
import kotlinx.coroutines.runBlocking
import tri.ai.openai.OpenAiSettings
import tri.ai.pips.AiPipelineExecutor
import tri.ai.pips.IgnoreMonitor

/** Standalone app for asking questions of documents. */
object DocumentQaRunner {
    @JvmStatic
    fun main(args: Array<String>) {
        OpenAiSettings.logLevel = LogLevel.None

        runBlocking {
            // initialize toolkit
            Platform.startup { }

            // chat with the user until they say "bye"
            val view = DocumentQaView()

            println("You can ask questions about the documents in ${view.documentFolder.get()}. Say 'bye' to exit.")
            print("> ")
            var input = readln()
            while (input != "bye") {
                view.question.set(input)
                val result = AiPipelineExecutor.execute(view.plan().plan(), IgnoreMonitor())
                println(result.finalResult)
                print("> ")
                input = readln()
            }
            println("Goodbye!")

            Platform.exit()
        }
    }
}