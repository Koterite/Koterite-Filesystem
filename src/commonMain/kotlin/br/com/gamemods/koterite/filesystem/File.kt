package br.com.gamemods.koterite.filesystem

import br.com.gamemods.koterite.annotation.JvmThrows
import br.com.gamemods.koterite.annotation.Throws
import io.ktor.utils.io.errors.IOException

expect inline class File(private val handler: FileHandler): Comparable<File> {
    constructor(path: String)
    constructor(parent: String?, child: String)
    constructor(parent: File?, child: String)

    fun canRead(): Boolean
    fun canWrite(): Boolean
    fun exists(): Boolean

    @Throws(IOException::class)
    fun createNewFile(): Boolean

    fun delete(): Boolean
    fun deleteOnExit()

    fun list(): List<String>?
    fun listFiles(): List<File>?

    fun mkdir(): Boolean
    fun mkdirs(): Boolean
    fun renameTo(dest: File): Boolean

    fun setLastModified(time: Long): Boolean

    fun setReadOnly(): Boolean

    fun setWritable(writable: Boolean, ownerOnly: Boolean = true): Boolean
    fun setReadable(readable: Boolean, ownerOnly: Boolean = true): Boolean
    fun setExecutable(executable: Boolean, ownerOnly: Boolean = true): Boolean

    fun canExecute(): Boolean

    override fun toString(): String
}

expect val File.name: String
expect val File.parent: String?
expect val File.parentFile: File?
expect val File.path: String
expect val File.isAbsolute: Boolean
expect val File.absolutePath: String
expect val File.absoluteFile: File
expect val File.canonicalPath: String
expect val File.canonicalFile: File

expect val File.isDirectory: Boolean
expect val File.isFile: Boolean
expect val File.isHidden: Boolean

expect val File.totalSpace: Long
expect val File.freeSpace: Long
expect val File.usableSpace: Long

expect val File.length: Long

@set:JvmThrows(IOException::class)
expect var File.lastModified: Long

@set:JvmThrows(IOException::class)
inline var File.isReadable: Boolean
    get() = canRead()
    set(value) {
        if (!setReadable(value, false)) {
            throw IOException("Could not make the file readable: $this")
        }
    }

@set:JvmThrows(IOException::class)
inline var File.isWritable: Boolean
    get() = canWrite()
    set(value) {
        if (!setWritable(value, false)) {
            throw IOException("Could not make the file writable: $this")
        }
    }

@set:JvmThrows(IOException::class)
inline var File.isExecutable: Boolean
    get() = canExecute()
    set(value) {
        if (!setExecutable(value, false)) {
            throw IOException("Could not make the file executable: $this")
        }
    }
