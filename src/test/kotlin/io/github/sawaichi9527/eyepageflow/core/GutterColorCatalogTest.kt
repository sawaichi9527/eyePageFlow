package io.github.sawaichi9527.eyepageflow.core

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class GutterColorCatalogTest {
    @Test
    fun `gutter presets are unique and readable`() {
        val presets = GutterColorCatalog.presets
        assertTrue(presets.size >= 4)
        assertEquals(presets.size, presets.map { it.id }.toSet().size)
        assertEquals(presets.size, presets.map { it.color }.toSet().size)
        presets.forEach { assertTrue(it.displayName.isNotBlank()) }
    }
}
