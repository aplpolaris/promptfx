/*-
 * #%L
 * tri.promptfx:promptkt
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
package tri.ai.research

object ResearchModels {}

/** Captures an information request. */
data class InformationRequest(
    val request: String
)

/** Captures a research plan for a given information request. */
data class ResearchPlan(
    val objectives: List<String>,
    val tasks: List<String>,
    val queries: List<String>,
    val deliverables: List<String>
)

/** Collection of resources gathered to address an information request/plan. */
data class ResearchPack(
    val resources: List<ResearchResource>
)

/** A single resource in a research pack. */
data class ResearchResource(
    val title: String
    // TODO
)

