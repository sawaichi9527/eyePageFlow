package io.github.sawaichi9527.eyepageflow.core

import java.nio.file.Files
import java.nio.file.Path
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir

class ReadingPositionStoreTest {
    @TempDir
    lateinit var tempDir: Path

    private fun store(maxEntries: Int = 200): ReadingPositionStore =
        ReadingPositionStore(tempDir.resolve("reading-positions.properties"), maxEntries)

    @Test
    fun `missing file has no positions`() {
        assertNull(store().get(tempDir.resolve("novel.txt")))
    }

    @Test
    fun `a position survives a reload`() {
        val target = tempDir.resolve("novel.txt")
        store().put(target, 12345)

        val reopened = store()
        assertEquals(12345, reopened.get(target))
    }

    @Test
    fun `paths are compared after normalising`() {
        val target = tempDir.resolve("novel.txt")
        store().put(target, 42)
        val roundabout = tempDir.resolve(".").resolve("novel.txt")
        assertEquals(42, store().get(roundabout))
    }

    @Test
    fun `putting the same file twice keeps one entry and the newer offset`() {
        val target = tempDir.resolve("novel.txt")
        val store = store()
        store.put(target, 100)
        store.put(target, 900)

        assertEquals(900, store.get(target))
        val contents = Files.readString(tempDir.resolve("reading-positions.properties"))
        assertEquals(1, contents.lines().count { it.startsWith("position.") && it.contains(".path=") })
    }

    @Test
    fun `old entries fall off the end`() {
        val store = store(maxEntries = 2)
        val first = tempDir.resolve("a.txt")
        val second = tempDir.resolve("b.txt")
        val third = tempDir.resolve("c.txt")

        store.put(first, 1)
        store.put(second, 2)
        store.put(third, 3)

        assertNull(store.get(first), "最舊的一筆應該被擠掉")
        assertEquals(2, store.get(second))
        assertEquals(3, store.get(third))
    }

    @Test
    fun `remove forgets a file`() {
        val target = tempDir.resolve("novel.txt")
        val store = store()
        store.put(target, 7)
        store.remove(target)
        assertNull(store.get(target))
    }
}
