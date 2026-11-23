package io.github.sawaichi9527.eyepageflow.core

/**
 * 閱讀歷程：`Ctrl+←`／`Ctrl+→` 在造訪過的頁面之間前後移動。
 *
 * 只記頁碼，不記文字 offset；換字級、切直排／橫排都會重新分頁，
 * 這時呼叫端應該 `clear()`，避免回到一個已經不存在的頁。
 */
class NavigationHistory(private val limit: Int = 100) {
    private val back = ArrayDeque<Int>()
    private val forward = ArrayDeque<Int>()

    val canGoBack: Boolean get() = back.isNotEmpty()

    val canGoForward: Boolean get() = forward.isNotEmpty()

    /** 記錄一次由 [from] 前往 [to] 的移動；同一頁不算。 */
    fun record(from: Int, to: Int) {
        if (from == to) return
        back.addLast(from)
        while (back.size > limit) {
            back.removeFirst()
        }
        forward.clear()
    }

    /** 回上一頁；回傳目標頁碼，沒有歷程時回傳 null。 */
    fun back(current: Int): Int? {
        val target = back.removeLastOrNull() ?: return null
        forward.addLast(current)
        return target
    }

    /** 前進到下一筆歷程；回傳目標頁碼，沒有歷程時回傳 null。 */
    fun forward(current: Int): Int? {
        val target = forward.removeLastOrNull() ?: return null
        back.addLast(current)
        return target
    }

    fun clear() {
        back.clear()
        forward.clear()
    }
}
