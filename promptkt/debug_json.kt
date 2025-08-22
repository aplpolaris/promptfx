import tri.ai.core.JsonResponseProcessor

fun main() {
    val text = """
        Here's the data:
        ```
        [{"id": 1}, {"id": 2}]
        ```
    """.trimIndent()
    
    println("Text:")
    println(text)
    println("\nResult:")
    val result = JsonResponseProcessor.extractFirstValidJson(text)
    println(result)
    
    val text2 = """
        First: {"a": 1}
        Second: [1, 2, 3]
        Third: {"b": 2}
    """.trimIndent()
    
    println("\nText2:")
    println(text2)
    println("\nResults:")
    val results = JsonResponseProcessor.extractAllValidJson(text2)
    println("Count: ${results.size}")
    results.forEachIndexed { i, r -> println("$i: $r") }
}
