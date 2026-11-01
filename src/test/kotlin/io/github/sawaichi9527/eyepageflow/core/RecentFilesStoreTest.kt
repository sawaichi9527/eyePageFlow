package io.github.sawaichi9527.eyepageflow.core

import java.nio.file.Path
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir

class RecentFilesStoreTest {
    @TempDir
    lateinit var tempDir: Path

    @Test
    fun `recent files move to front and deduplicate`() {
        val store = RecentFilesStore(tempDir.resolve("recent.properties"), maxEntries = 3)
        val a = tempDir.resolve("a.txt")
        val b = tempDir.resolve("b.txt")
        val c = tempDir.resolve("c.txt")
        store.add(a)
        store.add(b)
        store.add(a)
        assertEquals(listOf(a.toAbsolutePath(), b.toAbsolutePath()), store.load())

        store.add(c)
        store.add(tempDir.resolve("d.txt"))
        assertEquals(3, store.load().size)
        assertTrue(store.load().first().endsWith("d.txt"))
    }

    @Test
    fun `missing file yields an empty list`() {
        assertTrue(RecentFilesStore(tempDir.resolve("missing.properties")).load().isEmpty())
    }
}
