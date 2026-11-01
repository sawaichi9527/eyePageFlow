package io.github.sawaichi9527.eyepageflow.core

import java.nio.file.Files
import java.nio.file.Path
import java.util.Properties

data class Bookmark(
    val title: String,
    val path: Path,
    val offset: Int,
)

class BookmarkStore(private val file: Path) {
    fun load(): List<Bookmark> {
        if (!Files.exists(file)) return emptyList()
        val properties = Properties()
        try {
            Files.newInputStream(file).use { properties.load(it) }
        } catch (_: Exception) {
            return emptyList()
        }
        val bookmarks = ArrayList<Bookmark>()
        var index = 0
        while (true) {
            val pathValue = properties.getProperty("bookmark.$index.path") ?: break
            val title = properties.getProperty("bookmark.$index.title")
                ?: "書籤 ${index + 1}"
            val offset = properties.getProperty("bookmark.$index.offset")
                ?.toIntOrNull()
                ?: 0
            try {
                bookmarks += Bookmark(
                    title = title,
                    path = Path.of(pathValue),
                    offset = offset.coerceAtLeast(0),
                )
            } catch (_: Exception) {
                // Skip malformed entries.
            }
            index += 1
        }
        return bookmarks
    }

    fun save(bookmarks: List<Bookmark>) {
        val properties = Properties()
        bookmarks.forEachIndexed { index, bookmark ->
            properties["bookmark.$index.title"] = bookmark.title
            properties["bookmark.$index.path"] = bookmark.path.toAbsolutePath().normalize().toString()
            properties["bookmark.$index.offset"] = bookmark.offset.toString()
        }
        file.parent?.let { Files.createDirectories(it) }
        Files.newOutputStream(file).use { properties.store(it, "eyePageFlow bookmarks") }
    }
}
