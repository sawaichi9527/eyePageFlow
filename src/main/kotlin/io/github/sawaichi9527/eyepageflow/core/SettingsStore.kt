package io.github.sawaichi9527.eyepageflow.core

import java.nio.file.Files
import java.nio.file.Path
import java.util.Properties

data class AppSettings(
    val display: DisplaySettings,
    val chapterRule: ChapterRule,
)

class SettingsStore(private val file: Path) {
    fun load(
        defaultDisplay: DisplaySettings = DisplaySettings(),
        defaultRule: ChapterRule = ChapterRuleCatalog.DEFAULT,
    ): AppSettings {
        if (!Files.exists(file)) {
            return AppSettings(defaultDisplay, defaultRule)
        }
        val properties = Properties()
        try {
            Files.newInputStream(file).use { properties.load(it) }
        } catch (_: Exception) {
            return AppSettings(defaultDisplay, defaultRule)
        }

        // 舊版設定檔的字號與行距是舊基準；升版後改回新預設，其餘設定保留。
        val typographyVersion = int(properties.getProperty(KEY_TYPOGRAPHY_VERSION), 1)
        val typographyIsCurrent = typographyVersion >= CURRENT_TYPOGRAPHY_VERSION

        val display = defaultDisplay.copy(
            themeId = properties.getProperty(KEY_THEME, defaultDisplay.themeId),
            layoutMode = enumValue(
                properties.getProperty(KEY_LAYOUT),
                defaultDisplay.layoutMode,
            ),
            showBookAndDesktop = bool(
                properties.getProperty(KEY_SHOW_BOOK),
                defaultDisplay.showBookAndDesktop,
            ),
            pageWidth = int(properties.getProperty(KEY_PAGE_WIDTH), defaultDisplay.pageWidth),
            fontFamily = properties.getProperty(KEY_FONT_FAMILY, defaultDisplay.fontFamily),
            fontSize = if (typographyIsCurrent) {
                float(properties.getProperty(KEY_FONT_SIZE), defaultDisplay.fontSize)
            } else {
                defaultDisplay.fontSize
            },
            lineHeightFactor = if (typographyIsCurrent) {
                float(properties.getProperty(KEY_LINE_HEIGHT), defaultDisplay.lineHeightFactor)
            } else {
                defaultDisplay.lineHeightFactor
            },
            paragraphIndentEm = float(
                properties.getProperty(KEY_INDENT),
                defaultDisplay.paragraphIndentEm,
            ),
            bold = bool(properties.getProperty(KEY_BOLD), defaultDisplay.bold),
            underline = bool(properties.getProperty(KEY_UNDERLINE), defaultDisplay.underline),
            underlineColor = color(
                properties.getProperty(KEY_UNDERLINE_COLOR),
                defaultDisplay.underlineColor,
            ),
            underlineWidth = float(
                properties.getProperty(KEY_UNDERLINE_WIDTH),
                defaultDisplay.underlineWidth,
            ),
            punctuationPlacement = enumValue(
                properties.getProperty(KEY_PUNCTUATION),
                defaultDisplay.punctuationPlacement,
            ),
            animation = enumValue(
                properties.getProperty(KEY_ANIMATION),
                defaultDisplay.animation,
            ),
            desktopPatternId = properties.getProperty(
                KEY_DESKTOP_PATTERN,
                defaultDisplay.desktopPatternId,
            ),
            coverPatternId = properties.getProperty(
                KEY_COVER_PATTERN,
                defaultDisplay.coverPatternId,
            ),
            pagePatternId = properties.getProperty(
                KEY_PAGE_PATTERN,
                defaultDisplay.pagePatternId,
            ),
            desktopColor = color(
                properties.getProperty(KEY_DESKTOP_COLOR),
                defaultDisplay.desktopColor,
            ),
            coverColor = color(
                properties.getProperty(KEY_COVER_COLOR),
                defaultDisplay.coverColor,
            ),
            pageColor = color(properties.getProperty(KEY_PAGE_COLOR), defaultDisplay.pageColor),
            textColor = color(properties.getProperty(KEY_TEXT_COLOR), defaultDisplay.textColor),
            gutterColor = color(
                properties.getProperty(KEY_GUTTER_COLOR),
                defaultDisplay.gutterColor,
            ),
        )

        val pattern = properties.getProperty(KEY_CHAPTER_PATTERN)
        val rule = pattern?.let { ChapterRuleCatalog.custom(it) } ?: defaultRule
        return AppSettings(display, rule)
    }

    fun saveDisplaySettings(settings: DisplaySettings) {
        val current = load()
        save(AppSettings(settings, current.chapterRule))
    }

    fun saveChapterRule(rule: ChapterRule) {
        val current = load()
        save(AppSettings(current.display, rule))
    }

    private fun save(settings: AppSettings) {
        val properties = Properties()
        val display = settings.display
        properties[KEY_THEME] = display.themeId
        properties[KEY_LAYOUT] = display.layoutMode.name
        properties[KEY_SHOW_BOOK] = display.showBookAndDesktop.toString()
        properties[KEY_PAGE_WIDTH] = display.pageWidth.toString()
        properties[KEY_FONT_FAMILY] = display.fontFamily
        properties[KEY_FONT_SIZE] = display.fontSize.toString()
        properties[KEY_LINE_HEIGHT] = display.lineHeightFactor.toString()
        properties[KEY_INDENT] = display.paragraphIndentEm.toString()
        properties[KEY_BOLD] = display.bold.toString()
        properties[KEY_UNDERLINE] = display.underline.toString()
        properties[KEY_UNDERLINE_COLOR] = display.underlineColor.toString(16)
        properties[KEY_UNDERLINE_WIDTH] = display.underlineWidth.toString()
        properties[KEY_PUNCTUATION] = display.punctuationPlacement.name
        properties[KEY_ANIMATION] = display.animation.name
        properties[KEY_DESKTOP_PATTERN] = display.desktopPatternId
        properties[KEY_COVER_PATTERN] = display.coverPatternId
        properties[KEY_PAGE_PATTERN] = display.pagePatternId
        properties[KEY_DESKTOP_COLOR] = display.desktopColor.toString(16)
        properties[KEY_COVER_COLOR] = display.coverColor.toString(16)
        properties[KEY_PAGE_COLOR] = display.pageColor.toString(16)
        properties[KEY_TEXT_COLOR] = display.textColor.toString(16)
        properties[KEY_GUTTER_COLOR] = display.gutterColor.toString(16)
        properties[KEY_CHAPTER_PATTERN] = settings.chapterRule.pattern.pattern
        properties[KEY_TYPOGRAPHY_VERSION] = CURRENT_TYPOGRAPHY_VERSION.toString()

        file.parent?.let { Files.createDirectories(it) }
        Files.newOutputStream(file).use { properties.store(it, "eyePageFlow settings") }
    }

    private inline fun <reified T : Enum<T>> enumValue(value: String?, default: T): T =
        enumValues<T>().firstOrNull { it.name == value } ?: default

    private fun bool(value: String?, default: Boolean): Boolean =
        value?.toBooleanStrictOrNull() ?: default

    private fun int(value: String?, default: Int): Int =
        value?.toIntOrNull() ?: default

    private fun float(value: String?, default: Float): Float =
        value?.toFloatOrNull() ?: default

    private fun color(value: String?, default: Int): Int =
        value?.toIntOrNull(16) ?: default

    private companion object {
        const val KEY_THEME = "display.theme"
        const val KEY_LAYOUT = "display.layout"
        const val KEY_SHOW_BOOK = "display.showBook"
        const val KEY_PAGE_WIDTH = "display.pageWidth"
        const val KEY_FONT_FAMILY = "display.fontFamily"
        const val KEY_FONT_SIZE = "display.fontSize"
        const val KEY_LINE_HEIGHT = "display.lineHeight"
        const val KEY_INDENT = "display.indent"
        const val KEY_BOLD = "display.bold"
        const val KEY_UNDERLINE = "display.underline"
        const val KEY_UNDERLINE_COLOR = "display.underlineColor"
        const val KEY_UNDERLINE_WIDTH = "display.underlineWidth"
        const val KEY_PUNCTUATION = "display.punctuationPlacement"
        const val KEY_ANIMATION = "display.animation"
        const val KEY_DESKTOP_PATTERN = "display.desktopPattern"
        const val KEY_COVER_PATTERN = "display.coverPattern"
        const val KEY_PAGE_PATTERN = "display.pagePattern"
        const val KEY_DESKTOP_COLOR = "display.desktopColor"
        const val KEY_COVER_COLOR = "display.coverColor"
        const val KEY_PAGE_COLOR = "display.pageColor"
        const val KEY_TEXT_COLOR = "display.textColor"
        const val KEY_GUTTER_COLOR = "display.gutterColor"
        const val KEY_CHAPTER_PATTERN = "chapter.pattern"
        const val KEY_TYPOGRAPHY_VERSION = "display.typographyVersion"
        const val CURRENT_TYPOGRAPHY_VERSION = 2
    }
}
