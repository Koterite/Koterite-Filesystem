package br.com.gamemods.koterite.filesystem

actual inline class File actual constructor(actual val handler: FileHandler) : Comparable<File> {
    actual constructor(path: String) : this(FileHandler(path))
    actual constructor(parent: String?, child: String) : this(FileHandler(parent, child))
    actual constructor(parent: File?, child: String) : this(FileHandler(parent?.handler, child))

    actual fun list() = handler.list()?.toList()
    actual fun listFiles(): List<File>? = handler.listFiles()?.map(::File)

    actual fun canRead() = handler.canRead()
    actual fun canWrite() = handler.canWrite()
    actual fun exists() = handler.exists()
    actual fun createNewFile() = handler.createNewFile()
    actual fun delete() = handler.delete()
    actual fun deleteOnExit() = handler.deleteOnExit()
    actual fun mkdir() = handler.mkdir()
    actual fun mkdirs() = handler.mkdirs()
    actual fun renameTo(dest: File) = handler.renameTo(dest.handler)
    actual fun setLastModified(time: Long) = handler.setLastModified(time)
    actual fun setReadOnly() = handler.setReadOnly()
    actual fun setWritable(writable: Boolean, ownerOnly: Boolean) = handler.setWritable(writable, ownerOnly)
    actual fun setReadable(readable: Boolean, ownerOnly: Boolean) = handler.setReadable(readable, ownerOnly)
    actual fun setExecutable(executable: Boolean, ownerOnly: Boolean) = handler.setExecutable(executable, ownerOnly)
    actual fun canExecute() = handler.canExecute()
    actual override fun toString() = handler.toString()
    override fun compareTo(other: File) = handler.compareTo(other.handler)
}
