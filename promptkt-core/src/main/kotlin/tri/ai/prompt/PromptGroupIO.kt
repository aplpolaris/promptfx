/*-
 * #%L
 * tri.promptfx:promptkt
 * %%
 * Copyright (C) 2023 - 2026 Johns Hopkins University Applied Physics Laboratory
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
package tri.ai.prompt

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory
import com.fasterxml.jackson.dataformat.yaml.YAMLGenerator
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.KotlinModule
import tri.util.fine
import tri.util.warning
import java.io.InputStreamReader
import java.net.JarURLConnection
import java.net.URL
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths

/** Tools for loading a [PromptGroup] from a resource file with support for reading some legacy definitions. */
object PromptGroupIO {

    /** ObjectMapper for loading prompts. */
    val MAPPER = ObjectMapper(YAMLFactory()
        .enable(YAMLGenerator.Feature.MINIMIZE_QUOTES)
        .enable(YAMLGenerator.Feature.LITERAL_BLOCK_STYLE)
        .enable(YAMLGenerator.Feature.INDENT_ARRAYS)
    ).apply {
        registerModule(KotlinModule.Builder().build())
        registerModule(JavaTimeModule())
    }

    //region LOADING FROM RESOURCES

    /** Load a single PromptGroup from a classpath resource. */
    fun readFromResource(resourcePath: String, cl: ClassLoader = Thread.currentThread().contextClassLoader): PromptGroup {
        fine<PromptGroupIO>("Loading prompt group from resource: $resourcePath")
        val isr = cl.getResourceAsStream(resourcePath)
            ?: PromptGroupIO::class.java.getResourceAsStream(resourcePath)
            ?: PromptGroupIO::class.java.getResourceAsStream("resources/$resourcePath")
            ?: error("Resource not found on classpath: $resourcePath")
        InputStreamReader(isr, StandardCharsets.UTF_8).use { reader ->
            return MAPPER.readValue(reader, PromptGroup::class.java).resolved()
        }
    }

    /** Load multiple PromptGroups from explicit classpath resource paths. */
    fun readFromResources(resourcePaths: List<String>, cl: ClassLoader = Thread.currentThread().contextClassLoader): List<PromptGroup> =
        resourcePaths.map { readFromResource(it, cl) }

    //endregion

    //region LOADING FROM PATHS

    /** Load a single PromptGroup from a runtime file. */
    fun readFromFile(path: Path) =
        Files.newBufferedReader(path, StandardCharsets.UTF_8).use { reader ->
            fine<PromptGroupIO>("Loading prompt group from file: $path")
            MAPPER.readValue(reader, PromptGroup::class.java).resolved()
        }

    //endregion

    //region FINDING RESOURCES/FILES

    private fun String.yaml() = endsWith(".yaml", true) || endsWith(".yml", true)

    /**
     * Load all [PromptGroup]s from a directory.
     * Scans for *.yaml and *.yml (optionally recursive).
     */
    fun readFromDirectory(dir: Path, recursive: Boolean = true): List<PromptGroup> {
        require(Files.isDirectory(dir)) { "Not a directory: $dir" }
        val stream = if (recursive) Files.walk(dir) else Files.list(dir)
        stream.use { s ->
            val files = s.filter { Files.isRegularFile(it) }
                .filter { it.fileName.toString().yaml() }
                .toList()
            return files.map { readFromFile(it) }
        }
    }

    /**
     * Load all [PromptGroup]s from a jar file.
     */
    fun collectYamlResourcesFromJarDir(url: URL, recursive: Boolean): List<String> {
        val conn = url.openConnection() as JarURLConnection
        val jar = conn.jarFile
        val prefix = conn.entryName.let { if (it.endsWith("/")) it else "$it/" }

        val out = linkedSetOf<String>()
        val entries = jar.entries()
        while (entries.hasMoreElements()) {
            val e = entries.nextElement()
            if (!e.isDirectory && e.name.startsWith(prefix) && e.name.yaml())
                if (recursive || !e.name.substring(prefix.length).contains('/'))
                    out += e.name
        }
        return out.toList()
    }

    /**
     * Load all [PromptGroup]s from a classpath resource directory.
     */
    fun readAllFromResourceDirectory(
        basePackage: String = PromptGroupIO::class.java.`package`.name + ".resources",
        recursive: Boolean = true,
        cl: ClassLoader = Thread.currentThread().contextClassLoader
    ): List<PromptGroup> {
        val groups = mutableListOf<PromptGroup>()
        val jarResourcePaths = mutableSetOf<String>()

        val basePath = basePackage.replace('.', '/') + "/"
        cl.getResources(basePath).toList().forEach { url ->
            when (url.protocol.lowercase()) {
                "file" ->
                    // resources in exploded directory (e.g., during dev/tests)
                    groups += readFromDirectory(Paths.get(url.toURI()), recursive)
                "jar" ->
                    // resources inside a JAR
                    jarResourcePaths += collectYamlResourcesFromJarDir(url, recursive)
                else ->
                    warning<PromptGroupIO>("Unsupported resource protocol: ${url.protocol}. " +
                        "Only 'file' and 'jar' protocols are supported for loading prompt groups.")
            }
        }

        return groups + jarResourcePaths.map { readFromResource(it, cl) }
    }

    //endregion

}
