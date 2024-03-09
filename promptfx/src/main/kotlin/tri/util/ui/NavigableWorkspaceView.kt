/*-
 * #%L
 * tri.promptfx:promptfx
 * %%
 * Copyright (C) 2023 - 2024 Johns Hopkins University Applied Physics Laboratory
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

import tornadofx.Workspace
import java.util.*

/** A view that can be added to a workspace. */
interface NavigableWorkspaceView {

    val category: String
    val name: String
//    val description: String
//    val icon: String

    /** Add the view to the workspace. */
    fun dock(workspace: Workspace)

    companion object {
        val viewPlugins: List<NavigableWorkspaceView> by lazy {
            ServiceLoader.load(NavigableWorkspaceView::class.java).toList().onEach {
                println("Loaded NavigableWorkspaceView: ${it.category} - ${it.name}")
            }
        }
    }
}

