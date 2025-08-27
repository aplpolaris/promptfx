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

import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import tri.ai.text.docs.DocumentQaDriver
import tri.ai.text.docs.LocalDocumentQaDriver
import java.io.File

/** REST API server for Document Q&A functionality. */
class DocumentQaRestServer(
    private val rootPath: File,
    private val port: Int = 8080,
    private val host: String = "0.0.0.0"
) {
    
    private lateinit var driver: DocumentQaDriver
    private lateinit var server: NettyApplicationEngine
    
    fun start(wait: Boolean = true) {
        driver = LocalDocumentQaDriver(rootPath)
        driver.initialize()
        
        server = embeddedServer(Netty, port = port, host = host) {
            install(ContentNegotiation) {
                json(Json {
                    prettyPrint = true
                    isLenient = true
                })
            }
            
            routing {
                get("/") {
                    call.respond(mapOf(
                        "service" to "PromptFx Document Q&A API",
                        "version" to "1.0.0",
                        "endpoints" to listOf("/folders", "/qa")
                    ))
                }
                
                get("/folders") {
                    try {
                        val response = FoldersResponse(
                            folders = driver.folders,
                            currentFolder = driver.folder
                        )
                        call.respond(HttpStatusCode.OK, response)
                    } catch (e: Exception) {
                        call.respond(
                            HttpStatusCode.InternalServerError,
                            ErrorResponse("Failed to retrieve folders", e.message)
                        )
                    }
                }
                
                post("/qa") {
                    try {
                        val request = call.receive<DocumentQaRequest>()
                        
                        // Validate the request
                        if (request.question.isBlank()) {
                            call.respond(
                                HttpStatusCode.BadRequest,
                                ErrorResponse("Question cannot be empty")
                            )
                            return@post
                        }
                        
                        // Set the folder if provided
                        if (request.folder.isNotEmpty()) {
                            if (request.folder !in driver.folders) {
                                call.respond(
                                    HttpStatusCode.BadRequest,
                                    ErrorResponse("Invalid folder: ${request.folder}. Available folders: ${driver.folders}")
                                )
                                return@post
                            }
                            driver.folder = request.folder
                        }
                        
                        // Process the question
                        val result = driver.answerQuestion(
                            request.question,
                            request.numResponses,
                            request.historySize
                        )
                        
                        val response = DocumentQaResponse(
                            answer = result.finalResult.toString(),
                            question = request.question,
                            folder = driver.folder,
                            success = true
                        )
                        
                        call.respond(HttpStatusCode.OK, response)
                        
                    } catch (e: Exception) {
                        call.respond(
                            HttpStatusCode.InternalServerError,
                            ErrorResponse("Failed to process question", e.message)
                        )
                    }
                }
            }
        }
        
        server.start(wait = wait)
        println("Document Q&A REST API server started on http://$host:$port")
        println("Root document path: ${rootPath.absolutePath}")
        println("Available folders: ${driver.folders}")
    }
    
    fun stop() {
        if (::server.isInitialized) {
            server.stop(1000, 2000)
        }
        if (::driver.isInitialized) {
            driver.close()
        }
    }
}