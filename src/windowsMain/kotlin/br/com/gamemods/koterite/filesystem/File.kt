package br.com.gamemods.koterite.filesystem

import io.ktor.utils.io.errors.IOException
import kotlinx.cinterop.*
import platform.posix.*
import platform.windows.GetLastError

actual inline class File actual constructor(actual val handler: FileHandler): Comparable<File> {
    actual constructor(path: String) : this(FileHandler(path))
    actual constructor(parent: String?, child: String) : this(FileHandler("$parent/$child"))
    actual constructor(parent: File?, child: String) : this(parent?.handler?.path, child)

    actual fun canRead() = _waccess_s(handler.pathW, R_OK) == 0
    actual fun canWrite() = _waccess_s(handler.pathW, W_OK) == 0
    actual fun exists() = _waccess_s(handler.pathW, F_OK) == 0
    actual fun canExecute() = _waccess_s(handler.pathW, X_OK) == 0

    @Throws(IOException::class)
    actual fun createNewFile(): Boolean {
        if (exists()) {
            return false
        }

        memScoped {
            val fp = allocPointerTo<FILE>()
            if (_wfopen_s(fp.ptr, handler.pathW, "ab+".wcstr) != 0) {
                return false
            }
            return fclose(fp.value) == 0
        }
    }

    actual fun delete() = if (isDirectory) _wrmdir(handler.pathW) == 0 else _wremove(handler.pathW) == 0

    actual fun deleteOnExit() {
        if (filesPendingForRemoval.isEmpty()) {
            if (atexit(staticCFunction(::deletePendingFiles)) == 0) {
                throw AssertionError("Failed to schedule the file $this for removal. Code: " + GetLastError())
            }
        }
        filesPendingForRemoval += handler.path
    }

    actual fun list(): List<String>? = handler.listMappingW { d_name.toKString().replace('\\', '/') }
    actual fun listFiles(): List<File>? = handler.listMappingW { File(d_name.toKString()) }

    actual fun mkdir() = _wmkdir(handler.pathW) == 0

    actual fun mkdirs(): Boolean {
        val builder = StringBuilder()
        var last = -1
        handler.path.split('/', '\\').forEach { name ->
            builder.append(name).append('/')
            last = _wmkdir(builder.toString().wcstr)
        }
        return last == 0
    }

    actual fun renameTo(dest: File) = _wrename(handler.path.wcstr, dest.handler.path.wcstr) == 0

    actual fun setLastModified(time: Long): Boolean {
        require(time >= 0) { "Negative time is not allowed" }
        val seconds = time / 1000L
        return memScoped {
            val buf = alloc<__utimbuf64>()
            buf.modtime = seconds
            buf.actime = seconds
            _wutime64(handler.path.wcstr, buf.ptr) == 0
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
        val mode = statW { st_mode }.convert<Int>()
        if (value) {
            if (mode and flags == flags) {
                return true
            }
            return _wchmod(handler.pathW, mode or flags) == 0
        } else {
            if (mode and flags == 0) {
                return true
            }
            return _wchmod(handler.pathW, mode and flags.inv()) == 0
        }
    }

    actual override fun toString() = handler.path
    override fun compareTo(other: File): Int {
        return PathComparator.compare(handler.path, other.handler.path)
    }
}
