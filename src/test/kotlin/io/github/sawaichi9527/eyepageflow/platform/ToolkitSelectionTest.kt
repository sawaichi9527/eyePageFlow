package io.github.sawaichi9527.eyepageflow.platform

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

class ToolkitSelectionTest {
    @Test
    fun `linux wayland with jbr toolkit selects WLToolkit`() {
        assertEquals(
            ToolkitSelection.WAYLAND_TOOLKIT,
            ToolkitSelection.decide("Linux", "wayland", hasWaylandToolkit = true, override = null),
        )
    }

    @Test
    fun `linux wayland without jbr toolkit keeps the default`() {
        // Temurin 沒有 sun.awt.wl.WLToolkit，硬設會啟動失敗。
        assertNull(ToolkitSelection.decide("Linux", "wayland", hasWaylandToolkit = false, override = null))
    }

    @Test
    fun `linux x11 keeps the default toolkit`() {
        assertNull(ToolkitSelection.decide("Linux", "x11", hasWaylandToolkit = true, override = null))
    }

    @Test
    fun `windows and macos keep the default toolkit`() {
        assertNull(ToolkitSelection.decide("Windows 11", "wayland", hasWaylandToolkit = true, override = null))
        assertNull(ToolkitSelection.decide("Mac OS X", "wayland", hasWaylandToolkit = true, override = null))
    }

    @Test
    fun `explicit override wins over session detection`() {
        assertEquals(
            "XToolkit",
            ToolkitSelection.decide("Linux", "wayland", hasWaylandToolkit = true, override = "XToolkit"),
        )
        assertEquals(
            "WLToolkit",
            ToolkitSelection.decide("Linux", "x11", hasWaylandToolkit = false, override = "WLToolkit"),
        )
    }

    @Test
    fun `missing session type keeps the default toolkit`() {
        assertNull(ToolkitSelection.decide("Linux", null, hasWaylandToolkit = true, override = null))
    }
}
