package io.github.sawaichi9527.eyepageflow.platform

import java.awt.geom.AffineTransform
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class DisplayScaleTest {
    @Test
    fun `reads the scale factor from the default transform`() {
        assertEquals(1.0f, DisplayScale.of(AffineTransform()), 0.0001f)
        assertEquals(
            1.5f,
            DisplayScale.of(AffineTransform.getScaleInstance(1.5, 1.5)),
            0.0001f,
        )
        assertEquals(
            1.25f,
            DisplayScale.of(AffineTransform.getScaleInstance(1.25, 1.25)),
            0.0001f,
        )
    }

    @Test
    fun `detects a real scale change`() {
        assertTrue(DisplayScale.changed(1.0f, 1.25f))
        assertTrue(DisplayScale.changed(1.0f, 1.5f))
        assertTrue(DisplayScale.changed(1.5f, 1.0f))
    }

    @Test
    fun `ignores floating point noise and unknown values are treated as changed`() {
        assertFalse(DisplayScale.changed(1.5f, 1.5004f))
        assertFalse(DisplayScale.changed(1.25f, 1.25f))
        assertTrue(DisplayScale.changed(0f, 1.0f))
    }
}
