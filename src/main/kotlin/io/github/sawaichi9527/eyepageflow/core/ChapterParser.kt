package io.github.sawaichi9527.eyepageflow.core

data class Chapter(
    val title: String,
    val offset: Int,
    val lineNumber: Int,
)

object ChapterParser {
    const val MAX_CHAPTERS = 4096

    val DEFAULT_PATTERN: Regex = Regex(
        """^\s*(?:第[0-9０-９一二三四五六七八九十百千零〇兩两]+[章回節节篇卷].*|""" +
            """序章.*|楔子.*|前言.*|後記.*|后记.*|終章.*|终章.*|尾聲.*|尾声.*)$""",
    )

    fun parse(
        text: String,
        rule: ChapterRule,
        limit: Int = MAX_CHAPTERS,
    ): List<Chapter> = parse(text, rule.pattern, limit)

    fun parse(
        text: String,
        pattern: Regex = DEFAULT_PATTERN,
        limit: Int = MAX_CHAPTERS,
    ): List<Chapter> {
        val chapters = ArrayList<Chapter>()
        var offset = 0
        var lineNumber = 1
        while (offset < text.length && chapters.size < limit) {
            val newline = text.indexOf('\n', offset)
            val end = if (newline < 0) text.length else newline
            val raw = text.substring(offset, end).removeSuffix("\r")
            val title = raw.trim()
            if (title.isNotEmpty() && pattern.matches(title)) {
                chapters += Chapter(title = title, offset = offset, lineNumber = lineNumber)
            }
            offset = if (newline < 0) text.length else newline + 1
            lineNumber += 1
        }
        return chapters
    }
}
