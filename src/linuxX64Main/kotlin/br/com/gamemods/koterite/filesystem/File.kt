package br.com.gamemods.koterite.filesystem

import io.ktor.utils.io.errors.IOException
import kotlinx.cinterop.*
import platform.posix.*

actual inline class File actual constructor(actual val handler: FileHandler): Comparable<File> {
    actual constructor(path: String) : this(FileHandler(path))
    actual constructor(parent: String?, child: String) : this(FileHandler("$parent/$child"))
    actual constructor(parent: File?, child: String) : this(parent?.handler?.path, child)

    actual fun canRead() = access(handler.path, R_OK) == 0
    actual fun canWrite() = access(handler.path, W_OK) == 0
    actual fun exists() = access(handler.path, F_OK) == 0
    actual fun canExecute() = access(handler.path, X_OK) == 0

    @Throws(IOException::class)
    actual fun createNewFile(): Boolean {
        if (exists()) {
            return false
        }

        val fp = fopen(handler.path, "ab+") ?: return false
        return fclose(fp) == 0
    }

    actual fun delete() = if (isDirectory) rmdir(handler.path) == 0 else remove(handler.path) == 0

    actual fun deleteOnExit() {
        if (filesPendingForRemoval.isEmpty()) {
            atexit(staticCFunction(::deletePendingFiles))
        }
        filesPendingForRemoval += handler.path
    }

    actual fun list() = handler.listMapping { d_name.toKString() }
    actual fun listFiles(): List<File>? = handler.listMapping { File(d_name.toKString()) }

    actual fun mkdir() = mkdir(handler.path, DEFAULT_DIR_FLAGS.convert()) == 0

    actual fun mkdirs(): Boolean {
        val builder = StringBuilder()
        var last = -1
        handler.path.split('/', '\\').forEach { name ->
            builder.append(name).append('/')
            last = mkdir(builder.toString(), DEFAULT_DIR_FLAGS.convert())
        }
        return last == 0
    }

    actual fun renameTo(dest: File) = rename(handler.path, dest.handler.path) == 0

    actual fun setLastModified(time: Long): Boolean {
        require(time >= 0) { "Negative time is not allowed" }
        val seconds = time / 1000L
        return memScoped {
            val buf = alloc<utimbuf>()
            buf.modtime = seconds
            buf.actime = seconds
            utime(handler.path, buf.ptr) == 0
        }
    }

    actual fun setReadOnly() = setWritable(writable = false, ownerOnly = false)

    actual fun setWritable(writable: Boolean, ownerOnly: Boolean): Boolean {
        return changeChmod(writable, if(ownerOnly) S_IWUSR else S_IWUSR or S_IWGRP or S_IWOTH)
    }

    actual fun setReadable(readable: Boolean, ownerOnly: Boolean): Boolean {
        return changeChmod(readable, if(ownerOnly) S_IRUSR else S_IRUSR or S_IRGRP or S_IROTH)
    }

    actual fun setExecutable(executable: Boolean, ownerOnly: Boolean): Boolean {
        return changeChmod(executable, if(ownerOnly) S_IXUSR else S_IXUSR or S_IXGRP or S_IXOTH)
    }

    private fun changeChmod(value: Boolean, flags: Int): Boolean {
        val mode = stat { st_mode }.convert<Int>()
        if (value) {
            if (mode and flags == flags) {
                return true
            }
            return chmod(handler.path, (mode or flags).convert()) == 0
        } else {
            if (mode and flags == 0) {
                return true
            }
            return chmod(handler.path, (mode and flags).inv().convert()) == 0
        }
    }

    actual override fun toString() = handler.path
    override fun compareTo(other: File): Int {
        return PathComparator.compare(handler.path, other.handler.path)
    }
}
