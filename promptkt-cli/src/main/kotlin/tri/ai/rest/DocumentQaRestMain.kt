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
package tri.ai.rest

import java.io.File

/**
 * Standalone main class for running the Document Q&A REST API server.
 * 
 * Usage: java -cp target/promptkt-cli-*-jar-with-dependencies.jar tri.ai.rest.DocumentQaRestMain [rootPath] [port]
 * 
 * Arguments:
 * - rootPath: Path to directory containing document folders (default: current directory)
 * - port: Port to run server on (default: 8080)
 */
object DocumentQaRestMain {
    @JvmStatic
    fun main(args: Array<String>) {
        val rootPath = if (args.isNotEmpty()) File(args[0]) else File(".")
        val port = if (args.size > 1) args[1].toIntOrNull() ?: 8080 else 8080
        
        if (!rootPath.exists()) {
            println("Error: Root path '${rootPath.absolutePath}' does not exist")
            return
        }
        
        if (!rootPath.isDirectory) {
            println("Error: Root path '${rootPath.absolutePath}' is not a directory")
            return
        }
        
        println("Starting Document Q&A REST API server...")
        println("Root path: ${rootPath.absolutePath}")
        println("Port: $port")
        
        val server = DocumentQaRestServer(rootPath, port)
        
        // Add shutdown hook
        Runtime.getRuntime().addShutdownHook(Thread {
            println("Shutting down server...")
            server.stop()
        })
        
        try {
            server.start(wait = true)
        } catch (e: Exception) {
            println("Failed to start server: ${e.message}")
            e.printStackTrace()
        }
    }
}