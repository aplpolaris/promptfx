/*-
 * #%L
 * tri.promptfx:promptfx-sample-view-plugin
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
import tri.promptfx.sample.SamplePlugin;
import tri.util.ui.NavigableWorkspaceView;

module tri.promptfx.sample.view.plugin {
    requires transitive tri.promptfx;
    requires kotlin.stdlib;
    requires tornadofx;
    requires javafx.controls;
    requires javafx.fxml;
    requires javafx.graphics;

    exports tri.promptfx.sample;

    // services (service loader API)
    uses tri.util.ui.NavigableWorkspaceView;
    provides NavigableWorkspaceView with SamplePlugin;
}