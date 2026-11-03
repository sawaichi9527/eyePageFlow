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
    val underlineWidth: Float = 1f,
    val punctuationPlacement: VerticalPunctuation = VerticalPunctuation.CENTER,
    val justify: Boolean = true,
    val animation: PageTurnAnimation = PageTurnAnimation.NONE,
    val desktopPatternId: String = "desktop.wood-dark",
    val coverPatternId: String = "cover.leather-brown",
    val pagePatternId: String = "page.paper-cream",
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
}
