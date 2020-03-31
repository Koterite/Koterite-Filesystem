package br.com.gamemods.koterite.filesystem

import java.io.IOException

actual inline val File.name: String get() = handler.name
actual inline val File.parent: String? get() = handler.parent
actual inline val File.parentFile: File? get() = handler.parentFile?.let(::File)
actual inline val File.path: String get() = handler.path
actual inline val File.isAbsolute get() = handler.isAbsolute
actual inline val File.absolutePath: String get() = handler.absolutePath
actual inline val File.absoluteFile: File get() = File(handler.absoluteFile)
actual inline val File.canonicalPath: String get()= handler.canonicalPath
actual inline val File.canonicalFile: File get() = File(handler.canonicalFile)
actual inline val File.isDirectory get() = handler.isDirectory
actual inline val File.isFile get() = handler.isFile
actual inline val File.isHidden get() = handler.isHidden
actual inline val File.totalSpace get() = handler.totalSpace
actual inline val File.freeSpace get() = handler.freeSpace
actual inline val File.usableSpace get() = handler.usableSpace
actual inline val File.length get() = handler.length()

@set:Throws(IOException::class)
actual inline var File.lastModified: Long
    get() = handler.lastModified()
    set(value) {
        if (!handler.setLastModified(value)) {
            throw IOException("Unable to change the last modified time")
        }
    }
