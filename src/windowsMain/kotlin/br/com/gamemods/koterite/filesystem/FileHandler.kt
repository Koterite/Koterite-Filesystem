package br.com.gamemods.koterite.filesystem

actual open class FileHandler internal constructor(val path: String) {
    operator fun component1() = path
    override fun toString() = path
}
