package br.com.gamemods.koterite.filesystem

import br.com.gamemods.koterite.annotation.InternalApi
import io.ktor.utils.io.errors.IOException
import kotlinx.cinterop.*
import platform.posix.*
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract

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

@InternalApi
inline fun <R> File.stat(operation: stat.() -> R): R {
    contract { callsInPlace(operation, InvocationKind.EXACTLY_ONCE) }
    return memScoped {
        val stat = alloc<stat>()
        stat(handler.path, stat.ptr)
        operation(stat)
    }
}

@Suppress("NOTHING_TO_INLINE")
@InternalApi
inline fun stat.hasFlag(flag: Int) = (st_mode.convert<Int>() and flag) == flag

@InternalApi
internal val filesPendingForRemoval = mutableSetOf<String>()

@InternalApi
internal fun deletePendingFiles() {
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

@InternalApi
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

@InternalApi
fun absolutePath(path: String): String {
    memScoped {
        val buffer = allocArray<ByteVar>(PATH_MAX)
        val absolute = realpath(path, buffer)
            ?: throw AssertionError("Could not find the absolute path.\nRelative: $path\nCode: " + perror("realpath"))
        return absolute.toKString()
    }
}
