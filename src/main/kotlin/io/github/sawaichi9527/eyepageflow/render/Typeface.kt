package io.github.sawaichi9527.eyepageflow.render

import io.github.sawaichi9527.eyepageflow.core.PageMetrics
import java.awt.Font
import java.awt.GraphicsEnvironment

object Typeface {
    const val DEFAULT_FAMILY = "Noto Serif CJK TC"

    /**
     * 指定字型不存在時的替代順序：優先挑能顯示 CJK 的字型，
     * 避免落到拉丁字型後再由 Java 逐字 fallback（字重與字寬都會不一致）。
     */
    private val FALLBACK_FAMILIES = listOf(
        "Noto Serif CJK TC",
        "Noto Serif TC",
        "PMingLiU",
        "新細明體",
        "MingLiU",
        "細明體",
        "Noto Sans TC",
        "Microsoft JhengHei UI",
        "Microsoft JhengHei",
    )

    fun resolveFamily(preferred: String = DEFAULT_FAMILY): String {
        val families = GraphicsEnvironment.getLocalGraphicsEnvironment()
            .availableFontFamilyNames
            .toSet()
        if (preferred in families) {
            return preferred
        }
        return FALLBACK_FAMILIES.firstOrNull { it in families } ?: Font.SERIF
    }

    fun pageFont(metrics: PageMetrics, family: String = DEFAULT_FAMILY): Font = Font(
        resolveFamily(family),
        if (metrics.bold) Font.BOLD else Font.PLAIN,
        metrics.fontSize.toInt(),
    )

    /**
     * 頁面字型缺少某個字（例如 emoji）時，找一個能顯示它的系統字型。
     * 找不到就回傳 null，由呼叫端沿用原本的字型。
     */
    private val symbolCache = HashMap<Long, Font?>()

    private val SYMBOL_FAMILIES = listOf(
        "Segoe UI Emoji",
        "Segoe UI Symbol",
        "Noto Color Emoji",
        "Noto Emoji",
        "Apple Color Emoji",
    )

    fun symbolFont(codePoint: Int, size: Int): Font? {
        val key = (codePoint.toLong() shl 32) or (size.toLong() and 0xFFFFFFFFL)
        return symbolCache.getOrPut(key) {
            val families = GraphicsEnvironment.getLocalGraphicsEnvironment()
                .availableFontFamilyNames
                .toSet()
            SYMBOL_FAMILIES.firstOrNull { it in families }
                ?.let { Font(it, Font.PLAIN, size) }
                ?.takeIf { it.canDisplay(codePoint) }
        }
    }
}
