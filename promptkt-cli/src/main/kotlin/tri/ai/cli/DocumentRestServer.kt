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
package tri.ai.cli

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.requireObject
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.validate
import com.github.ajalt.clikt.parameters.types.int
import tri.ai.rest.DocumentQaRestServer

/** Command to start the Document Q&A REST API server. */
class DocumentRestServer : CliktCommand(name = "server", help = "Start REST API server for document Q&A") {
    private val config by requireObject<DocumentQaConfig>()
    private val port by option(help = "Port to run the server on (default 8080)")
        .int()
        .default(8080)
        .validate {
            require(it in 1..65535) { "Port must be between 1 and 65535" }
        }
    private val host by option(help = "Host address to bind to (default 0.0.0.0)")
        .default("0.0.0.0")

    override fun run() {
        println("Starting Document Q&A REST API server...")
        println("Configuration:")
        println("  Root path: ${config.root}")
        println("  Default folder: ${config.folder}")
        println("  Chat model: ${config.chatModel}")
        println("  Embedding model: ${config.embeddingModel}")
        println("  Temperature: ${config.temp}")
        println("  Max tokens: ${config.maxTokens}")
        
        val server = DocumentQaRestServer(
            rootPath = config.root.toFile(),
            port = port,
            host = host
        )
        
        // Add shutdown hook
        Runtime.getRuntime().addShutdownHook(Thread {
            println("Shutting down server...")
            server.stop()
        })
        
        try {
            server.start(wait = true)
        } catch (e: Exception) {
            println("Failed to start server: ${e.message}")
            throw e
        }
    }
}