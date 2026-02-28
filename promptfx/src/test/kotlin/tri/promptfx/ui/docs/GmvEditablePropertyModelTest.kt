/*-
 * #%L
 * tri.promptfx:promptfx
 * %%
 * Copyright (C) 2023 - 2026 Johns Hopkins University Applied Physics Laboratory
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
package tri.promptfx.ui.docs

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import tri.ai.process.pdf.GuessedMetadataObject
import tri.ai.process.pdf.PdfMetadataGuesser.resolveConflicts
import java.time.LocalDate

class GmvEditablePropertyModelTest {

    //region CREATE WITH AN ORIGINAL VALUE AND NO OPTIONS

    @Test
    fun testCreate1() {
        val model = GmvEditablePropertyModel("Test", "Initial Value", null, listOf())
        assertTrue(model.isOriginal.value)
        assertFalse(model.isCustom.value)
        assertFalse(model.isValueOption.value)
        assertFalse(model.isValueCyclable.value)
        assertTrue(model.isDeletable.value)
        assertFalse(model.isDeletePending.value)
        assertFalse(model.isUpdatePending.value)
        assertFalse(model.isAnyChangePending.value)
        assertEquals(0, model.editingIndex.value)
        assertEquals("Original", model.changeLabel.value)
    }

    @Test
    fun testCustomValue() {
        val model = GmvEditablePropertyModel("Test", "Initial Value", null, listOf())
        model.updateCustom("Custom user value")
        assertFalse(model.isOriginal.value)
        assertTrue(model.isCustom.value)
        assertFalse(model.isValueOption.value)
        assertTrue(model.isValueCyclable.value)
        assertTrue(model.isDeletable.value)
        assertFalse(model.isDeletePending.value)
        assertFalse(model.isUpdatePending.value)
        assertFalse(model.isAnyChangePending.value)
        assertEquals(1, model.editingIndex.value)
        assertEquals("Custom user value", model.editingValue.value)
        assertEquals("Changed - Custom", model.changeLabel.value)
    }

    @Test
    fun testRevert() {
        val model = GmvEditablePropertyModel("Test", "Initial Value", null, listOf())
        model.editingValue.set("Custom user value")
        model.revertChanges()
        assertTrue(model.isOriginal.value)
        assertFalse(model.isCustom.value)
        assertFalse(model.isValueOption.value)
        assertFalse(model.isValueCyclable.value)
        assertTrue(model.isDeletable.value)
        assertFalse(model.isDeletePending.value)
        assertFalse(model.isUpdatePending.value)
        assertFalse(model.isAnyChangePending.value)
        assertEquals(0, model.editingIndex.value)
        assertEquals("Original", model.changeLabel.value)
    }

    //endregion

    //region CREATE WITH AN ORIGINAL VALUE FROM AN OPTIONS LIST

    @Test
    fun testCreate2() {
        val model = GmvEditablePropertyModel("Test", "A", null, listOf("A" to "label A", "B" to "label B"))
        assertTrue(model.isOriginal.value)
        assertFalse(model.isCustom.value)
        assertTrue(model.isValueOption.value)
        assertTrue(model.isValueCyclable.value)
        assertTrue(model.isDeletable.value)
        assertFalse(model.isDeletePending.value)
        assertFalse(model.isUpdatePending.value)
        assertFalse(model.isAnyChangePending.value)
        assertEquals(0, model.editingIndex.value)
        assertEquals("Original", model.changeLabel.value)
    }

    @Test
    fun testCustomValue2() {
        val model = GmvEditablePropertyModel("Test", "A", null, listOf("A" to "label A", "B" to "label B"))
        model.updateCustom("Custom user value")
        assertFalse(model.isOriginal.value)
        assertTrue(model.isCustom.value)
        assertFalse(model.isValueOption.value)
        assertTrue(model.isValueCyclable.value)
        assertTrue(model.isDeletable.value)
        assertFalse(model.isDeletePending.value)
        assertFalse(model.isUpdatePending.value)
        assertFalse(model.isAnyChangePending.value)
        assertEquals(1, model.editingIndex.value)
        assertEquals("Changed - Custom", model.changeLabel.value)
    }

    @Test
    fun testRevert2() {
        val model = GmvEditablePropertyModel("Test", "A", null, listOf("A" to "label A", "B" to "label B"))
        model.editingValue.set("Custom user value")
        model.revertChanges()
        assertTrue(model.isOriginal.value)
        assertFalse(model.isCustom.value)
        assertTrue(model.isValueOption.value)
        assertTrue(model.isValueCyclable.value)
        assertTrue(model.isDeletable.value)
        assertFalse(model.isDeletePending.value)
        assertFalse(model.isUpdatePending.value)
        assertFalse(model.isAnyChangePending.value)
        assertEquals(0, model.editingIndex.value)
        assertEquals("Original", model.changeLabel.value)
    }

    //endregion

    //region CREATE WITH A MISSING ORIGINAL VALUE

    @Test
    fun testCreate3() {
        val model = GmvEditablePropertyModel("Test", null, "A", listOf("A" to "label A", "B" to "label B"))
        assertFalse(model.isOriginal.value)
        assertFalse(model.isCustom.value)
        assertTrue(model.isValueOption.value)
        assertTrue(model.isValueCyclable.value)
        assertFalse(model.isDeletable.value)
        assertFalse(model.isDeletePending.value)
        assertFalse(model.isUpdatePending.value)
        assertFalse(model.isAnyChangePending.value)
        assertEquals(0, model.editingIndex.value)
        assertEquals("New - label A", model.changeLabel.value)
    }

    @Test
    fun testRevert3() {
        val model = GmvEditablePropertyModel("Test", null, "A", listOf("A" to "label A", "B" to "label B"))
        model.editingValue.set("Custom user value")
        model.revertChanges()
        assertFalse(model.isOriginal.value)
        assertFalse(model.isCustom.value)
        assertTrue(model.isValueOption.value)
        assertTrue(model.isValueCyclable.value)
        assertFalse(model.isDeletable.value)
        assertFalse(model.isDeletePending.value)
        assertFalse(model.isUpdatePending.value)
        assertFalse(model.isAnyChangePending.value)
        assertEquals(0, model.editingIndex.value)
        assertEquals("New - label A", model.changeLabel.value)
    }

    //endregion

    //region MUTATOR TESTS

    @Test
    fun testAddAlternateValues() {
        val model = GmvEditablePropertyModel("Test", "Initial Value", null, listOf())
        with (model) {
            assertFalse(isValueCyclable.value)

            model.addAlternateValues(listOf("A" to "A", "B" to "B"), false)
            assertTrue(isOriginal.value)
            assertFalse(isCustom.value)
            assertFalse(isValueOption.value)
            assertTrue(isValueCyclable.value)
            assertTrue(isDeletable.value)
            assertFalse(isDeletePending.value)
            assertFalse(isUpdatePending.value)
            assertFalse(isAnyChangePending.value)
            assertEquals(0, editingIndex.value)
            assertEquals("Original", changeLabel.value)
        }
    }

    //endregion

    //region VALUE CYCLING TESTS

    @Test
    fun testPreviousValue() {
        val model = GmvEditablePropertyModel("Test", "Initial Value", "A", listOf("A" to "label A", "B" to "label B"))
        assertFalse(model.isOriginal.value)
        assertFalse(model.isCustom.value)
        assertTrue(model.isValueOption.value)
        assertTrue(model.isValueCyclable.value)
        assertTrue(model.isDeletable.value)
        assertFalse(model.isDeletePending.value)
        assertFalse(model.isUpdatePending.value)
        assertFalse(model.isAnyChangePending.value)
        assertEquals(1, model.editingIndex.value)
        assertEquals("Changed - label A", model.changeLabel.value)

        model.previousValue()
        assertTrue(model.isOriginal.value)
        assertEquals(0, model.editingIndex.value)
        assertEquals("Original", model.changeLabel.value)

        model.previousValue()
        assertFalse(model.isOriginal.value)
        assertEquals(2, model.editingIndex.value)
        assertEquals("Changed - label B", model.changeLabel.value)

        model.previousValue()
        assertFalse(model.isOriginal.value)
        assertEquals(1, model.editingIndex.value)
        assertEquals("Changed - label A", model.changeLabel.value)
    }

    @Test
    fun testNextValue() {
        val model = GmvEditablePropertyModel("Test", "Initial Value", "Custom", listOf("A" to "label A", "B" to "label B"))
        assertFalse(model.isOriginal.value)
        assertTrue(model.isCustom.value)
        assertFalse(model.isValueOption.value)
        assertTrue(model.isValueCyclable.value)
        assertTrue(model.isDeletable.value)
        assertFalse(model.isDeletePending.value)
        assertFalse(model.isUpdatePending.value)
        assertFalse(model.isAnyChangePending.value)
        assertEquals(1, model.editingIndex.value)
        assertEquals("Changed - Custom", model.changeLabel.value)

        model.nextValue()
        assertFalse(model.isOriginal.value)
        assertEquals(2, model.editingIndex.value)
        assertEquals("Changed - label A", model.changeLabel.value)

        model.nextValue()
        assertFalse(model.isOriginal.value)
        assertEquals(3, model.editingIndex.value)
        assertEquals("Changed - label B", model.changeLabel.value)

        model.nextValue()
        assertTrue(model.isOriginal.value)
        assertEquals(0, model.editingIndex.value)
        assertEquals("Original", model.changeLabel.value)
    }

    //endregion

    @Test
    fun testMultiModel() {
        val model = MetadataValidatorModel()
        assertFalse(model.isChanged.value)

        val prop1 = GmvEditablePropertyModel<Any?>("Test", "Initial Value", "Custom", listOf("A" to "label A", "B" to "label B"))
        assertFalse(prop1.isAnyChangePending.value)

        model.merge(listOf(prop1), true)
        assertFalse(model.isChanged.value)

        prop1.isUpdatePending.set(true)
        assertTrue(model.isChanged.value)
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

        val gpl = merged.asGmvPropList(isOriginal = false)
        assertEquals(11, gpl.size)
        assertEquals("title", gpl[0].editingValue.value)
        assertEquals(0, gpl[0].editingIndex.value)
        assertEquals(3, gpl[0].getAlternateValueList().size)
        assertEquals(listOf("title" to "Combined", "title" to "A", "title B" to "B"), gpl[0].getAlternateValueList())
        assertFalse(gpl[0].isOriginal.value)
        assertTrue(gpl[0].isValueCyclable.value)
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
