package com.example.timekeeper.ui.icons

import java.awt.BasicStroke
import java.awt.Color
import java.awt.RenderingHints
import java.awt.geom.Ellipse2D
import java.awt.geom.Line2D
import java.awt.image.BufferedImage

/**
 * The app's own icon — the same clock mark as the phone app, drawn here so the
 * build needs no image asset. Used for the window and the tray.
 */
object AppIcon {

    fun image(size: Int = 64): BufferedImage {
        val img = BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB)
        val g = img.createGraphics()
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)

        val c = size / 2.0
        val plate = Color(0x4F, 0x5B, 0xD5)   // indigo, matches the theme's primary
        val hand = Color.WHITE

        g.color = plate
        g.fill(Ellipse2D.Double(c - size * 0.47, c - size * 0.47, size * 0.94, size * 0.94))

        g.color = hand
        g.stroke = BasicStroke((size * 0.06f).coerceAtLeast(1.5f), BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND)
        // A hose/clock style face ring and two hands, so it still reads when small.
        g.draw(Ellipse2D.Double(c - size * 0.31, c - size * 0.31, size * 0.62, size * 0.62))
        g.draw(Line2D.Double(c, c, c, c - size * 0.21))       // minute hand, up
        g.draw(Line2D.Double(c, c, c + size * 0.16, c))       // hour hand, right

        g.dispose()
        return img
    }
}
