package br.com.gamemods.koterite.filesystem

import platform.posix.*

actual inline class File actual constructor(actual val handler: FileHandler): Comparable<File> {
    actual constructor(path: String) : this(FileHandler(path))
    actual constructor(parent: String?, child: String) : this(FileHandler("$parent/$child"))
    actual constructor(parent: File?, child: String) : this(parent?.handler?.path, child)

    actual fun canRead() = access(handler.path, R_OK) == 0
    actual fun canWrite() = access(handler.path, W_OK) == 0
    actual fun exists() = access(handler.path, F_OK) == 0
    actual fun canExecute() = access(handler.path, X_OK) == 0

    //@Throws(IOException::class)
    actual fun createNewFile(): Boolean {
        TODO("Not yet implemented")
    }

    actual fun delete() = remove(handler.path) == 0

    actual fun deleteOnExit() {
        TODO()
    }

    actual fun list(): Array<String>? {
        TODO("Not yet implemented")
    }

    actual fun listFiles(): Array<File>? {
        TODO("Not yet implemented")
    }

    actual fun mkdir(): Boolean {
        TODO("Not yet implemented")
    }

    actual fun mkdirs(): Boolean {
        TODO("Not yet implemented")
    }

    actual fun renameTo(dest: File): Boolean {
        TODO("Not yet implemented")
    }

    actual fun setLastModified(time: Long): Boolean {
        TODO("Not yet implemented")
    }

    actual fun setReadOnly(): Boolean {
        TODO("Not yet implemented")
    }

    actual fun setWritable(writable: Boolean, ownerOnly: Boolean): Boolean {
        TODO("Not yet implemented")
    }

    actual fun setWritable(writable: Boolean): Boolean {
        TODO("Not yet implemented")
    }

    actual fun setReadable(readable: Boolean, ownerOnly: Boolean): Boolean {
        TODO("Not yet implemented")
    }

    actual fun setReadable(readable: Boolean): Boolean {
        TODO("Not yet implemented")
    }

    actual fun setExecutable(executable: Boolean, ownerOnly: Boolean): Boolean {
        TODO("Not yet implemented")
    }

    actual fun setExecutable(executable: Boolean): Boolean {
        TODO("Not yet implemented")
    }

    actual override fun toString() = handler.path
    override fun compareTo(other: File): Int {
        TODO("Not yet implemented")
    }
}


actual inline val File.name: String get() = TODO()
actual inline val File.parent: String get() = TODO()
actual inline val File.parentFile: File get() = TODO()
actual inline val File.path: String get() = TODO()
actual inline val File.isAbsolute: Boolean get() = TODO()
actual inline val File.absolutePath: String get() = TODO()
actual inline val File.absoluteFile: File get() = TODO()
actual inline val File.canonicalPath: String get() = TODO()
actual inline val File.canonicalFile: File get() = TODO()

actual inline val File.isDirectory: Boolean get() = TODO()
actual inline val File.isFile: Boolean get() = TODO()
actual inline val File.isHidden: Boolean get() = TODO()

actual inline val File.totalSpace: Long get() = TODO()
actual inline val File.freeSpace: Long get() = TODO()
actual inline val File.usableSpace: Long get() = TODO()

//@set:JvmThrows(IOException::class)
actual inline var File.lastModified: Long
    get() = TODO()
    set(value) {
        TODO()
    }

actual inline val File.length: Long get() = TODO()
