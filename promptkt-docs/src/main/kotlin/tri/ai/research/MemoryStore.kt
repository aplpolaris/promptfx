package tri.ai.research

import com.fasterxml.jackson.databind.JsonNode

/** A basic memory store interface. */
interface MemoryStore {
    /** Get memory keys. */
    fun keys(): List<String>
    /** Store a memory item. */
    fun store(item: MemoryItem)
    /** Retrieve a memory item by key. */
    fun retrieve(key: String): MemoryItem?
}

/** A single memory object. */
data class MemoryItem(val key: String, val value: JsonNode)

/** A simple in-memory implementation of MemoryStore. */
class InMemoryStore : MemoryStore {
    private val memory = mutableMapOf<String, JsonNode>()
    override fun keys() = memory.keys.toList()
    override fun store(item: MemoryItem) { memory[item.key] = item.value }
    override fun retrieve(key: String) = memory[key]?.let { MemoryItem(key, it) }
}