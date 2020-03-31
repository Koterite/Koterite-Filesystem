package br.com.gamemods.koterite.filesystem

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
