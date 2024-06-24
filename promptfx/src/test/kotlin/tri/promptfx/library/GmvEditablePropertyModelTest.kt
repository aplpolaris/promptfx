package tri.promptfx.library

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import tri.ai.text.chunks.process.GuessedMetadataObject
import tri.ai.text.chunks.process.PdfMetadataGuesser.resolveConflicts
import java.time.LocalDate

class GmvEditablePropertyModelTest {

    @Test
    fun testCreate1() {
        val model = GmvEditablePropertyModel("Test", "Initial Value", listOf())
        assertFalse(model.isSaved.value)
        assertEquals(0, model.getAlternateValueList().size)
        assertEquals(-1, model.editingIndex.value)
        assertFalse(model.supportsValueCycling.value)

        model.saveValue()
        assertTrue(model.isSaved.value)
        assertEquals(-1, model.editingIndex.value)
        assertFalse(model.supportsValueCycling.value)
    }

    @Test
    fun testCreate2() {
        val model = GmvEditablePropertyModel("Test", "A", listOf("A" to "A"))
        assertFalse(model.isSaved.value)
        assertEquals(1, model.getAlternateValueList().size)
        assertEquals(0, model.editingIndex.value)
        assertFalse(model.supportsValueCycling.value)

        model.saveValue()
        assertTrue(model.isSaved.value)
        assertEquals(-1, model.editingIndex.value)
        assertFalse(model.supportsValueCycling.value)
    }

    @Test
    fun testAddAlternateValues() {
        val model = GmvEditablePropertyModel("Test", "Initial Value", listOf())
        assertFalse(model.supportsValueCycling.value)
        model.addAlternateValues(listOf("A" to "A", "B" to "B"), false)
        assertFalse(model.isSaved.value)
        assertEquals(2, model.getAlternateValueList().size)
        assertTrue(model.supportsValueCycling.value)
        assertEquals(-1, model.editingIndex.value)
    }

    @Test
    fun testPreviousValue() {
        val model = GmvEditablePropertyModel("Test", "Initial Value", listOf("A" to "A", "B" to "B"))
        assertFalse(model.isSaved.value)
        assertTrue(model.supportsValueCycling.value)
        assertEquals(2, model.getAlternateValueList().size)
        assertEquals(-1, model.editingIndex.value)

        model.previousValue()
        assertFalse(model.isSaved.value)
        assertEquals(1, model.editingIndex.value)
        assertEquals("B", model.editingValue.value)

        model.previousValue()
        assertFalse(model.isSaved.value)
        assertEquals(0, model.editingIndex.value)
        assertEquals("A", model.editingValue.value)

        model.previousValue()
        assertFalse(model.isSaved.value)
        assertEquals(1, model.editingIndex.value)
        assertEquals("B", model.editingValue.value)

        model.saveValue()
        assertEquals(-1, model.editingIndex.value)
        model.previousValue()
        assertEquals(1, model.editingIndex.value)
        model.previousValue()
        assertEquals(0, model.editingIndex.value)
        model.previousValue()
        assertEquals(-1, model.editingIndex.value)
    }

    @Test
    fun testNextValue() {
        val model = GmvEditablePropertyModel("Test", "Initial Value", listOf("A" to "A", "B" to "B"))
        assertFalse(model.isSaved.value)
        assertTrue(model.supportsValueCycling.value)
        assertEquals(2, model.getAlternateValueList().size)
        assertEquals(-1, model.editingIndex.value)

        model.nextValue()
        assertFalse(model.isSaved.value)
        assertEquals(0, model.editingIndex.value)
        assertEquals("A", model.editingValue.value)

        model.nextValue()
        assertFalse(model.isSaved.value)
        assertEquals(1, model.editingIndex.value)
        assertEquals("B", model.editingValue.value)

        model.nextValue()
        assertFalse(model.isSaved.value)
        assertEquals(0, model.editingIndex.value)
        assertEquals("A", model.editingValue.value)

        model.saveValue()
        assertEquals(-1, model.editingIndex.value)
        model.nextValue()
        assertEquals(0, model.editingIndex.value)
        model.nextValue()
        assertEquals(1, model.editingIndex.value)
        model.nextValue()
        assertEquals(-1, model.editingIndex.value)
    }

    @Test
    fun testCreateFromMultipleGuessedMetadataObjects() {
        val gmo1 = GuessedMetadataObject("A", "title", "subtitle", listOf("author 1", "author 2"), "date", listOf("keyword"), "abstract", null, listOf("section"), listOf("caption"), listOf("reference"), mapOf("other" to "value"))
        val gmo2 = GuessedMetadataObject("B", "title B", "subtitle B", listOf("author B"), "date B", listOf("keyword B"), null, "executiveSummary B", listOf("section B"), listOf("caption B"), listOf("reference B"), mapOf("other" to "value B"))
        val merged = listOf(gmo1, gmo2).resolveConflicts()
        assertEquals("title", merged.combined.title)
        assertEquals("abstract", merged.combined.abstract)
        assertEquals("executiveSummary B", merged.combined.executiveSummary)
        assertEquals(listOf("author 1", "author 2"), merged.combined.authors)
        assertEquals(listOf("section", "section B"), merged.combined.sections)

        val gpl = merged.asGmvPropList(markSaved = false)
        assertEquals(11, gpl.size)
        assertEquals("title", gpl[0].editingValue.value)
        assertEquals(0, gpl[0].editingIndex.value)
        assertEquals(3, gpl[0].getAlternateValueList().size)
        assertEquals(listOf("title" to "Combined", "title" to "A", "title B" to "B"), gpl[0].getAlternateValueList())
        assertFalse(gpl[0].isSaved.value)
        assertTrue(gpl[0].supportsValueCycling.value)
    }

    @Test
    fun testMaybeParseDate() {
        assertEquals(LocalDate.of(2024, 1, 2), "2024-01-02".maybeParseDate())
        assertEquals(LocalDate.of(2024, 1, 2), "2024-01-02T03:04:05".maybeParseDate())
        assertEquals(LocalDate.of(2024, 1, 2), "2024-01-02T03:04:05.678".maybeParseDate())
        assertEquals(LocalDate.of(2024, 1, 2), listOf(2024, 1, 2).maybeParseDate())
        assertEquals(LocalDate.of(2024, 1, 2), listOf(2024, 1, 2, 3, 4, 5).maybeParseDate())
        assertEquals(LocalDate.of(2024, 1, 2), listOf(2024, 1, 2, 3, 4, 5, 6).maybeParseDate())
    }
}