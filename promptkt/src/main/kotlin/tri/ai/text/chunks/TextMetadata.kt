package tri.ai.text.chunks

import com.fasterxml.jackson.annotation.JsonInclude
import java.time.LocalDate

/** Metadata for text documents and chunks. */
@JsonInclude(JsonInclude.Include.NON_DEFAULT)
data class TextMetadata(
    val id: String,
    val path: String? = null,
    val title: String? = null,
    val author: String? = null,
    val date: LocalDate? = null
)