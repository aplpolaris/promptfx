package tri.ai.pips

/** Takes user input and generates a series of tasks to be executed. */
interface AiPlanner {

    fun plan(): List<AiTask<*>>

    /** Executes the plan with [AiPipelineExecutor]. */
    suspend fun execute(monitor: AiTaskMonitor) = AiPipelineExecutor.execute(plan(), monitor)

}