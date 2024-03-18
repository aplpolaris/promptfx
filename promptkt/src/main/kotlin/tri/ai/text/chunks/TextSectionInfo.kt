package tri.ai.text.chunks

import com.fasterxml.jackson.annotation.JsonInclude

/** Serializable version of [TextChunkInBook]. */
class TextSectionInfo(
    var first: Int = 0,
    var last: Int = 0,
    @JsonInclude(JsonInclude.Include.NON_DEFAULT)
    var attributes: TextAttributes = mutableMapOf()
) {
    constructor(section: TextChunkInBook) : this() {
        first = section.first
        last = section.last
        attributes = section.attributes
    }
}