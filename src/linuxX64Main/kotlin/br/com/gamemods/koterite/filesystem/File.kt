package br.com.gamemods.koterite.filesystem

import io.ktor.utils.io.errors.IOException
import kotlinx.cinterop.*
import platform.posix.*
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract

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
        TODO("Not yet implemented")
    }
}

const val DEFAULT_DIR_FLAGS = S_IRWXU or S_IRWXG or S_IROTH or S_IXOTH

actual inline val File.name: String get() = handler.path.let {
    val index = it.lastIndexOfAny(charArrayOf('/', '\\'))
    if (index < 0) it
    else it.substring(index)
}

actual val File.parent: String? get() = handler.path.let {
    if (it.isEmpty() || it == "/" || it.matches(Regex("""^[a-zA-Z]+:[/\\]?"""))) null
    else {
        val index = it.lastIndexOfAny(charArrayOf('/', '\\'))
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

actual inline val File.isDirectory: Boolean get() = stat { hasFlag(S_IFDIR) }
actual inline val File.isFile: Boolean get() = stat { hasFlag(S_IFREG) }
actual inline val File.isHidden: Boolean get() = name.startsWith('.')
actual inline val File.totalSpace: Long get() = TODO()
actual inline val File.freeSpace: Long get() = TODO()
actual inline val File.usableSpace: Long get() = TODO()

actual inline var File.lastModified: Long
    get() = stat { st_mtim.tv_sec } * 1000L
    set(value) {
        if (!setLastModified(value)) {
            throw IOException("Unable to change the last modified time")
        }
    }

actual inline val File.length: Long get() = stat { st_size }.toLong()

inline fun <R> File.stat(operation: stat.() -> R): R {
    contract { callsInPlace(operation, InvocationKind.EXACTLY_ONCE) }
    return memScoped {
        val stat = alloc<stat>()
        stat(handler.path, stat.ptr)
        operation(stat)
    }
}

@Suppress("NOTHING_TO_INLINE")
inline fun stat.hasFlag(flag: Int) = (st_mode.convert<Int>() and flag) == flag

private val filesPendingForRemoval = mutableSetOf<String>()

private fun deletePendingFiles() {
    memScoped {
        val stat = alloc<stat>()
        val ptr = stat.ptr
        filesPendingForRemoval.reversed().forEach {
            stat(it, ptr)
            if (stat.hasFlag(S_IFDIR)) {
                rmdir(it)
            } else {
                remove(it)
            }
        }
    }
}

fun <R> FileHandler.listMapping(mapper: dirent.() -> R): List<R>? {
    val dir = opendir(path) ?: return null
    try {
        var entry = readdir(dir) ?: return emptyList()
        val entries = mutableListOf<R>()
        while (true) {
            entries += entry.pointed.mapper()
            entry = readdir(dir) ?: return entries
        }
    } finally {
        closedir(dir)
    }
}

fun absolutePath(path: String): String {
    memScoped {
        val buffer = allocArray<ByteVar>(PATH_MAX)
        val absolute = realpath(path, buffer)
            ?: throw AssertionError("Could not find the absolute path.\nRelative: $path\nCode: " + perror("realpath"))
        return absolute.toKString()
    }
}
