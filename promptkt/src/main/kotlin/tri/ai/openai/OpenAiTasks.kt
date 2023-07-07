package tri.ai.openai

import com.fasterxml.jackson.module.kotlin.readValue
import tri.ai.prompt.AiPromptLibrary
import tri.ai.core.TextCompletion
import tri.ai.pips.aitask

/** Generate a task that adds user input to a prompt. */
suspend fun TextCompletion.promptTask(promptId: String, input: String, tokenLimit: Int, stop: String? = null) =
    AiPromptLibrary.lookupPrompt(promptId).prompt(input).let {
        complete(it, tokenLimit, stop)
    }

/** Generate a task that combines a single instruction or question about contextual text. */
suspend fun TextCompletion.instructTask(promptId: String, instruct: String, userText: String, tokenLimit: Int) =
    AiPromptLibrary.lookupPrompt(promptId).instruct(instruct, userText).let {
        complete(it, tokens = tokenLimit)
    }

/** Generate a task that fills inputs into a prompt. */
suspend fun TextCompletion.templateTask(promptId: String, vararg fields: Pair<String, String>, tokenLimit: Int) =
    AiPromptLibrary.lookupPrompt(promptId).fill(*fields).let {
        complete(it, tokens = tokenLimit)
    }

//region CONVERTING TASKS

/** Generate a task that adds user input to a prompt, and attempt to convert the result to json if possible. */
suspend inline fun <reified T> TextCompletion.jsonPromptTask(id: String, input: String, tokenLimit: Int) =
    promptTask(id, input, tokenLimit).let {
        it.map { mapper.readValue<T>(it.trim()) }
    }

//endregion

//region PLANNERS

/** Planner that generates a plan for a single completion prompt. */
fun TextCompletion.promptPlan(promptId: String, input: String, tokenLimit: Int, stop: String? = null) = aitask(promptId) {
    promptTask(promptId, input, tokenLimit, stop)
}.planner

/** Planner that generates a plan for a single instruction or question about user's text. */
fun TextCompletion.instructTextPlan(promptId: String, instruct: String, userText: String, tokenLimit: Int) = aitask(promptId) {
    instructTask(promptId, instruct, userText, tokenLimit)
}.planner

/** Planner that generates a plan to fill inputs into a prompt. */
fun TextCompletion.templatePlan(promptId: String, vararg fields: Pair<String, String>, tokenLimit: Int) = aitask(promptId) {
    templateTask(promptId, *fields, tokenLimit = tokenLimit)
}.planner

//endregion