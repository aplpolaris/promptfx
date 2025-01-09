package tri.ai.text.docs

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import tri.ai.embedding.EmbeddingMatch
import tri.ai.embedding.SemanticTextQuery
import tri.ai.text.chunks.*

class SnippetJoinerTest {

    private val testQuery = SemanticTextQuery("test", listOf(0.0), "embedding-model")
    private val testMatches = listOf(
        EmbeddingMatch(testQuery, TextDoc("doc1"), TextChunkRaw("abc"), "embedding-model", listOf(0.0), 0.5f),
        EmbeddingMatch(testQuery, TextDoc("doc1"), TextChunkRaw("def"), "embedding-model", listOf(0.0), 0.5f),
        EmbeddingMatch(testQuery, TextDoc("doc2"), TextChunkRaw("ghi"), "embedding-model", listOf(0.0), 0.5f)
    )

    @Test
    fun testBasicTemplateJoiner() {
        val res0 = BasicTemplateJoiner("snippet-joiner-basic").constructContext(testMatches)
        assertEquals("""
            Document: doc1

            Relevant Text: abc

            Document: doc1

            Relevant Text: def

            Document: doc2

            Relevant Text: ghi


        """.trimIndent(), res0)
    }

    @Test
    fun testGroupingTemplateJoiner() {
        val res0 = GroupingTemplateJoiner("snippet-joiner-basic").constructContext(testMatches)
        assertEquals("""
            Document: doc1

            Relevant Text: abc
            ...
            def

            Document: doc2

            Relevant Text: ghi


        """.trimIndent(), res0)
    }

    @Test
    fun testBulletedTemplateJoiner() {
        val res0 = BulletedTemplateJoiner("snippet-joiner-basic").constructContext(testMatches)
        assertEquals("""
            Document: doc1

            Relevant Text: - (A1) abc
             - (A2) def

            Document: doc2

            Relevant Text: - (B1) ghi


        """.trimIndent(), res0)
    }

}