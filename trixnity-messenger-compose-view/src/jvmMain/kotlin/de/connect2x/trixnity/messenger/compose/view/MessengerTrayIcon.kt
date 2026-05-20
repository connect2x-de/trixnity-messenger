package de.connect2x.trixnity.messenger.compose.view

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.toComposeImageBitmap
import de.connect2x.lognity.api.logger.Logger
import de.connect2x.trixnity.messenger.util.OS
import de.connect2x.trixnity.messenger.util.getOs
import org.jetbrains.skia.Font
import org.jetbrains.skia.FontMgr
import org.jetbrains.skia.FontStyle
import org.jetbrains.skia.Image
import org.jetbrains.skia.Paint
import org.jetbrains.skia.shaper.Shaper

class MessengerTrayIcon(private val unreadMessages: Int, iconSize: Float = 32f) : Painter() {
    override val intrinsicSize = Size(iconSize, iconSize)

    companion object {
        private val log: Logger = Logger("de.connect2x.trixnity.messenger.compose.view.MessengerTrayIcon")

        private val bitmap16 =
            MessengerTrayIcon::class
                .java
                .getResourceAsStream("/logo_16.png")
                ?.readAllBytes()
                ?.let { Image.makeFromEncoded(it) }
                ?.toComposeImageBitmap()
        private val bitmap32 =
            MessengerTrayIcon::class
                .java
                .getResourceAsStream("/logo_32.png")
                ?.readAllBytes()
                ?.let { Image.makeFromEncoded(it) }
                ?.toComposeImageBitmap()
        private val bitmap64 =
            MessengerTrayIcon::class
                .java
                .getResourceAsStream("/logo_64.png")
                ?.readAllBytes()
                ?.let { Image.makeFromEncoded(it) }
                ?.toComposeImageBitmap()
        private val bitmap =
            MessengerTrayIcon::class
                .java
                .getResourceAsStream("/logo.png")
                ?.readAllBytes()
                ?.let { Image.makeFromEncoded(it) }
                ?.toComposeImageBitmap()
    }

    override fun DrawScope.onDraw() {
        val offset =
            if (getOs() == OS.MAC_OS) {
                when (size.width) {
                    22f,
                    32f -> Offset(3f, 3f)
                    44f,
                    64f -> Offset(6f, 6f)
                    else -> Offset(0f, 0f)
                }
            } else Offset(0f, 0f)
        val logoName =
            when {
                size.width < 30f -> bitmap16
                size.width < 60f -> bitmap32
                size.width < 120f -> bitmap64
                else -> bitmap
            }
        logoName?.let { bitmap: ImageBitmap ->
            log.debug { "set logo with size: ${size.width}" }
            drawImage(bitmap, topLeft = offset)
            if (unreadMessages > 0) {
                val red = Color(0xFFFF00000)
                val radius = size.minDimension / 6
                val start = size.minDimension - radius * 2
                drawCircle(red, radius, center = Offset(start, offset.y + radius))
                drawCircle(red, radius, center = Offset(size.minDimension - radius, offset.y + radius))
                drawRect(red, topLeft = Offset(start, offset.y), size = Size(radius, radius * 2))
                if (size.width > 128f) {
                    drawImage(createImageBitmap(unreadMessages, size.minDimension), topLeft = offset)
                }
            }
        }
    }
}

private fun createImageBitmap(unreadMessages: Int, iconSize: Float): ImageBitmap {
    val imageWidth = iconSize.toInt()
    val imageHeight = iconSize.toInt()
    val image = ImageBitmap(imageWidth, imageHeight)
    Canvas(image).nativeCanvas.apply {
        val skiaPaint = Paint()
        skiaPaint.color = 0xFFFFFFFF.toInt()
        val fontSize =
            when {
                iconSize < 1024f -> 15f
                else -> 160f
            }
        FontMgr.default.matchFamilyStyle("verdana", FontStyle.NORMAL)?.use { typeface ->
            Font(typeface, fontSize).use { font ->
                Shaper.makePrimitive().use { shaper ->
                    val string = if (unreadMessages > 99) "99+" else unreadMessages.toString()
                    val text = shaper.shape(string, font, 400f)
                    when {
                        iconSize < 1024f ->
                            text?.let { drawTextBlob(text, 23f - ((string.length - 1) * 5f), 0f, skiaPaint) }

                        else -> text?.let { drawTextBlob(text, 720f - ((string.length - 1) * 60f), 80f, skiaPaint) }
                    }
                }
            }
        }
    }
    return image
}
