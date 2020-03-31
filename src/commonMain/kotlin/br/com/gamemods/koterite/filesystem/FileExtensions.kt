package br.com.gamemods.koterite.filesystem

import br.com.gamemods.koterite.annotation.JvmThrows
import io.ktor.utils.io.errors.IOException


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
