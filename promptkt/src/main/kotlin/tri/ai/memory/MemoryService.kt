/*-
 * #%L
 * promptkt-0.1.0-SNAPSHOT
 * %%
 * Copyright (C) 2023 Johns Hopkins University Applied Physics Laboratory
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

/** Interface for a memory service. */
interface MemoryService {

    /** Initializes memory. */
    fun initMemory()

    /** Saves record of conversation in memory. */
    suspend fun saveMemory(interimSave: Boolean = true)

    /** Adds a chat to memory of current conversation. */
    suspend fun addChat(chatMessage: MemoryItem)

    /** Builds a history to use for the next query. */
    fun buildContextualConversationHistory(userInput: MemoryItem): List<MemoryItem>

}
