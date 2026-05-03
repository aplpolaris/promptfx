/*-
 * #%L
 * tri.promptfx:promptrt
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
package tri.ai.cli

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.main
import com.github.ajalt.clikt.core.subcommands
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.types.file
import tri.ai.cli.config.ConfigLoader
import tri.ai.cli.repl.PromptRtRepl
import java.io.File

class PromptRt : CliktCommand(name = "promptrt") {
    override val invokeWithoutSubcommand = true

    companion object {
        @JvmStatic
        fun main(args: Array<String>) = PromptRt()
            .subcommands(PromptRtChatOnce(), PromptRtBatch(), PromptRtModels(), PromptRtShowConfig())
            .main(args)
    }

    private val configFile by option("--config", "-c", help = "Config file (default ~/.promptrt/config.yaml)")
        .file(mustExist = false)
        .default(File(System.getProperty("user.home"), ".promptrt/config.yaml"))

    private val mode by option("--mode", "-m", help = "Launch in a specific mode")

    override fun run() {
        if (currentContext.invokedSubcommand != null) return
        val config = ConfigLoader.load(configFile)
        val effectiveConfig = if (mode != null) config.copy(defaultMode = mode!!) else config
        PromptRtRepl(effectiveConfig).start()
    }
}

class PromptRtChatOnce : CliktCommand(name = "chat") {
    override fun run() = echo("[stub] single-turn chat not yet implemented")
}

class PromptRtBatch : CliktCommand(name = "batch") {
    override fun run() = echo("[stub] batch not yet implemented")
}

class PromptRtModels : CliktCommand(name = "models") {
    override fun run() = echo("[stub] models not yet implemented")
}

class PromptRtShowConfig : CliktCommand(name = "config") {
    override fun run() = echo("[stub] config display not yet implemented")
}
