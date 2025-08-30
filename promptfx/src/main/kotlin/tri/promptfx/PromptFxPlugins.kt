package tri.promptfx

import tri.util.info
import java.util.ServiceLoader
import kotlin.io.path.Path
import kotlin.io.path.listDirectoryEntries

object PromptFxPlugins {

    private const val PLUGIN_DIR = "config/plugins/"

    /** Load plugins from config/plugins/ folder. */
    fun <C> loadPlugins(type: Class<C>): List<C> {
        val pluginFiles = listOf(Path(PLUGIN_DIR))
            .flatMap { it.listDirectoryEntries("*.jar") }
        if (pluginFiles.isNotEmpty()) {
            val urls = pluginFiles.map { it.toUri().toURL() }.toTypedArray()
            info<PromptFxPlugins>("Loading plugins from $PLUGIN_DIR - " + pluginFiles.joinToString(", ") { it.fileName.toString() })
            val loader = java.net.URLClassLoader(urls, type.classLoader)
            val serviceLoader = ServiceLoader.load(type, loader)
            return mutableListOf<C>().apply {
                serviceLoader.forEach { add(it) }
                info<PromptFxPlugins>("Loaded $size ${type.simpleName} plugins.")
            }
        } else {
            info<PromptFxPlugins>("No ${type.simpleName} plugins found.")
        }
        return listOf()
    }
}