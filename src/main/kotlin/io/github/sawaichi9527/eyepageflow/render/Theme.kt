package io.github.sawaichi9527.eyepageflow.render

import io.github.sawaichi9527.eyepageflow.core.DisplaySettings
import java.awt.Color

enum class TextureKind {
    NONE,
    PAPER,
    LEATHER,
    WOOD,
    LINEN,
}

data class Theme(
    val id: String,
    val displayName: String,
    val desktopColor: Color,
    val desktopTexture: TextureKind,
    val coverColor: Color,
    val coverTexture: TextureKind,
    val pageColor: Color,
    val pageTexture: TextureKind,
    val textColor: Color,
    val underlineColor: Color,
    val gutterColor: Color,
    val shadowColor: Color,
    val pageNumberColor: Color,
) {
    companion object {
        val REALISTIC = Theme(
            id = "realistic",
            displayName = "仿真紙書",
            desktopColor = Color(0x2B211B),
            desktopTexture = TextureKind.WOOD,
            coverColor = Color(0x6B4A2F),
            coverTexture = TextureKind.LEATHER,
            pageColor = Color(0xF3E9D2),
            pageTexture = TextureKind.PAPER,
            textColor = Color(0x2A2622),
            underlineColor = Color(0x2A2622),
            gutterColor = Color(0x4A3424),
            shadowColor = Color(0x000000),
            pageNumberColor = Color(0x5A4A3A),
        )

        val MODERN = Theme(
            id = "modern",
            displayName = "現代簡約",
            desktopColor = Color(0x1E1E22),
            desktopTexture = TextureKind.NONE,
            coverColor = Color(0x3A3F46),
            coverTexture = TextureKind.NONE,
            pageColor = Color(0xFAFAF8),
            pageTexture = TextureKind.PAPER,
            textColor = Color(0x222222),
            underlineColor = Color(0x222222),
            gutterColor = Color(0xD8D8D4),
            shadowColor = Color(0x000000),
            pageNumberColor = Color(0x777777),
        )

        val DEFAULT: Theme = REALISTIC

        fun byId(id: String): Theme = when (id) {
            MODERN.id -> MODERN
            else -> REALISTIC
        }

        fun fromSettings(settings: DisplaySettings): Theme {
            val desktop = PatternCatalog.desktopById(settings.desktopPatternId)
            val cover = PatternCatalog.coverById(settings.coverPatternId)
            val page = PatternCatalog.pageById(settings.pagePatternId)
            return byId(settings.themeId).copy(
                desktopColor = Color(settings.desktopColor),
                desktopTexture = desktop.texture,
                coverColor = Color(settings.coverColor),
                coverTexture = cover.texture,
                pageColor = Color(settings.pageColor),
                pageTexture = page.texture,
                textColor = Color(settings.textColor),
                underlineColor = Color(settings.underlineColor),
                gutterColor = Color(settings.gutterColor),
                pageNumberColor = Color(settings.textColor).darker(),
            )
        }
    }
}
