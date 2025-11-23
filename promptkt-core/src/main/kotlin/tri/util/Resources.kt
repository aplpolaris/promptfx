/*-
 * #%L
 * tri.promptfx:promptkt
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
package tri.util

import java.io.InputStream
import java.nio.file.Paths

/**
 * Loads a single resource located in `...resources/<fileName>` under the package
 * of the given [owner] class. Works reliably in IntelliJ, Gradle, and inside JARs.
 * @param owner the class whose package is used to locate the resource directory
 * @param fileName the resource file name (e.g., "openai-models.yaml")
 * @return the resource input stream, or null if not found anywhere on the classpath
 */
fun loadResourceFromSiblingResources(
    owner: Class<*>,
    fileName: String,
    cl: ClassLoader = Thread.currentThread().contextClassLoader
): InputStream? {
    val basePackage = owner.`package`.name + ".resources"
    val basePath = basePackage.replace('.', '/') + "/"
    val dirs = cl.getResources(basePath).toList()
    val baseUrl = dirs.firstOrNull() ?: return null

    return when (baseUrl.protocol.lowercase()) {
        "file" -> {
            val dir = Paths.get(baseUrl.toURI())
            val filePath = dir.resolve(fileName)
            if (filePath.toFile().exists())
                filePath.toFile().inputStream()
            else null
        }
        "jar" -> {
            val resourcePath = basePath + fileName
            cl.getResourceAsStream(resourcePath)
        }
        else -> null
    }
}
