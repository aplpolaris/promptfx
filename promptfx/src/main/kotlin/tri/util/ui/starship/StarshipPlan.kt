package tri.util.ui.starship

import com.fasterxml.jackson.databind.JsonNode
import kotlinx.coroutines.runBlocking
import tri.ai.core.agent.createObject
import tri.ai.core.tool.ExecContext
import tri.ai.core.tool.Executable
import tri.ai.core.tool.ExecutableRegistry
import tri.ai.core.tool.impl.PromptLibraryExecutableRegistry
import tri.ai.pips.api.PPlan
import tri.ai.pips.api.PPlanExecutor
import tri.ai.prompt.PromptLibrary

/** Parses a [PPlan] from JSON and uses result to drive the Starship visualization. */
class StarshipPlan {
    fun loadPlanYaml(yaml: String): PPlan {
        val plan = PPlan.parseYaml(yaml)
        return plan
    }
}

val SAMPLE_PLAN_YAML = """
id: starship/demo@0.0.1
steps:

  - tool: starship/random-question
    input: {}
    saveAs: question
    
  - tool: starship/execute-view
    input:
      input: { "${"$"}var": "question" }
    saveAs: viewResult
    
  - tool: prompt/text-summarize/simplify-audience
    input:
      input: { "${"$"}var": "viewResult" }
      audience: (UI option)
    saveAs: simpleResponse
    
  - tool: prompt/docs-reduce/outline
    input:
      input: { "${"$"}var": "viewResult" }
    saveAs: outline
    
  - tool: prompt/docs-reduce/technical-terms
    input:
      input: { "${"$"}var": "viewResult" }
    saveAs: technicalTerms
    
  - tool: prompt/text-translate/translate
    input:
      input: { "${"$"}var": "viewResult" }
      instruct: (UI option, random language)
    saveAs: translatedResponse
"""

/** Generates a random question for use in the Starship view.*/
class StarshipQuestionGenerator : Executable {
    override val name = "starship/random-question"
    override val description = "Generates a random question."
    override val version = "0.0.1"
    override val inputSchema: JsonNode? = null
    override val outputSchema: JsonNode? = null

    override suspend fun execute(input: JsonNode, context: ExecContext) =
        createObject("question", "What is the meaning of life?")
}

/** Executes the current view and returns the result as text. */
class StarshipCurrentViewExecutor : Executable {
    override val name = "starship/execute-view"
    override val description = "Executes the view that was active when Starship was launched and returns the result."
    override val version = "0.0.1"
    override val inputSchema: JsonNode? = null
    override val outputSchema: JsonNode? = null

    override suspend fun execute(input: JsonNode, context: ExecContext) =
        createObject("viewResult", "This is a simulated result of executing the Starship view.")
}

object StarshipPlanTest {
    @JvmStatic
    fun main(args: Array<String>) {
        val plan = StarshipPlan().loadPlanYaml(SAMPLE_PLAN_YAML)
        println("Loaded plan: $plan")

        val registry = ExecutableRegistry.create(
            listOf(StarshipQuestionGenerator(), StarshipCurrentViewExecutor()) +
            PromptLibraryExecutableRegistry(PromptLibrary.INSTANCE).list()
        )

        // TODO - the default executables from prompts just fill in text, don't actually execute the LLM calls --> create a secondary executor registry where it's assumed they will execute

        runBlocking {
            PPlanExecutor(registry).execute(plan, ExecContext())
        }
    }
}