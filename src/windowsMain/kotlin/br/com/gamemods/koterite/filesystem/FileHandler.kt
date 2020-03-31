package br.com.gamemods.koterite.filesystem

import br.com.gamemods.koterite.annotation.InternalApi
import kotlinx.cinterop.CValues
import kotlinx.cinterop.wcstr
import platform.windows.WCHARVar

@InternalApi
actual open class FileHandler internal constructor(path: String) {
    val path: String
    val pathW: CValues<WCHARVar>
    init {
        check('\u0000' !in path) { "Path may not contain null chars" }
        val normalized = when {
            path.isEmpty() -> "."
            path == "/" -> path
            else -> path.trimEnd('/', '\\').takeIf { it.isNotEmpty() } ?: "/"
        }.toCharArray()

        normalized.forEachIndexed { index, c ->
            if (c == '\\') {
                normalized[index] = '/'
            }
        }
        this.path = String(normalized)

        normalized.forEachIndexed { index, c ->
            if (c == '/') {
                normalized[index] = '\\'
            }
        }
        pathW = String(normalized).wcstr
    }

    operator fun component1() = path
    operator fun component2() = pathW

    override fun toString() = path
}
