package tri.promptfx

import javafx.stage.FileChooser
import tornadofx.*
import tri.promptfx.tools.TextLibraryView
import tri.util.loggerFor
import java.io.File
import java.net.URI
import java.net.URISyntaxException

/**
 * Central management of configuration options for PromptFX.
 * All configuration that is saved/restored across multiple runs should be managed here.
 */
class PromptFxConfig: Component(), ScopedInstance {

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
    fun libraryFiles(): List<File> = try {
        (config.getProperty(TEXTLIB_FILES) ?: "").split(",")
            .map { URI.create(it) }
            .map { File(it) }
    } catch (x: IllegalArgumentException) {
        loggerFor<PromptFxConfig>().warning("Error loading text library files: ${x.message}")
        listOf()
    }

    /** Save configuration options before closing application. */
    fun save() {
        val libs = find<TextLibraryView>().libraryList
        config[TEXTLIB_FILES] = libs.mapNotNull { it.file?.toURI()?.toString() }.joinToString(",")
        config.save()
    }

    companion object {
        private const val DIR_PREFIX = "dir."
        private const val DIR_FILE_PREFIX = "dir_file."

        const val TEXTLIB_FILES = "textlib.files"

        const val DIR_KEY_TEXTLIB = "textlib"
        const val DIR_KEY_TXT = "txt"
        const val DIR_KEY_TRACE = "trace"

        val FF_JSON = FileChooser.ExtensionFilter("JSON", "*.json")
        val FF_TXT = FileChooser.ExtensionFilter("Text Files", "*.txt", "*.md")
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