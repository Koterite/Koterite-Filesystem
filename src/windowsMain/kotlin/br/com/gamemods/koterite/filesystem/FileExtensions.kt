package br.com.gamemods.koterite.filesystem

import br.com.gamemods.koterite.annotation.InternalApi
import io.ktor.utils.io.errors.IOException
import kotlinx.cinterop.*
import platform.posix.*
import platform.windows.*
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract

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

@InternalApi
inline fun <R> File.statW(operation: _stat64.() -> R): R {
    contract { callsInPlace(operation, InvocationKind.EXACTLY_ONCE) }
    return memScoped {
        val stat = alloc<_stat64>()
        _wstat64(handler.pathW, stat.ptr)
        operation(stat)
    }
}

@Suppress("NOTHING_TO_INLINE")
@InternalApi
inline fun _stat64.hasFlag(flag: Int) = (st_mode.convert<Int>() and flag) == flag

@InternalApi
internal val filesPendingForRemoval = mutableSetOf<String>()

@InternalApi
internal fun deletePendingFiles() {
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

@InternalApi
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

@InternalApi
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
