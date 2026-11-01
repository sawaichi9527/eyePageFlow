package io.github.sawaichi9527.eyepageflow.core

data class ColorScheme(
    val id: String,
    val displayName: String,
    val pageColor: Int,
    val textColor: Int,
    val gutterColor: Int,
)

object ColorSchemeCatalog {
    val schemes: List<ColorScheme> = listOf(
        ColorScheme(
            id = "scheme.default",
            displayName = "預設",
            pageColor = 0xF3E9D2,
            textColor = 0x2A2622,
            gutterColor = 0x4A3424,
        ),
        ColorScheme(
            id = "scheme.eye-care",
            displayName = "護眼",
            pageColor = 0xE8F0E0,
            textColor = 0x2A332A,
            gutterColor = 0x5A6B52,
        ),
        ColorScheme(
            id = "scheme.night",
            displayName = "夜間",
            pageColor = 0x2A2A2E,
            textColor = 0xD8D8D8,
            gutterColor = 0x1A1A1E,
        ),
        ColorScheme(
            id = "scheme.parchment",
            displayName = "羊皮紙",
            pageColor = 0xF0E0B8,
            textColor = 0x5A3A1A,
            gutterColor = 0x8A6A3A,
        ),
        ColorScheme(
            id = "scheme.high-contrast",
            displayName = "高對比",
            pageColor = 0xFFFFFF,
            textColor = 0x000000,
            gutterColor = 0x000000,
        ),
    )
}
