package tri.ai.pips

/** Pipeline for chaining together collection of tasks to be accomplished by AI or APIs. */
object AiPipelineExecutor {

    /**
     * Execute tasks in order, chaining results from one to another.
     * Returns the table of execution results.
     */
    suspend fun execute(tasks: List<AiTask<*>>, monitor: AiTaskMonitor): AiPipelineResult {
        require(tasks.isNotEmpty())

        val completedTasks = mutableMapOf<String, AiTaskResult<*>>()
        val failedTasks = mutableMapOf<String, AiTaskResult<*>>()

        var tasksToDo: List<AiTask<*>>
        do {
            tasksToDo = tasks.filter {
                it.id !in completedTasks && it.id !in failedTasks
            }.filter {
                it.dependencies.all { it in completedTasks && completedTasks[it]!!.error == null }
            }
            tasksToDo.forEach {
                try {
                    monitor.taskStarted(it)
                    val input = it.dependencies.associateWith { completedTasks[it]!! }
                    val result = it.execute(input, monitor)
                    if (result.error == null) {
                        monitor.taskCompleted(it, result.value)
                        completedTasks[it.id] = result
                    } else {
                        monitor.taskFailed(it, result.error)
                        failedTasks[it.id] = result
                    }
                } catch (x: Exception) {
                    x.printStackTrace()
                    monitor.taskFailed(it, x)
                    failedTasks[it.id] = AiTaskResult.error(x.message!!, x)
                }
            }
        } while (tasksToDo.isNotEmpty())

        return AiPipelineResult(tasks.last().id, completedTasks + failedTasks)
    }

}

