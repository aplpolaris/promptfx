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
package tri.util.ui

import tornadofx.*
import kotlin.reflect.KClass

/** Partial implementation of [NavigableWorkspaceView]. */
abstract class NavigableWorkspaceViewImpl<T : UIComponent>(
    override val category: String,
    override val name: String,
    override val affordances: WorkspaceViewAffordance = WorkspaceViewAffordance.NONE,
    internal val type: KClass<T>
) : NavigableWorkspaceView {

    override fun dock(workspace: Workspace) {
        workspace.dock(find(type))
    }

}

