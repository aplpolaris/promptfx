package tri.promptfx.ui

import javafx.beans.property.SimpleIntegerProperty
import javafx.stage.Window
import tornadofx.*
import tri.ai.prompt.trace.AiPromptTraceSupport
import tri.ai.text.docs.FormattedPromptTraceResult
import tri.ai.text.docs.FormattedText
import tri.ai.text.docs.toHtml

/** Implementation of a prompt result area with support for displaying a list of results. */
class PromptResultAreaModel {

    /** The trace being represented in the result area. */
    val traces = observableListOf<AiPromptTraceSupport<*>>()

    /** Whether there are multiple traces to display. */
    val multiTrace = traces.sizeProperty.greaterThan(1)
    /** Index of the currently selected trace. */
    val traceIndex = SimpleIntegerProperty()

    /** The trace currently being displayed. */
    val trace = traceIndex.objectBinding(traces) {
        if (it != null && it.toInt() in traces.indices) {
            traces[it.toInt()]
        } else {
            null
        }
    }
    init {
        trace.onChange { updateResults() }
    }

    /** List of results to display in the result area. */
    val results = observableListOf<String>()
    /** List of results, as [FormattedText], if present. */
    val resultsFormatted = observableListOf<FormattedText>()

    /** Whether there are multiple results to display. */
    val multiResult = results.sizeProperty.greaterThan(1)
    /** Index of the currently selected result. */
    val resultIndex = SimpleIntegerProperty()

    /** String representation of the currently selected result. */
    val resultText = resultIndex.stringBinding(results) {
        results.getOrNull(it?.toInt() ?: 0)
    }
    /** Formatted representation of the currently selected result. */
    val resultTextFormatted = resultIndex.objectBinding(resultsFormatted) {
        resultsFormatted.getOrNull(it?.toInt() ?: 0)
    }
    /** HTML representation of the currently selected result. */
    val resultTextHtml
        get() = resultTextFormatted.value?.toHtml() ?: "<html>(No result)"

    //region ADDITIONAL RESULT CONTENT DETECTION

    /** Flag indicating if the selected result contains code. */
    val containsCode = resultText.booleanBinding { it != null && it.lines().count { it.startsWith("```") } >= 2 }
    /** Flag indicating if the selected result contains PlantUML or MindMap code. */
    val containsPlantUml = resultText.booleanBinding { it != null && (
            it.contains("@startuml") && it.contains("@enduml") ||
                    it.contains("@startmindmap") && it.contains("@endmindmap")
            )
    }
    /** PlantUML content from the selected result, if present. */
    val plantUmlText
        get() = if ("@startuml" in resultText.value) {
            "@startuml\n" + resultText.value.substringAfter("@startuml").substringBefore("@enduml").trim() + "\n@enduml"
        } else if ("@startmindmap" in resultText.value) {
            "@startmindmap\n" + resultText.value.substringAfter("@startmindmap").substringBefore("@endmindmap").trim() + "\n@endmindmap"
        } else {
            throw IllegalStateException("No PlantUML or MindMap code found in the selected result.")
        }

    //endregion

    /** Update the result objects, when the trace changes. */
    private fun updateResults() {
        val single = traces == listOf(trace.value)
        if (traces.isEmpty()) {
            results.setAll("(no result)")
            resultsFormatted.setAll(listOf())
            resultIndex.set(0)
        } else if (single && trace.value?.exec?.error != null) {
            results.setAll("(error)")
            resultsFormatted.setAll(listOf())
            resultIndex.set(0)
        } else {
            val results = trace.value!!.values?.map { it?.toString() ?: "(no result)" } ?: listOf("(no result)")
            this.results.setAll(results)
            this.resultsFormatted.setAll((trace.value as? FormattedPromptTraceResult)?.formattedOutputs ?: listOf())
            resultIndex.set(0)
        }
    }

    /** Set the final result to display in the result area. */
    fun setFinalResult(finalResult: AiPromptTraceSupport<*>, currentWindow: Window?) {
        traces.setAll(finalResult)
        traceIndex.set(0)

        if (finalResult.exec.error != null) {
            error(
                owner = currentWindow,
                header = "Error during Execution",
                content = "Error: ${finalResult.exec.error}"
            )
        }
    }

    /** Set the final result to display in the result area, as a list of results. */
    fun setFinalResultList(resultList: List<AiPromptTraceSupport<*>>, currentWindow: Window?) {
        traces.setAll(resultList)
        traceIndex.set(0)
    }

}