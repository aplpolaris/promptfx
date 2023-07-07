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