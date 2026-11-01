package io.github.sawaichi9527.eyepageflow.render

data class PatternOption(
    val id: String,
    val displayName: String,
    val texture: TextureKind,
    val defaultColor: Int,
)

object PatternCatalog {
    val desktop: List<PatternOption> = listOf(
        PatternOption("desktop.solid", "純色", TextureKind.NONE, 0x2B211B),
        PatternOption("desktop.wood-dark", "深木紋", TextureKind.WOOD, 0x2B211B),
        PatternOption("desktop.wood-light", "淺木紋", TextureKind.WOOD, 0x6B5138),
        PatternOption("desktop.linen", "亞麻布", TextureKind.LINEN, 0x3A3A3E),
    )

    val cover: List<PatternOption> = listOf(
        PatternOption("cover.solid", "純色", TextureKind.NONE, 0x6B4A2F),
        PatternOption("cover.leather-brown", "棕皮革", TextureKind.LEATHER, 0x6B4A2F),
        PatternOption("cover.leather-blue", "藍布面", TextureKind.LEATHER, 0x2F4A6B),
        PatternOption("cover.paper", "米紙", TextureKind.PAPER, 0xC9B79A),
    )

    val page: List<PatternOption> = listOf(
        PatternOption("page.solid", "純色", TextureKind.NONE, 0xF3E9D2),
        PatternOption("page.paper-cream", "米紙", TextureKind.PAPER, 0xF3E9D2),
        PatternOption("page.paper-white", "白紙", TextureKind.PAPER, 0xFBFBF8),
        PatternOption("page.paper-antique", "仿古紙", TextureKind.PAPER, 0xE8D8B0),
    )

    fun desktopById(id: String): PatternOption =
        desktop.firstOrNull { it.id == id } ?: desktop[1]

    fun coverById(id: String): PatternOption =
        cover.firstOrNull { it.id == id } ?: cover[1]

    fun pageById(id: String): PatternOption =
        page.firstOrNull { it.id == id } ?: page[1]
}
