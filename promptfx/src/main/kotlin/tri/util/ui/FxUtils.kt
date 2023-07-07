/*-
 * #%L
 * promptfx-0.1.0-SNAPSHOT
 * %%
 * Copyright (C) 2023 Johns Hopkins University Applied Physics Laboratory
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

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.KotlinModule
import de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon
import de.jensd.fx.glyphs.fontawesome.FontAwesomeIconView
import javafx.beans.property.Property
import javafx.event.EventTarget
import javafx.scene.control.Slider
import javafx.scene.paint.Color
import tornadofx.ResourceLookup
import tornadofx.slider

fun icon(icon: FontAwesomeIcon) = FontAwesomeIconView(icon)

val FontAwesomeIcon.graphic
    get() = icon(this)

val FontAwesomeIconView.gray
    get() = apply {
        fill = Color.GRAY
    }

fun EventTarget.slider(range: ClosedRange<Double>, value: Property<Number>, op: Slider.() -> Unit = {}) =
    slider(range, 0.0, null, op).apply {
        valueProperty().bindBidirectional(value)
    }

fun EventTarget.slider(range: IntRange, value: Property<Number>, op: Slider.() -> Unit = {}) =
    slider(range, 0, null, op).apply {
        valueProperty().bindBidirectional(value)
    }

val MAPPER = ObjectMapper(YAMLFactory()).apply {
    registerModule(KotlinModule())
    registerModule(JavaTimeModule())
}

fun ResourceLookup.yaml(resource: String) =
    stream(resource).use { MAPPER.readValue(it, Map::class.java) }
