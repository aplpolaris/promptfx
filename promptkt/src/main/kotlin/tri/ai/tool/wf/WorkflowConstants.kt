package tri.ai.tool.wf

const val REQUEST = "request"
const val INPUT = "input"
const val INTERMEDIATE_RESULTS = "intermediate_results"
const val RESULT = "result"

const val ANSWERED = "answered"
const val RATIONALE = "rationale"

const val AGGREGATOR_PROMPT_ID = "tool-aggregate"
const val USER_REQUEST_PARAM = "user_request"
const val INPUTS_PARAM = "inputs"

const val VALIDATOR_PROMPT_ID = "tool-validate"
const val PROPOSED_RESULT_PARAM = "proposed_result"
const val VALIDATED_RESULT = "validated_result"
const val FINAL_RESULT_ID = "${WorkflowValidatorTask.TASK_ID}.$VALIDATED_RESULT"