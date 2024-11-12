package de.connect2x.messenger.compose.view.files

import de.connect2x.trixnity.messenger.util.BasicFileDescriptor
import de.connect2x.trixnity.messenger.util.FileDescriptor
import de.connect2x.trixnity.messenger.util.PathFileDescriptor
import io.github.oshai.kotlinlogging.KotlinLogging
import io.ktor.http.*
import net.folivo.trixnity.utils.byteArrayFlowFromInputStream
import net.folivo.trixnity.utils.nextString
import net.folivo.trixnity.utils.toByteArrayFlow
import okio.FileSystem
import okio.Path.Companion.toPath
import java.awt.Toolkit
import java.awt.datatransfer.Clipboard
import java.awt.datatransfer.DataFlavor
import java.awt.datatransfer.UnsupportedFlavorException
import java.awt.image.BufferedImage
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.io.InputStream
import java.io.Reader
import java.net.URI
import javax.imageio.ImageIO
import kotlin.random.Random

private val log = KotlinLogging.logger { }

private val uriListContentType = ContentType.parse("text/uri-list")

private interface ClipboardType {
    data object Image : ClipboardType
    data object AwtImage : ClipboardType
    data object UriList : ClipboardType
}

private inline fun <reified T> Clipboard.getDataOrNull(flavor: DataFlavor): T? {
    try {
        val rawData = getData(flavor)
        if (rawData is T) {
            return rawData
        } else {
            log.error { "expected data $rawData to be of type ${T::class.qualifiedName}" }
            return null
        }
    } catch (e: IllegalStateException) {
        log.error { "tried to get data from clipboard which is no longer available" }
    } catch (e: UnsupportedFlavorException) {
        log.error { "tried to get data from clipboard in an unsupported flavor: ${flavor.humanPresentableName}" }
    } catch (e: IOException) {
        log.error { "could not read data: ${e.message}" }
    }

    return null
}

private fun isPreviewableImage(contentType: ContentType): Boolean {
    return contentType.match(ContentType.Image.PNG) ||
            contentType.match(ContentType.Image.JPEG) ||
            contentType.match(ContentType.Image.GIF)
}

actual fun getClipboardFile(fileSystem: FileSystem): FileDescriptor? {
    log.debug { "access clipboard" }
    val clipboard = Toolkit.getDefaultToolkit().systemClipboard

    clipboard.availableDataFlavors.forEach { flavor ->
        val contentType = ContentType.parse(flavor.mimeType)

        val clipboardType = when {
            isPreviewableImage(contentType) -> ClipboardType.Image
            flavor.representationClass == java.awt.Image::class.java -> ClipboardType.AwtImage
            contentType.match(uriListContentType) -> ClipboardType.UriList
            else -> {
                log.trace { "unknown clipboard flavor: ${flavor.humanPresentableName}" }
                return@forEach
            }
        }

        if (clipboardType == ClipboardType.Image && !flavor.isRepresentationClassInputStream) {
            log.warn { "cannot handle image stored in ${flavor.representationClass}" }
            return@forEach
        }
        if (clipboardType == ClipboardType.UriList && !flavor.isRepresentationClassReader) {
            log.warn { "cannot handle uri list stored in ${flavor.representationClass}" }
            return@forEach
        }

        when (clipboardType) {
            ClipboardType.Image -> {
                val estimatedSize =
                    clipboard.getDataOrNull<InputStream>(flavor)?.use { it.available().toLong() } ?: run { return null }
                val baseName = Random.nextString(12)
                val extStr = contentType.fileExtensions().firstOrNull()?.let { ".$it" } ?: ""

                return BasicFileDescriptor(
                    baseName + extStr,
                    estimatedSize,
                    contentType,
                    byteArrayFlowFromInputStream {
                        clipboard.getDataOrNull<InputStream>(flavor) ?: InputStream.nullInputStream()
                    }
                )
            }

            ClipboardType.AwtImage -> {
                clipboard.getDataOrNull<java.awt.Image>(flavor)?.let { img ->
                    // TODO: revisit this when we have ImageMagick
                    // this might not be the most efficient way, but works for images in memory on MacOS...
                    val image = BufferedImage(img.getWidth(null), img.getHeight(null), BufferedImage.TYPE_INT_ARGB)
                    image.createGraphics().apply {
                        drawImage(img, 0, 0, null)
                        dispose()
                    }
                    val outputStream = ByteArrayOutputStream()
                    ImageIO.write(image, "png", outputStream)
                    outputStream.flush()
                    val byteArray = outputStream.toByteArray()
                    outputStream.close()

                    val baseName = Random.nextString(12)
                    return BasicFileDescriptor(
                        "$baseName.png",
                        byteArray.size.toLong(),
                        ContentType.Image.PNG,
                        byteArray.toByteArrayFlow(),
                    )
                }
            }

            ClipboardType.UriList -> {
                val data = clipboard.getDataOrNull<Reader>(flavor) ?: run { return null }

                // TODO: Attach multiple files at once
                val fileName = data.useLines { lines -> lines.firstOrNull() } ?: run {
                    log.info { "the selected files list is empty" }
                    return null
                }
                val uri = URI(fileName)
                if (uri.scheme != "file") {
                    log.warn { "improperly formatted uri: $fileName" }
                    return null
                }
                return PathFileDescriptor(uri.path.toPath(normalize = true), fileSystem = fileSystem)
            }
        }
    }

    log.info { "content in clipboard was not paste-able" }
    return null
}
