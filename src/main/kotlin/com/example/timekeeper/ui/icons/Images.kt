package com.example.timekeeper.ui.icons

import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.toComposeImageBitmap
import org.jetbrains.skia.Image as SkiaImage
import java.awt.Image
import java.awt.image.BufferedImage
import java.io.ByteArrayOutputStream
import javax.imageio.ImageIO

/**
 * Composes the shell's AWT icon into something [androidx.compose.foundation.Image]
 * can draw. The round-trip through PNG is the simplest reliable AWT → Skia bridge
 * and these are tiny (a few KB) icons, decoded once each.
 */
fun Image.toIconBitmap(): ImageBitmap? = try {
    val buffered = if (this is BufferedImage) {
        this
    } else {
        BufferedImage(getWidth(null).coerceAtLeast(1), getHeight(null).coerceAtLeast(1), BufferedImage.TYPE_INT_ARGB)
            .also { it.createGraphics().drawImage(this, 0, 0, null) }
    }
    val bytes = ByteArrayOutputStream().use { out ->
        ImageIO.write(buffered, "png", out)
        out.toByteArray()
    }
    SkiaImage.makeFromEncoded(bytes).toComposeImageBitmap()
} catch (t: Throwable) {
    null
}
