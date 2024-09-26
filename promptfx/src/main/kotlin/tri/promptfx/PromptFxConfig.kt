/*-
 * #%L
 * tri.promptfx:promptfx
 * %%
 * Copyright (C) 2023 - 2024 Johns Hopkins University Applied Physics Laboratory
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
package tri.promptfx

import javafx.stage.FileChooser
import tornadofx.*
import tri.promptfx.library.TextClusterView
import tri.promptfx.library.TextManagerView
import tri.util.loggerFor
import java.io.File
import java.net.URI

/**
 * Central management of configuration options for PromptFX.
 * All configuration that is saved/restored across multiple runs should be managed here.
 */
class PromptFxConfig: Component(), ScopedInstance {

    /** Whether starship button is enabled. */
    var isStarshipEnabled: Boolean = false

    /** Management of local file/folder directory selections. */
    private val directories by lazy {
        mutableMapOf<String, File>().apply {
            put("default", File(System.getProperty("user.home")))
            config.keys.filterIsInstance<String>().filter { it.startsWith(DIR_PREFIX) }.forEach {
                put(it.substringAfter(DIR_PREFIX), File(config.getProperty(it)))
            }
        }
    }
    /** Management of local file/folder directory selections. */
    private val directoryFiles by lazy {
        mutableMapOf<String, String>().apply {
            config.keys.filterIsInstance<String>().filter { it.startsWith(DIR_FILE_PREFIX) }.forEach {
                put(it.substringAfter(DIR_FILE_PREFIX), config.getProperty(it))
            }
        }
    }

    /** Get the directory for a given key. */
    fun directory(key: String): File = directories[key] ?: directories["default"]!!

    /** Get the directory file for a given key. */
    fun directoryFile(key: String): String? = directoryFiles[key]

    /** Update directory for a given key. */
    fun updateDirectory(key: String, file: File) {
        directoryFiles[key] = file.name
        config[DIR_FILE_PREFIX + key] = file.absolutePath
        directories[key] = file.parentFile
        config[DIR_PREFIX + key] = file.parentFile.absolutePath
    }

    /** Get library files from configuration. */
    fun textManagerFiles(): List<File> = loadLibrary(TEXTLIB_FILES)
    /** Get cluster files from configuration. */
    fun textClusterFiles(): List<File> = loadLibrary(CLUSTERLIB_FILES)

    private fun loadLibrary(key: String): List<File> {
        val value = (config.getProperty(key) ?: "").trim()
        if (value.isNotBlank()) {
            try {
                return (config.getProperty(key) ?: "").split(",")
                    .map { URI.create(it) }
                    .map { File(it) }
            } catch (x: IllegalArgumentException) {
                loggerFor<PromptFxConfig>().warning("Error loading text library files: ${x.message}. Value was $value")
            }
        }
        return listOf()
    }

    /** Save configuration options before closing application. */
    fun save() {
        find<TextManagerView>().model.libraryList
            .mapNotNull { it.file?.toURI()?.toString() }.joinToString(",")
            .let {
                if (it.isNotBlank()) config[TEXTLIB_FILES] = it
            }
        find<TextClusterView>().model.libraryList
            .mapNotNull { it.file?.toURI()?.toString() }.joinToString(",")
            .let {
                if (it.isNotBlank()) config[CLUSTERLIB_FILES] = it
            }
        config.save()
    }

    companion object {
        private const val DIR_PREFIX = "dir."
        private const val DIR_FILE_PREFIX = "dir_file."

        const val TEXTLIB_FILES = "textlib.files"
        const val CLUSTERLIB_FILES = "clusterlib.files"

        const val DIR_KEY_TEXTLIB = "textlib"
        const val DIR_KEY_TXT = "txt"
        const val DIR_KEY_TRACE = "trace"
        const val DIR_KEY_IMAGE = "image"

        val FF_JSON = FileChooser.ExtensionFilter("JSON", "*.json")
        val FF_TXT = FileChooser.ExtensionFilter("Text Files", "*.txt", "*.md")
        val FF_PNG = FileChooser.ExtensionFilter("PNG Images", "*.png")
        val FF_IMAGE = FileChooser.ExtensionFilter("Images", "*.png", "*.jpg", "*.jpeg", "*.gif", "*.bmp", "*.tiff")
        val FF_ALL = FileChooser.ExtensionFilter("All Files", "*.*")
    }

}

/** Show a file chooser dialog with given settings, using global PromptFx config to manage initial directory. */
fun UIComponent.promptFxFileChooser(
    title: String,
    filters: Array<FileChooser.ExtensionFilter> = emptyArray(),
    mode: FileChooserMode = FileChooserMode.Single,
    dirKey: String = "default",
    onComplete: (List<File>) -> Unit
) = chooseFile(
    title = title,
    filters = filters,
    initialDirectory = find<PromptFxConfig>().directory(dirKey),
    initialFileName = find<PromptFxConfig>().directoryFile(dirKey),
    mode = mode,
    owner = currentWindow,
    op = { }
).let {
    if (it.isNotEmpty()) {
        find<PromptFxConfig>().updateDirectory(dirKey, it.first())
        onComplete(it)
    }
}

/** Show a directory chooser dialog with given settings, using global PromptFx config to manage initial directory. */
fun UIComponent.promptFxDirectoryChooser(
    title: String = "Select Folder",
    dirKey: String = "default",
    onComplete: (File) -> Unit
) = chooseDirectory(
    title = title,
    initialDirectory = find<PromptFxConfig>().directory(dirKey),
    owner = currentWindow,
    op = { }
).let {
    if (it != null) {
        find<PromptFxConfig>().updateDirectory(dirKey, it)
        onComplete(it)
    }
}
