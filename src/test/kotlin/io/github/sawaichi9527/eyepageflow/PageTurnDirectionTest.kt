package io.github.sawaichi9527.eyepageflow

import io.github.sawaichi9527.eyepageflow.core.WritingMode
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class PageTurnDirectionTest {
    @Test
    fun `horizontal turns the next page right to left`() {
        assertTrue(pageTurnForward(WritingMode.HORIZONTAL, from = 0, to = 2), "下一頁：右→左")
        assertFalse(pageTurnForward(WritingMode.HORIZONTAL, from = 6, to = 4), "上一頁：左→右")
    }

    @Test
    fun `vertical turns the next page left to right because it is a right-bound book`() {
        assertFalse(pageTurnForward(WritingMode.VERTICAL, from = 0, to = 2), "下一頁：左→右")
        assertTrue(pageTurnForward(WritingMode.VERTICAL, from = 6, to = 4), "上一頁：右→左")
    }

    @Test
    fun `vertical is the mirror of horizontal for the same move`() {
        listOf(0 to 2, 2 to 4, 4 to 2, 2 to 0).forEach { (from, to) ->
            assertTrue(
                pageTurnForward(WritingMode.VERTICAL, from, to) !=
                    pageTurnForward(WritingMode.HORIZONTAL, from, to),
                "$from -> $to 的橫排與直排方向應該相反",
            )
        }
    }
}
