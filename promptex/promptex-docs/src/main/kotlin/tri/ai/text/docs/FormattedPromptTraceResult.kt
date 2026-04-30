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
package tri.ai.text.docs

import tri.ai.prompt.trace.AiOutputInfo
import tri.ai.prompt.trace.AiTaskTrace

private const val FORMATTED_OUTPUTS_KEY = "formattedOutputs"

/**
 * Returns a copy of this trace annotated with a list of [FormattedText] outputs.
 * The outputs are stored in [AiOutputInfo.annotations] (accessed via [AiTaskTrace.annotations])
 * and can be retrieved via [formattedOutputs]. The trace must have a non-null [AiTaskTrace.output]
 * for the annotations to be retained; if [output] is null the annotations are silently discarded.
 */
fun AiTaskTrace.withFormattedOutputs(outputs: List<FormattedText>): AiTaskTrace =
    apply { annotations[FORMATTED_OUTPUTS_KEY] = outputs }

/**
 * Returns the formatted text outputs stored on this trace via [withFormattedOutputs], or `null` if none are present.
 */
@Suppress("UNCHECKED_CAST")
val AiTaskTrace.formattedOutputs: List<FormattedText>?
    get() = annotations[FORMATTED_OUTPUTS_KEY] as? List<FormattedText>
