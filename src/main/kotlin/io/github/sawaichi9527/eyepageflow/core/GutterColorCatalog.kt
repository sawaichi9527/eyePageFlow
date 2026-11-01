package io.github.sawaichi9527.eyepageflow.core

data class GutterColorPreset(
    val id: String,
    val displayName: String,
    val color: Int,
)

object GutterColorCatalog {
    val presets: List<GutterColorPreset> = listOf(
        GutterColorPreset("gutter.dark-brown", "深棕", 0x4A3424),
        GutterColorPreset("gutter.light-gray", "淺灰", 0xC8C8C8),
        GutterColorPreset("gutter.beige", "米色", 0xD8C8A8),
        GutterColorPreset("gutter.matcha", "抹茶", 0x5A6B52),
        GutterColorPreset("gutter.night", "深黑", 0x1A1A1E),
    )
}
