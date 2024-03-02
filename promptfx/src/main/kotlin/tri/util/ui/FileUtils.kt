package tri.util.ui

import java.io.File

/** Return this if its a directory, otherwise try to find a parent directory. If none, return null. */
internal fun File.findDirectory(): File? = if (isDirectory) this else parentFile?.findDirectory()
