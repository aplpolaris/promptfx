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
        val res0 = BasicTemplateJoiner("snippet-joiners/basic").constructContext(testMatches)
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
        val res0 = GroupingTemplateJoiner("snippet-joiners/basic").constructContext(testMatches)
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
        val res0 = BulletedTemplateJoiner("snippet-joiners/basic").constructContext(testMatches)
        assertEquals("""
            Document: doc1

            Relevant Text: - (A1) abc
             - (A2) def

            Document: doc2

            Relevant Text: - (B1) ghi


        """.trimIndent(), res0)
    }

}
