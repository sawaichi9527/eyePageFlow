package io.github.sawaichi9527.eyepageflow.core

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class ColorSchemeCatalogTest {
    @Test
    fun `schemes have unique ids and usable contrast`() {
        val schemes = ColorSchemeCatalog.schemes
        assertTrue(schemes.size >= 5)
        assertEquals(schemes.size, schemes.map { it.id }.toSet().size)
        schemes.forEach { scheme ->
            assertTrue(scheme.pageColor != scheme.textColor)
            assertTrue(scheme.displayName.isNotBlank())
        }
    }
}
