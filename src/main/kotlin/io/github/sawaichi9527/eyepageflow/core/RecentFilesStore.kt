package io.github.sawaichi9527.eyepageflow.core

import java.nio.file.Files
import java.nio.file.Path
import java.util.Properties

class RecentFilesStore(
    private val file: Path,
    private val maxEntries: Int = 10,
) {
    fun load(): List<Path> {
        if (!Files.exists(file)) return emptyList()
        val properties = Properties()
        try {
            Files.newInputStream(file).use { properties.load(it) }
        } catch (_: Exception) {
            return emptyList()
        }
        val paths = ArrayList<Path>()
        var index = 0
        while (true) {
            val value = properties.getProperty("recent.$index.path") ?: break
            try {
                paths.add(Path.of(value))
            } catch (_: Exception) {
                // Skip malformed entries.
            }
            index += 1
        }
        return paths.distinct()
    }

    fun add(path: Path): List<Path> {
        val normalized = path.toAbsolutePath().normalize()
        val updated = (listOf(normalized) + load().filter { it != normalized })
            .take(maxEntries)
        save(updated)
        return updated
    }

    private fun save(paths: List<Path>) {
        val properties = Properties()
        paths.forEachIndexed { index, path ->
            properties["recent.$index.path"] = path.toAbsolutePath().normalize().toString()
        }
        file.parent?.let { Files.createDirectories(it) }
        Files.newOutputStream(file).use { properties.store(it, "eyePageFlow recent files") }
    }
}
