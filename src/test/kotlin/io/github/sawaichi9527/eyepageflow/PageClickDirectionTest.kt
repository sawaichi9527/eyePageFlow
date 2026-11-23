package io.github.sawaichi9527.eyepageflow

import io.github.sawaichi9527.eyepageflow.core.WritingMode
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class PageClickDirectionTest {
    @Test
    fun `horizontal advances on the right half`() {
        assertFalse(pageClickTurnsForward(40, width = 100, WritingMode.HORIZONTAL))
        assertTrue(pageClickTurnsForward(60, width = 100, WritingMode.HORIZONTAL))
    }

    @Test
    fun `vertical advances on the left half because it is a right-bound book`() {
        assertTrue(pageClickTurnsForward(40, width = 100, WritingMode.VERTICAL))
        assertFalse(pageClickTurnsForward(60, width = 100, WritingMode.VERTICAL))
    }

    @Test
    fun `vertical is the mirror of horizontal`() {
        (0 until 100 step 5).forEach { x ->
            assertTrue(
                pageClickTurnsForward(x, 100, WritingMode.VERTICAL) !=
                    pageClickTurnsForward(x, 100, WritingMode.HORIZONTAL),
                "x=$x 的橫排與直排點擊方向應該相反",
            )
        }
    }
}
