package tri.ai.tool.wf

import org.junit.jupiter.api.Test
import tri.ai.openai.OpenAiPlugin

class WorkflowExecutorTest {

    private val GPT35 = OpenAiPlugin().textCompletionModels()[0]

    //region CALC SOLVERS

    private val CALC_FUNCTION : (String) -> String = { "42" }
    private val ROMANIZER_FUNCTION : (String) -> String = {
        it.toInt().let {
            when (it) {
                42 -> "XLII"
                84 -> "LXXXIV"
                else -> "I don't know"
            }
        }
    }

    private val CALC_SOLVER = RunSolver(
        "Calculator",
        "Use this to do math",
        "Expression to evaluate",
        "Calculated result",
        CALC_FUNCTION
    )
    private val ROMANIZER_SOLVER = RunSolver(
        "Romanizer",
        "Converts numbers to Roman numerals",
        "Positive integer to convert",
        "Roman numeral result",
        ROMANIZER_FUNCTION
    )

    //endregion

    @Test
    fun `test WorkflowExecutor with a calculator`() {
        val executor = WExecutorChat(GPT35, maxTokens = 1000, temp = 0.3)
        val exec = WorkflowExecutor(executor, listOf(CALC_SOLVER, ROMANIZER_SOLVER))
        val problem = WorkflowUserRequest("I need a Roman numeral that represents the product 21 times 2.")
        exec.solve(problem)
    }

    //region QUERY/TIMELINE SOLVERS

    private val SOLVER_QUERY = ChatSolver(
        "Data Query",
        "Use this to search for data that is needed to answer a question",
        "The query to search for",
        "The result of the query",
        "tool-query"
    )

    private val SOLVER_TIMELINE = ChatSolver(
        "Timeline",
        "Use this once you have all the data needed to visualize the result on a timeline.",
        "The data to visualize",
        "Visualization specification (structured content)",
        "tool-timeline"
    )

    //endregion

    @Test
    fun `test WorkflowExecutor with a timeline visualization`() {
        val executor = WExecutorChat(GPT35, maxTokens = 1000, temp = 0.3)
        val exec = WorkflowExecutor(executor, listOf(SOLVER_QUERY, SOLVER_TIMELINE))
        val problem = WorkflowUserRequest("What is the timeline of the life of Albert Einstein?")
        exec.solve(problem)
    }

    @Test
    fun `test WorkflowExecutor with a timeline visualization (presidents)`() {
        val executor = WExecutorChat(GPT35, maxTokens = 1000, temp = 0.3)
        val exec = WorkflowExecutor(executor, listOf(SOLVER_QUERY, SOLVER_TIMELINE))
        val problem = WorkflowUserRequest("Give me a timeline visualization of the lifetimes and terms of the first 10 US presidents.")
        exec.solve(problem)
    }

}