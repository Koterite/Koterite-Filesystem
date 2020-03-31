package br.com.gamemods.koterite.filesystem

import br.com.gamemods.koterite.annotation.InternalApi

@InternalApi
actual open class FileHandler internal constructor(path: String) {
    init {
        check('\u0000' !in path) { "Path may not contain null chars" }
    }

    val path = when {
        path.isEmpty() -> "."
        path == "/" -> path
        else -> path.trimEnd('/').takeIf { it.isNotEmpty() } ?: "/"
    }
    
    operator fun component1() = path
    override fun toString() = path
}
