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
package tri.ai.tool.wf

/** General exception within dynamic workflow execution. */
open class WorkflowException(message: String, cause: Throwable? = null) : Exception(message, cause)

/** Workflow exception caused by tool not available/not found. */
class WorkflowToolNotFoundException(message: String, cause: Throwable? = null) : WorkflowException(message, cause)

/** Workflow exception caused by task not available/not found. */
class WorkflowTaskNotFoundException(message: String, cause: Throwable? = null) : WorkflowException(message, cause)
