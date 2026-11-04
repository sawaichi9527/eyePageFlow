package io.github.sawaichi9527.eyepageflow.render

import io.github.sawaichi9527.eyepageflow.core.PageMetrics
import java.awt.Font
import java.awt.GraphicsEnvironment

object Typeface {
    const val DEFAULT_FAMILY = "Noto Serif CJK TC"

    private const val EMBEDDED_SERIF_RESOURCE = "/fonts/NotoSerifCJKtc-Regular.otf"

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

    /** UI 用的 sans CJK 家族；只列免費、跨平台安裝率高又不改變 Windows 預設外觀的字型。 */
    private val UI_FAMILIES = listOf(
        "Noto Sans CJK TC",
        "Noto Sans TC",
    )

    fun resolveFamily(preferred: String = DEFAULT_FAMILY): String {
        val families = availableFamilies()
        if (preferred in families) {
            return preferred
        }
        return FALLBACK_FAMILIES.firstOrNull { it in families } ?: Font.SERIF
    }

    /**
     * UI（選單、工具列、狀態列）文字用的字型家族。
     *
     * 系統字型若無法顯示中文，15px 的中文會落到不合適的 fallback，看起來又硬又擠；
     * 這裡在允許範圍內改用 CJK sans 家族。只列免費且跨平台安裝率高的字型，
     * 找不到就回傳 null，沿用系統預設（例如 Windows 的 Segoe UI）。
     *
     * 注意：不能改用 `Font(name).canDisplay('檔')` 判斷系統字型，因為 Java 會把未知
     * 家族對應到 Dialog，而 Dialog 的複合字型永遠回報可顯示中文。
     */
    fun resolveUiFamily(): String? = UI_FAMILIES.firstOrNull { it in availableFamilies() }

    private fun availableFamilies(): Set<String> = GraphicsEnvironment.getLocalGraphicsEnvironment()
        .availableFontFamilyNames
        .toSet()

    /**
     * 內嵌的 Noto Serif CJK TC（OFL）。有這個檔案時，預設字型不再依賴系統安裝，
     * Windows 與 Linux 的頁數、字型才會一致。
     */
    private val embeddedFont: Font? by lazy { loadEmbeddedFont() }

    /** 內嵌字型是否載入成功（診斷與測試用）。 */
    val hasEmbeddedFont: Boolean get() = embeddedFont != null

    private fun loadEmbeddedFont(): Font? = try {
        Typeface::class.java.getResourceAsStream(EMBEDDED_SERIF_RESOURCE)?.use { stream ->
            Font.createFont(Font.TRUETYPE_FONT, stream).also {
                GraphicsEnvironment.getLocalGraphicsEnvironment().registerFont(it)
            }
        }
    } catch (_: Throwable) {
        null
    }

    fun pageFont(metrics: PageMetrics, family: String = DEFAULT_FAMILY): Font {
        val style = if (metrics.bold) Font.BOLD else Font.PLAIN
        val embedded = embeddedFont
        if (embedded != null && family == DEFAULT_FAMILY) {
            return embedded.deriveFont(style, metrics.fontSize)
        }
        return Font(resolveFamily(family), style, metrics.fontSize.toInt())
    }

    /**
     * 圖示標記用的字型：優先使用內嵌 CJK 字型，沒有時退回系統字型。
     * 不用 `Font.deriveFont(BOLD)`：內嵌的是 Regular，假粗體會讓小尺寸糊掉。
     */
    fun iconFont(size: Float): Font {
        embeddedFont?.let { return it.deriveFont(size) }
        return Font(resolveFamily(), Font.PLAIN, size.toInt().coerceAtLeast(1))
    }

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
