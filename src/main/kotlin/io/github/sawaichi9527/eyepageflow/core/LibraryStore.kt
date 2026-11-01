package io.github.sawaichi9527.eyepageflow.core

import java.nio.file.Files
import java.nio.file.Path
import java.util.Properties

data class LibraryShelf(
    val name: String,
    val path: Path,
)

data class Library(
    val shelves: List<LibraryShelf> = emptyList(),
    val defaultShelfPath: Path? = null,
)

class LibraryStore(private val file: Path) {
    fun load(): Library {
        if (!Files.exists(file)) return Library()
        val properties = Properties()
        try {
            Files.newInputStream(file).use { properties.load(it) }
        } catch (_: Exception) {
            return Library()
        }
        val shelves = ArrayList<LibraryShelf>()
        var index = 0
        while (true) {
            val value = properties.getProperty("shelf.$index.path") ?: break
            val name = properties.getProperty("shelf.$index.name")
                ?: Path.of(value).fileName?.toString()
                ?: "書架"
            try {
                shelves += LibraryShelf(name, Path.of(value))
            } catch (_: Exception) {
                // Skip malformed entries.
            }
            index += 1
        }
        val default = properties.getProperty("shelf.default")?.let {
            try {
                Path.of(it)
            } catch (_: Exception) {
                null
            }
        }
        return Library(shelves, default)
    }

    fun save(library: Library) {
        val properties = Properties()
        library.shelves.forEachIndexed { index, shelf ->
            properties["shelf.$index.name"] = shelf.name
            properties["shelf.$index.path"] = shelf.path.toAbsolutePath().normalize().toString()
        }
        library.defaultShelfPath?.let {
            properties["shelf.default"] = it.toAbsolutePath().normalize().toString()
        }
        file.parent?.let { Files.createDirectories(it) }
        Files.newOutputStream(file).use { properties.store(it, "eyePageFlow library") }
    }
}

object LibraryFiles {
    val TEXT_EXTENSIONS: Set<String> = setOf("txt", "text", "log", "md")

    fun listTextFiles(directory: Path): List<Path> {
        if (!Files.isDirectory(directory)) return emptyList()
        return try {
            Files.list(directory).use { stream ->
                stream.filter { Files.isRegularFile(it) }
                    .filter { path ->
                        val name = path.fileName?.toString().orEmpty()
                        val extension = name.substringAfterLast('.', "").lowercase()
                        extension in TEXT_EXTENSIONS
                    }
                    .sorted()
                    .toList()
            }
        } catch (_: Exception) {
            emptyList()
        }
    }
}
