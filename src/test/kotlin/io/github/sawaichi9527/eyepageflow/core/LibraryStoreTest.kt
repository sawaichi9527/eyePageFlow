package io.github.sawaichi9527.eyepageflow.core

import java.nio.file.Files
import java.nio.file.Path
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir

class LibraryStoreTest {
    @TempDir
    lateinit var tempDir: Path

    @Test
    fun `library shelves and default shelf round trip`() {
        val store = LibraryStore(tempDir.resolve("library.properties"))
        val shelfA = tempDir.resolve("a")
        val shelfB = tempDir.resolve("b")
        Files.createDirectories(shelfA)
        Files.createDirectories(shelfB)
        store.save(
            Library(
                shelves = listOf(
                    LibraryShelf("小說", shelfA),
                    LibraryShelf("測試", shelfB),
                ),
                defaultShelfPath = shelfB,
            ),
        )
        val loaded = store.load()
        assertEquals(2, loaded.shelves.size)
        assertEquals("小說", loaded.shelves[0].name)
        assertEquals(shelfB.toAbsolutePath(), loaded.defaultShelfPath?.toAbsolutePath())
    }

    @Test
    fun `missing library is empty`() {
        assertTrue(LibraryStore(tempDir.resolve("missing.properties")).load().shelves.isEmpty())
    }

    @Test
    fun `file list filters supported text extensions`() {
        val directory = tempDir.resolve("books")
        Files.createDirectories(directory)
        Files.writeString(directory.resolve("a.txt"), "a")
        Files.writeString(directory.resolve("b.md"), "b")
        Files.writeString(directory.resolve("c.png"), "c")
        val files = LibraryFiles.listTextFiles(directory).map { it.fileName.toString() }
        assertEquals(listOf("a.txt", "b.md"), files)
    }
}
