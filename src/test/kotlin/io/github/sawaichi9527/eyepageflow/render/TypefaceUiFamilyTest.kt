package io.github.sawaichi9527.eyepageflow.render

import java.awt.Font
import java.awt.GraphicsEnvironment
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Assumptions.assumeTrue
import org.junit.jupiter.api.Test

class TypefaceUiFamilyTest {
    private val families = GraphicsEnvironment.getLocalGraphicsEnvironment()
        .availableFontFamilyNames
        .toSet()

    @Test
    fun `ui family resolves to an installed font that can display chinese`() {
        assumeTrue(families.any { it.startsWith("Noto Sans") }, "需要 Noto Sans CJK／Noto Sans 才能驗證")

        val resolved = Typeface.resolveUiFamily()

        assertTrue(resolved != null && resolved in families, "解析出的家族必須真的存在：$resolved")
        assertTrue(Font(resolved, Font.PLAIN, 15).canDisplay('檔'), "UI 字型必須能顯示中文")
    }

    @Test
    fun `ui family list only contains free cross platform fonts`() {
        val resolved = Typeface.resolveUiFamily()
        if (resolved != null) {
            assertTrue(
                resolved.startsWith("Noto Sans"),
                "UI 家族只接受 Noto Sans 系列，避免帶入有授權顧慮的系統字型：$resolved",
            )
        }
    }
}
