package io.github.sawaichi9527.eyepageflow.core

data class ChapterRule(
    val id: String,
    val displayName: String,
    val pattern: Regex,
)

object ChapterRuleCatalog {
    val DEFAULT: ChapterRule = ChapterRule(
        id = "default",
        displayName = "預設（繁中／簡中）",
        pattern = ChapterParser.DEFAULT_PATTERN,
    )

    val SIMPLIFIED: ChapterRule = ChapterRule(
        id = "simplified",
        displayName = "簡中數字章節",
        pattern = Regex(
            """^\s*(?:第[0-9０-９一二三四五六七八九十百千零〇两]+[章回节篇卷].*|""" +
                """序章.*|楔子.*|前言.*|后记.*|终章.*|尾声.*)$""",
        ),
    )

    val NUMBERED: ChapterRule = ChapterRule(
        id = "numbered",
        displayName = "純數字章節",
        pattern = Regex("""^\s*第[0-9０-９]+[章回節节篇卷].*$"""),
    )

    val presets: List<ChapterRule> = listOf(DEFAULT, SIMPLIFIED, NUMBERED)

    fun custom(pattern: String): ChapterRule? = try {
        ChapterRule(id = "custom", displayName = "自訂", pattern = Regex(pattern))
    } catch (_: Exception) {
        null
    }
}
