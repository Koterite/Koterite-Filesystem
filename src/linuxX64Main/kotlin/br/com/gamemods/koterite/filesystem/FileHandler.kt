package br.com.gamemods.koterite.filesystem

actual open class FileHandler internal constructor(val path: String) {
    init {
        check('\u0000' !in path) { "Path may not contain null chars" }
    }
    operator fun component1() = path
    override fun toString() = path
}
