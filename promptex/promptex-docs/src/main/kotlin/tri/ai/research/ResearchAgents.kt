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

import tri.ai.pips.api.AgentExecutable

val PROJECT_MANAGER_AGENT = AgentExecutable(
    name = "Project Manager Agent",
    description = "Plans and manages AI research projects based around a user's information request.",
    version = "1.0",
    null,
    null,
    listOf()
)

val RESEARCH_PACK_AGENT = AgentExecutable(
    name = "Research Pack Agent",
    description = "Gathers and curates a collection of resources to address a research plan.",
    version = "1.0",
    null,
    null,
    listOf()
)

val RESEARCH_PLANNER_AGENT = AgentExecutable(
    name = "Research Planner Agent",
    description = "Creates a detailed research plan based on an information request.",
    version = "1.0",
    null,
    null,
    listOf()
)
