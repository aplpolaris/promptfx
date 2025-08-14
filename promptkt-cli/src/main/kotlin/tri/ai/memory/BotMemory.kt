/*-
 * #%L
 * tri.promptfx:promptkt
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
package tri.ai.memory

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.KotlinModule
import com.fasterxml.jackson.module.kotlin.readValue
import tri.ai.core.EmbeddingModel
import tri.ai.core.TextChat
import tri.ai.core.TextChatMessage
import tri.ai.core.MChatRole
import tri.ai.embedding.dot
import java.io.File

/**
 * A memory of previous conversations. Uses a chat engine to summarize memories of previous conversations,
 * which are then periodically ingested into the chat engine's memory.
 */
class BotMemory(val persona: BotPersona, val chatEngine: TextChat, val embeddingModel: EmbeddingModel) : MemoryService {

    val memoryHistoryLimit = 5
    val historyLimit = 20

    /** Number of steps between interim saves. */
    val stepsToSaveMemory = 20

    val memoryFile = File("memory.json")
    val chatHistory = mutableListOf<MemoryItem>()

    //region API IMPLEMENTATION

    override fun initMemory() {
        if (!memoryFile.exists()) {
            memoryFile.writeText("[]")
        }
        val memory = ObjectMapper()
            .registerModule(KotlinModule.Builder().build())
            .readValue<List<MemoryItem>>(memoryFile)
        chatHistory.addAll(memory)
    }

    override suspend fun saveMemory(interimSave: Boolean) {
        if (!interimSave || stepsSinceLastMemory() >= stepsToSaveMemory) {
            println("\u001B[90mSaving memory...\u001B[0m")
            generateMemories()
            val memories = chatHistory.map { it.withEmbedding() }
            ObjectMapper()
                .registerModule(KotlinModule.Builder().build())
                .writerWithDefaultPrettyPrinter()
                .writeValue(memoryFile, memories)
        }
    }

    override suspend fun addChat(chatMessage: MemoryItem) {
        chatHistory += chatMessage.withEmbedding()
    }

    override fun buildContextualConversationHistory(userInput: MemoryItem): List<MemoryItem> {
        // use embedding index with recent chat for relevant messages
        val historyForMemorySearch = chatHistory.takeLast(2)
        val avgHistoryEmbedding = historyForMemorySearch.map { it.embedding }
            .mapIndexed { i, floats -> floats.map { it * (i + 1) } }
            .reduce { acc, floats -> acc.zip(floats).map { it.first + it.second } }
        val relevant = chatHistory.map { it to it.embedding.dot(avgHistoryEmbedding) }
            .sortedByDescending { it.second }
            .take(memoryHistoryLimit)
            .filter { (it.first.content?: "").length > 50 }
            .map { it.first }
        // gather more recent memories
        val memories = chatHistory.filter { it.isMemory() }.takeLast(memoryHistoryLimit).toSet()
        // gather more recent chat messages
        val recentChat = chatHistory.takeLast(historyLimit).toSet()
        return (relevant - memories - recentChat) + (memories - recentChat) + recentChat
    }

    //endregion

    private suspend fun MemoryItem.withEmbedding(): MemoryItem {
        return if (embedding.isEmpty())
            MemoryItem(role, content, embeddingModel.calculateEmbedding(content ?: "").map { String.format("%.4f", it).toFloat() })
        else
            this
    }

    private fun stepsSinceLastMemory() = chatHistory.size - chatHistory.indexOfLast { it.isMemory() }

    private suspend fun generateMemories() {
        // collect chat since last memory
        val lastMemory = chatHistory.indexOfLast { it.isMemory() }
        val chatSinceLastMemory = chatHistory.subList(lastMemory + 1, chatHistory.size)

        // summarize content for memory
        val conversation = chatSinceLastMemory.joinToString("\n") {
            (if (it.role == MChatRole.Assistant) persona.name else it.role.toString()) + ": " + it.content
        }
        val query = """
            Please summarize the following conversation:
            '''
            $conversation
            '''
            Include any notable topics discussed, specific facts, and in particular things you learned about the user.
        """.trimIndent()

        val response = chatEngine.chat(
            listOf(
                TextChatMessage(MChatRole.System, "You are a chatbot that summarizes key content from prior conversations."),
                TextChatMessage(MChatRole.User, query)
            ))
        val summaryMessage = TextChatMessage(MChatRole.Assistant, "[MEMORY] " + (response.firstValue.content ?: "").trim())
        chatHistory.add(MemoryItem(summaryMessage))
    }

    private fun MemoryItem.isMemory() =
        role == MChatRole.Assistant && (content ?: "").startsWith("[MEMORY]")

}
