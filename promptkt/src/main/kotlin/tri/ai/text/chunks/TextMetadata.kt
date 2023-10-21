package tri.ai.text.chunks

import java.time.LocalDate

/** Metadata for text documents and chunks. */
data class TextMetadata(
    val id: String,
    val path: String? = null,
    val title: String? = null,
    val author: String? = null,
    val date: LocalDate? = null
)