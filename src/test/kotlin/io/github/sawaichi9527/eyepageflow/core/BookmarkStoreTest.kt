package io.github.sawaichi9527.eyepageflow.core

import java.nio.file.Path
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir

class BookmarkStoreTest {
    @TempDir
    lateinit var tempDir: Path

    @Test
    fun `bookmarks round trip`() {
        val file = tempDir.resolve("bookmarks.properties")
        val store = BookmarkStore(file)
        val bookmarks = listOf(
            Bookmark("第一章", tempDir.resolve("novel.txt"), 120),
            Bookmark("第二章", tempDir.resolve("novel.txt"), 4900),
        )
        store.save(bookmarks)
        val loaded = store.load()
        assertEquals(2, loaded.size)
        assertEquals("第一章", loaded[0].title)
        assertEquals(120, loaded[0].offset)
        assertEquals(4900, loaded[1].offset)
    }

    @Test
    fun `missing file yields no bookmarks`() {
        assertTrue(BookmarkStore(tempDir.resolve("missing.properties")).load().isEmpty())
    }
}
