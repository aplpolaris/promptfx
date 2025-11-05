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
package tri.ai.core.agent.wf

const val REQUEST = tri.util.PARAM_REQUEST
const val INPUT = tri.util.PARAM_INPUT
const val INTERMEDIATE_RESULTS = "intermediate_results"
const val RESULT = tri.util.PARAM_RESULT

const val ANSWERED = "answered"
const val RATIONALE = "rationale"

const val PLANNER_PROMPT_ID = "workflow/planning"

const val AGGREGATOR_PROMPT_ID = "workflow/aggregation"
const val USER_REQUEST_PARAM = "user_request"
const val INPUTS_PARAM = "inputs"

const val VALIDATOR_PROMPT_ID = "workflow/validation"
const val PROPOSED_RESULT_PARAM = "proposed_result"
const val VALIDATED_RESULT = "validated_result"
const val FINAL_RESULT_ID = "${WorkflowValidatorTask.TASK_ID}.$VALIDATED_RESULT"
