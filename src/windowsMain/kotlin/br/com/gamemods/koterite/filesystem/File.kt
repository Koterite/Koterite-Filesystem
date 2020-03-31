package br.com.gamemods.koterite.filesystem

import io.ktor.utils.io.errors.IOException
import kotlinx.cinterop.*
import platform.posix.*
import platform.windows.*
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract

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
        TODO("Not yet implemented")
    }
}

actual inline val File.name: String get() = handler.path.substringAfterLast('/')

actual val File.parent: String? get() = handler.path.let {
    if (it.isEmpty() || it == "/" || it.matches(Regex("""^[a-zA-Z]+:/?"""))) null
    else {
        val index = it.lastIndexOf('/')
        if (index < 1) null
        else it.substring(0, index)
    }
}

actual val File.isAbsolute: Boolean get() = handler.path.let {
    it.firstOrNull() == '/' || it.matches(Regex("^[a-zA-Z]+:.*"))
}

actual inline val File.parentFile: File? get() = parent?.let(::File)
actual inline val File.path: String get() = handler.path

actual inline val File.absolutePath: String get() {
    if (isAbsolute) return handler.path
    return absolutePath(handler.path)
}

actual inline val File.canonicalPath: String get() = if (!isAbsolute) handler.path else TODO()

actual inline val File.absoluteFile: File get() = if (isAbsolute) this else File(absolutePath)
actual inline val File.canonicalFile: File get() = if (!isAbsolute) this else File(canonicalPath)

actual inline val File.isDirectory: Boolean get() = statW { hasFlag(S_IFDIR) }
actual inline val File.isFile: Boolean get() = statW { hasFlag(S_IFREG) }

actual inline val File.isHidden: Boolean get() = memScoped {
    (GetFileAttributesW(handler.path).convert<Int>() and FILE_ATTRIBUTE_HIDDEN) == FILE_ATTRIBUTE_HIDDEN
}

actual inline val File.totalSpace: Long get() = TODO()
actual inline val File.freeSpace: Long get() = TODO()
actual inline val File.usableSpace: Long get() = TODO()

actual inline var File.lastModified: Long
    get() = statW { st_mtime } * 1000L
    set(value) {
        if (!setLastModified(value)) {
            throw IOException("Unable to change the last modified time")
        }
    }

actual inline val File.length: Long get() = statW { st_size }

inline fun <R> File.statW(operation: _stat64.() -> R): R {
    contract { callsInPlace(operation, InvocationKind.EXACTLY_ONCE) }
    return memScoped {
        val stat = alloc<_stat64>()
        _wstat64(handler.pathW, stat.ptr)
        operation(stat)
    }
}

@Suppress("NOTHING_TO_INLINE")
inline fun _stat64.hasFlag(flag: Int) = (st_mode.convert<Int>() and flag) == flag

private val filesPendingForRemoval = mutableSetOf<String>()

private fun deletePendingFiles() {
    memScoped {
        val stat = alloc<_stat64>()
        val ptr = stat.ptr
        filesPendingForRemoval.reversed().forEach {
            val fileName = it.wcstr
            _wstat64(it.wcstr, ptr)
            if (stat.hasFlag(S_IFDIR)) {
                _wrmdir(fileName)
            } else {
                _wremove(fileName)
            }
        }
    }
}

fun <R> FileHandler.listMappingW(mapper: _wdirent.() -> R): List<R>? {
    val dir = _wopendir(pathW) ?: return null
    try {
        var entry = _wreaddir(dir) ?: return emptyList()
        val entries = mutableListOf<R>()
        while (true) {
            entries += entry.pointed.mapper()
            entry = _wreaddir(dir) ?: return entries
        }
    } finally {
        _wclosedir(dir)
    }
}

fun absolutePath(path: String): String {
    memScoped {
        val buffer = allocArray<wchar_tVar>(MAX_PATH)
        val written = GetCurrentDirectoryW(MAX_PATH, buffer).convert<Int>()
        if (written == 0) throw AssertionError("Could not get the current directory! Code: " + GetLastError())
        val currentDir = buffer.pointed.readValues(written).ptr.toKString()
        val absolute = PathCombineW(buffer, currentDir, path)
            ?: throw AssertionError("Could not combine the paths!\nBase: $currentDir\nRelative:$path\nCode: " + GetLastError())
        return absolute.toKString()
    }
}
