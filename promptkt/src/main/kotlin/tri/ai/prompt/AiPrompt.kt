package tri.ai.prompt

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonValue
import com.github.mustachejava.DefaultMustacheFactory
import com.github.mustachejava.MustacheResolver
import com.github.mustachejava.resolver.DefaultResolver
import java.io.StringReader
import java.io.StringWriter
import java.time.LocalDate

/** A prompt template that can be filled in with a user input. */
class AiPrompt @JsonCreator(mode = JsonCreator.Mode.DELEGATING) constructor (@JsonValue var template: String) {

    /** Fills in input field. */
    fun prompt(input: String) = template.fill(
        "input" to input,
        "today" to LocalDate.now()
    )

    /** Fills in input and instruct fields. */
    fun instruct(instruct: String, input: String) = template.fill(
        "input" to input,
        "instruct" to instruct,
        "today" to LocalDate.now()
    )

    /** Fills in arbitrary fields. */
    fun fill(vararg fields: Pair<String, String>) = template.fill(
        "today" to LocalDate.now(),
        *fields
    ).also {
        println(it)
    }

    companion object {

        //region MUSTACHE TEMPLATES

        /** Fills in mustache template. */
        private fun String.fill(vararg fields: Pair<String, Any>) =
            StringWriter().apply {
                mustacheFactory(this@fill).compile(this@fill).execute(this, fields.toMap())
            }.toString()

        private fun mustacheFactory(template: String) = DefaultMustacheFactory {
            StringReader(template)
        }

        //endregion

    }

}