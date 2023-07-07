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