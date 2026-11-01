package io.github.sawaichi9527.eyepageflow.render

import io.github.sawaichi9527.eyepageflow.core.PageMetrics
import java.awt.Font
import java.awt.GraphicsEnvironment

object Typeface {
    const val DEFAULT_FAMILY = "Noto Serif CJK TC"

    fun resolveFamily(preferred: String = DEFAULT_FAMILY): String {
        val families = GraphicsEnvironment.getLocalGraphicsEnvironment()
            .availableFontFamilyNames
            .toSet()
        return if (preferred in families) preferred else Font.SERIF
    }

    fun pageFont(metrics: PageMetrics, family: String = DEFAULT_FAMILY): Font = Font(
        resolveFamily(family),
        if (metrics.bold) Font.BOLD else Font.PLAIN,
        metrics.fontSize.toInt(),
    )
}
