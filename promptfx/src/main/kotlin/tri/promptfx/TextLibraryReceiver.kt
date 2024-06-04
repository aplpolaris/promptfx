package tri.promptfx

import tri.promptfx.tools.TextLibraryInfo

/** Marks a view as capable of receiving a text library. */
interface TextLibraryReceiver {
    fun loadTextLibrary(library: TextLibraryInfo)
}