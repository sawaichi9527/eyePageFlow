package io.github.sawaichi9527.eyepageflow.core

import java.nio.file.Files
import java.nio.file.Path
import java.util.Properties

/**
 * 記住每個檔案上次讀到哪裡（以字元 offset 表示）。
 *
 * 用 offset 而不是頁碼，因為換字級、換字型或切直排／橫排都會重新分頁，
 * 頁碼會漂移，offset 不會。呼叫端再用 `Pagination.pageIndexFor` 換算回頁。
 *
 * 以「最近使用」排序並限制筆數，避免長期使用後檔案無限長大。
 */
class ReadingPositionStore(
    private val file: Path,
    private val maxEntries: Int = 200,
) {
    private data class Entry(val path: String, val offset: Int)

    fun get(path: Path): Int? = load().firstOrNull { it.path == key(path) }?.offset

    fun put(path: Path, offset: Int) {
        val normalized = key(path)
        val updated = (listOf(Entry(normalized, offset)) + load().filter { it.path != normalized })
            .take(maxEntries)
        save(updated)
    }

    fun remove(path: Path) {
        val normalized = key(path)
        val remaining = load().filter { it.path != normalized }
        save(remaining)
    }

    private fun key(path: Path): String = path.toAbsolutePath().normalize().toString()

    private fun load(): List<Entry> {
        if (!Files.exists(file)) return emptyList()
        val properties = Properties()
        try {
            Files.newInputStream(file).use { properties.load(it) }
        } catch (_: Exception) {
            return emptyList()
        }
        val entries = ArrayList<Entry>()
        var index = 0
        while (true) {
            val path = properties.getProperty("position.$index.path") ?: break
            val offset = properties.getProperty("position.$index.offset")?.toIntOrNull()
            if (offset != null && offset >= 0) {
                entries.add(Entry(path, offset))
            }
            index += 1
        }
        return entries
    }

    private fun save(entries: List<Entry>) {
        val properties = Properties()
        entries.forEachIndexed { index, entry ->
            properties["position.$index.path"] = entry.path
            properties["position.$index.offset"] = entry.offset.toString()
        }
        file.parent?.let { Files.createDirectories(it) }
        Files.newOutputStream(file).use { properties.store(it, "eyePageFlow reading positions") }
    }
}
