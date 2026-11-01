package io.github.sawaichi9527.eyepageflow.render

import java.awt.Color
import java.awt.Graphics2D
import java.awt.Rectangle
import java.awt.geom.RoundRectangle2D
import kotlin.random.Random

/**
 * Deterministic, procedural textures so the project does not need to ship
 * photographic assets. The seed is derived from the texture kind so repeated
 * paints look identical.
 */
object TexturePainter {
    fun paint(
        g2: Graphics2D,
        rect: Rectangle,
        baseColor: Color,
        kind: TextureKind,
    ) {
        g2.color = baseColor
        g2.fillRect(rect.x, rect.y, rect.width, rect.height)
        if (kind == TextureKind.NONE) return

        val random = Random(kind.ordinal * 7919)
        when (kind) {
            TextureKind.PAPER -> paintPaper(g2, rect, baseColor, random)
            TextureKind.LEATHER -> paintLeather(g2, rect, baseColor, random)
            TextureKind.WOOD -> paintWood(g2, rect, baseColor, random)
            TextureKind.LINEN -> paintLinen(g2, rect, baseColor, random)
            TextureKind.NONE -> Unit
        }
    }

    fun paintRounded(
        g2: Graphics2D,
        shape: RoundRectangle2D,
        baseColor: Color,
        kind: TextureKind,
    ) {
        val clip = g2.clip
        g2.clip(shape)
        paint(g2, shape.bounds, baseColor, kind)
        g2.clip = clip
    }

    private fun paintPaper(
        g2: Graphics2D,
        rect: Rectangle,
        baseColor: Color,
        random: Random,
    ) {
        repeat(180) {
            val y = rect.y + random.nextInt(rect.height)
            val alpha = 8 + random.nextInt(12)
            g2.color = if (random.nextBoolean()) {
                Color(80, 60, 35, alpha)
            } else {
                Color(255, 255, 255, alpha)
            }
            g2.drawLine(rect.x, y, rect.x + rect.width, y)
        }
        repeat(60) {
            val x = rect.x + random.nextInt(rect.width)
            val y = rect.y + random.nextInt(rect.height)
            g2.color = Color(90, 70, 45, 12 + random.nextInt(10))
            g2.fillOval(x, y, 1 + random.nextInt(2), 1 + random.nextInt(2))
        }
        val shade = Color(
            baseColor.red,
            baseColor.green,
            baseColor.blue,
            40,
        )
        g2.color = shade
        g2.drawRect(rect.x, rect.y, rect.width - 1, rect.height - 1)
    }

    private fun paintLeather(
        g2: Graphics2D,
        rect: Rectangle,
        baseColor: Color,
        random: Random,
    ) {
        repeat(260) {
            val x = rect.x + random.nextInt(rect.width)
            val y = rect.y + random.nextInt(rect.height)
            val dark = random.nextInt(3) == 0
            g2.color = if (dark) {
                Color(0, 0, 0, 10 + random.nextInt(14))
            } else {
                Color(255, 255, 255, 6 + random.nextInt(10))
            }
            g2.fillOval(x, y, 1 + random.nextInt(3), 1 + random.nextInt(3))
        }
        g2.color = Color(
            baseColor.red / 2,
            baseColor.green / 2,
            baseColor.blue / 2,
            60,
        )
        g2.drawRoundRect(rect.x, rect.y, rect.width - 1, rect.height - 1, 10, 10)
    }

    private fun paintWood(
        g2: Graphics2D,
        rect: Rectangle,
        baseColor: Color,
        random: Random,
    ) {
        repeat(46) {
            val y = rect.y + random.nextInt(rect.height)
            val amplitude = 1 + random.nextInt(4)
            val alpha = 8 + random.nextInt(16)
            g2.color = if (random.nextBoolean()) {
                Color(0, 0, 0, alpha)
            } else {
                Color(255, 235, 200, alpha)
            }
            var x = rect.x
            while (x < rect.x + rect.width) {
                val dy = random.nextInt(amplitude * 2 + 1) - amplitude
                g2.drawLine(x, y + dy, x + 18, y + dy)
                x += 18
            }
        }
        g2.color = Color(
            baseColor.red / 2,
            baseColor.green / 2,
            baseColor.blue / 2,
            80,
        )
        g2.drawRect(rect.x, rect.y, rect.width - 1, rect.height - 1)
    }

    private fun paintLinen(
        g2: Graphics2D,
        rect: Rectangle,
        baseColor: Color,
        random: Random,
    ) {
        repeat(60) {
            val x = rect.x + random.nextInt(rect.width)
            val alpha = 6 + random.nextInt(10)
            g2.color = Color(0, 0, 0, alpha)
            g2.drawLine(x, rect.y, x, rect.y + rect.height)
        }
        repeat(60) {
            val y = rect.y + random.nextInt(rect.height)
            val alpha = 6 + random.nextInt(10)
            g2.color = Color(255, 255, 255, alpha)
            g2.drawLine(rect.x, y, rect.x + rect.width, y)
        }
        g2.color = Color(
            baseColor.red / 2,
            baseColor.green / 2,
            baseColor.blue / 2,
            60,
        )
        g2.drawRect(rect.x, rect.y, rect.width - 1, rect.height - 1)
    }
}
