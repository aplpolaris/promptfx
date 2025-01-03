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

import java.util.*
import java.util.zip.Deflater

/** Collection of utilities for working with PlantUML diagrams. */
object PlantUmlUtils {

    private const val BASE_URL = "http://www.plantuml.com/plantuml"

    /** Returns a URL to the PlantUML image for the given PlantUML code. */
    fun plantUmlUrlText(plantUml: String, type: String = "uml"): String {
        val compressor = Deflater(Deflater.DEFLATED, false)
        compressor.setInput(plantUml.toByteArray(Charsets.UTF_8))
        compressor.finish()
        val compressedData = ByteArray(plantUml.length)
        val compressedSize = compressor.deflate(compressedData)
        compressor.end()
        val encodedUml = Base64.getEncoder().withoutPadding()
            .encodeToString(compressedData.copyOf(compressedSize))
            .convertToPlantUmlBase()
        return "$BASE_URL/$type/~1${encodedUml}"
    }

    private const val FROM_ARR = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/="
    private const val TO_ARR = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz-_="

    private fun String.convertToPlantUmlBase() = try {
        map { TO_ARR[FROM_ARR.indexOf(it)] }.joinToString("")
    } catch (x: ArrayIndexOutOfBoundsException) {
        println("Error converting to PlantUML base: $this")
    }

}
