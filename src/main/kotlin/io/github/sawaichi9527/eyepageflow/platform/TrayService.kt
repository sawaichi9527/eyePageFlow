package io.github.sawaichi9527.eyepageflow.platform

import java.awt.AWTException
import java.awt.BasicStroke
import java.awt.Color
import java.awt.Frame
import java.awt.Graphics2D
import java.awt.GraphicsEnvironment
import java.awt.MenuItem
import java.awt.PopupMenu
import java.awt.RenderingHints
import java.awt.SystemTray
import java.awt.TrayIcon
import java.awt.image.BufferedImage
import javax.swing.JFrame

class TrayService(
    private val frame: JFrame,
    private val onExit: () -> Unit,
) {
    private var trayIcon: TrayIcon? = null

    val installed: Boolean
        get() = trayIcon != null

    fun install() {
        if (GraphicsEnvironment.isHeadless() || !SystemTray.isSupported()) {
            println("tray: not supported, boss key will minimize")
            return
        }
        val popup = PopupMenu()
        val show = MenuItem("顯示主視窗")
        show.addActionListener { restore() }
        val exit = MenuItem("結束")
        exit.addActionListener { onExit() }
        popup.add(show)
        popup.addSeparator()
        popup.add(exit)

        val icon = TrayIcon(createIcon(), "eyePageFlow", popup)
        icon.isImageAutoSize = true
        icon.addActionListener { restore() }
        try {
            SystemTray.getSystemTray().add(icon)
            trayIcon = icon
            println("tray: installed")
        } catch (exception: AWTException) {
            println("tray: could not install (${exception.message})")
        }
    }

    fun restore() {
        frame.isVisible = true
        frame.setState(Frame.NORMAL)
        frame.toFront()
        frame.requestFocus()
    }

    fun remove() {
        trayIcon?.let { SystemTray.getSystemTray().remove(it) }
        trayIcon = null
    }

    private fun createIcon(): BufferedImage {
        val image = BufferedImage(32, 32, BufferedImage.TYPE_INT_ARGB)
        val graphics = image.createGraphics()
        graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
        graphics.color = Color(0x6B4A2F)
        graphics.fillRoundRect(3, 6, 26, 21, 5, 5)
        graphics.color = Color(0xF3E9D2)
        graphics.fillRoundRect(6, 8, 9, 17, 2, 2)
        graphics.fillRoundRect(17, 8, 9, 17, 2, 2)
        graphics.color = Color(0x4A3424)
        graphics.stroke = BasicStroke(1.2f)
        graphics.drawLine(16, 7, 16, 25)
        graphics.dispose()
        return image
    }
}
