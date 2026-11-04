package io.github.sawaichi9527.eyepageflow.core

enum class LayoutMode(val label: String) {
    AUTO("自動單雙頁"),
    SINGLE("單頁"),
    DOUBLE("雙頁"),
    EQUAL_HEIGHT("等高對稱"),
}

enum class PageTurnAnimation(val label: String) {
    NONE("無"),
    FADE("淡入"),
    SLIDE("平移"),
    CURL("捲頁"),
}

data class DisplaySettings(
    val themeId: String = "realistic",
    val layoutMode: LayoutMode = LayoutMode.AUTO,
    val showBookAndDesktop: Boolean = true,
    val pageWidth: Int = 900,
    val fontFamily: String = "Noto Serif CJK TC",
    val fontSize: Float = 26f,
    val lineHeightFactor: Float = 1.44f,
    val paragraphIndentEm: Float = 2f,
    val bold: Boolean = false,
    val underline: Boolean = false,
    val underlineColor: Int = 0x2A2622,
    val underlineWidth: Float = 0.5f,
    val punctuationPlacement: VerticalPunctuation = VerticalPunctuation.CENTER,
    val justify: Boolean = true,
    val animation: PageTurnAnimation = PageTurnAnimation.NONE,
    val desktopPatternId: String = "desktop.wood-dark",
    val coverPatternId: String = "cover.leather-brown",
    val pagePatternId: String = "page.paper-cream",
    /** 自訂圖片路徑；空字串表示使用圖案／純色。 */
    val desktopImage: String = "",
    val coverImage: String = "",
    val pageImage: String = "",
    val desktopColor: Int = 0x2B211B,
    val coverColor: Int = 0x6B4A2F,
    val pageColor: Int = 0xF3E9D2,
    val textColor: Int = 0x2A2622,
    val gutterColor: Int = 0x4A3424,
) {
    fun toPageMetrics(): PageMetrics = PageMetrics(
        pageWidth = pageWidth,
        fontSize = fontSize,
        lineHeightFactor = lineHeightFactor,
        paragraphIndentEm = paragraphIndentEm,
        bold = bold,
        underline = underline,
        underlineWidth = underlineWidth,
        punctuationPlacement = punctuationPlacement,
        justify = justify,
    )

    fun usesDoublePage(): Boolean = layoutMode != LayoutMode.SINGLE

    /**
     * AUTO 依畫布比例決定單／雙頁：視窗夠寬才排雙頁，否則單頁避免字變太小。
     * 其他模式固定，不看畫布大小。
     */
    fun doublePageFor(width: Int, height: Int): Boolean = when (layoutMode) {
        LayoutMode.SINGLE -> false
        LayoutMode.DOUBLE, LayoutMode.EQUAL_HEIGHT -> true
        LayoutMode.AUTO -> width >= height * AUTO_DOUBLE_RATIO
    }

    companion object {
        /** AUTO 模式下，畫布寬高比超過這個值才排雙頁。 */
        const val AUTO_DOUBLE_RATIO = 1.25f
    }
}
