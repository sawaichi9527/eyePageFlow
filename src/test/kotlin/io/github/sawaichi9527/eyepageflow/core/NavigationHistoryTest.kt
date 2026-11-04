package io.github.sawaichi9527.eyepageflow.core

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class NavigationHistoryTest {
    @Test
    fun `back returns the previous page after a move`() {
        val history = NavigationHistory()
        history.record(from = 0, to = 2)
        assertTrue(history.canGoBack)
        assertEquals(0, history.back(current = 2))
        assertFalse(history.canGoBack)
        assertEquals(2, history.forward(current = 0))
    }

    @Test
    fun `a new move clears the forward stack`() {
        val history = NavigationHistory()
        history.record(0, 2)
        history.back(2)
        history.record(from = 0, to = 4)
        assertFalse(history.canGoForward)
        assertEquals(0, history.back(current = 4))
    }

    @Test
    fun `returning to the same page is not recorded`() {
        val history = NavigationHistory()
        history.record(4, 4)
        assertFalse(history.canGoBack)
        assertNull(history.back(4))
    }

    @Test
    fun `history is capped`() {
        val history = NavigationHistory(limit = 3)
        (0..5).forEach { history.record(it * 2, it * 2 + 2) }
        var steps = 0
        var current = 12
        while (true) {
            val target = history.back(current) ?: break
            current = target
            steps += 1
        }
        assertEquals(3, steps)
    }

    @Test
    fun `clear drops both directions`() {
        val history = NavigationHistory()
        history.record(0, 2)
        history.back(2)
        history.clear()
        assertFalse(history.canGoBack)
        assertFalse(history.canGoForward)
        assertNull(history.back(0))
    }
}
