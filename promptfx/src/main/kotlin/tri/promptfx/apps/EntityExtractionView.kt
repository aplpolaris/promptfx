/*-
 * #%L
 * tri.promptfx:promptfx
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
package tri.promptfx.apps

import tri.promptfx.RuntimePromptView
import tri.promptfx.RuntimePromptViewConfigs
import tri.util.ui.NavigableWorkspaceViewImpl
import tri.util.ui.WorkspaceViewAffordance

/** Plugin for [EntityExtractionView]. */
class EntityExtractionPlugin : NavigableWorkspaceViewImpl<EntityExtractionView>("Text", "Entity Extraction", WorkspaceViewAffordance.INPUT_ONLY, EntityExtractionView::class)

/** View for prompts designed to extract entities from text. */
class EntityExtractionView: RuntimePromptView(RuntimePromptViewConfigs.config("entity-extraction"))
