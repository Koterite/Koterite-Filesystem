package br.com.gamemods.koterite.filesystem

import java.io.IOException

actual inline class File actual constructor(actual val handler: FileHandler) : Comparable<File> {
    actual constructor(path: String) : this(FileHandler(path))
    actual constructor(parent: String?, child: String) : this(FileHandler(parent, child))
    actual constructor(parent: File?, child: String) : this(FileHandler(parent?.handler, child))

    actual fun canRead() = handler.canRead()
    actual fun canWrite() = handler.canWrite()
    actual fun exists() = handler.exists()
    actual fun createNewFile() = handler.createNewFile()
    actual fun delete() = handler.delete()

    actual fun deleteOnExit() {
        handler.deleteOnExit()
    }

    actual fun list(): Array<String>? = handler.list()

    @Suppress("UNCHECKED_CAST")
    actual fun listFiles() = handler.listFiles() as Array<File>?

    actual fun mkdir() = handler.mkdir()
    actual fun mkdirs() = handler.mkdirs()
    actual fun renameTo(dest: File) = handler.renameTo(dest.handler)
    actual fun setLastModified(time: Long) = handler.setLastModified(time)
    actual fun setReadOnly() = handler.setReadOnly()
    actual fun setWritable(writable: Boolean, ownerOnly: Boolean) = handler.setWritable(writable, ownerOnly)
    actual fun setWritable(writable: Boolean) = handler.setWritable(writable)
    actual fun setReadable(readable: Boolean, ownerOnly: Boolean) = handler.setReadable(readable, ownerOnly)
    actual fun setReadable(readable: Boolean) = handler.setReadable(readable)
    actual fun setExecutable(executable: Boolean, ownerOnly: Boolean) = handler.setExecutable(executable, ownerOnly)
    actual fun setExecutable(executable: Boolean) = handler.setExecutable(executable)
    actual fun canExecute() = handler.canExecute()
    actual override fun toString() = handler.toString()
    override fun compareTo(other: File) = handler.compareTo(other.handler)
}

actual inline val File.name: String get() = handler.name
actual inline val File.parent: String get() = handler.parent
actual inline val File.parentFile: File get() = File(handler.parentFile)
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
            throw IOException()
        }
    }
