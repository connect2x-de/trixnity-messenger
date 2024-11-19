package de.connect2x.messenger.compose.view.files

import de.connect2x.trixnity.messenger.MatrixMessengerConfiguration
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
import java.io.File
import java.io.IOException
import java.io.InputStream
import java.io.Reader
import java.net.URI
import java.net.URISyntaxException
import javax.imageio.ImageIO
import kotlin.random.Random

private val log = KotlinLogging.logger { }

private val uriListContentType = ContentType.parse("text/uri-list")

private interface ClipboardType {
    data object Image : ClipboardType
    data object AwtImage : ClipboardType
    data object UriList : ClipboardType
    data object FileList : ClipboardType
    data object Text : ClipboardType
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

actual fun getClipboardFile(fileSystem: FileSystem, maxAttachmentSize: Long): Result<FileDescriptor?> {
    log.debug { "access clipboard" }
    val clipboard = Toolkit.getDefaultToolkit().systemClipboard

    if (clipboard.availableDataFlavors.size == 0) {
        return Result.success(null)
    } else {
        clipboard.availableDataFlavors.forEach { flavor ->
            val contentType = ContentType.parse(flavor.mimeType)
            log.info { "content type of current flavor is $contentType" }

            val clipboardType = when {
                isPreviewableImage(contentType) -> ClipboardType.Image
                flavor.representationClass == java.awt.Image::class.java -> ClipboardType.AwtImage
                contentType.match(uriListContentType) -> ClipboardType.UriList
                flavor.isFlavorJavaFileListType -> ClipboardType.FileList
                flavor.isFlavorTextType -> ClipboardType.Text
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
                    log.info { "Sending image via clipboard" }
                    val estimatedSize =
                        clipboard.getDataOrNull<InputStream>(flavor)?.use { it.available().toLong() }
                            ?: run { return Result.success(null) }
                    val baseName = Random.nextString(12)
                    val extStr = contentType.fileExtensions().firstOrNull()?.let { ".$it" } ?: ""

                    return Result.success(
                        BasicFileDescriptor(
                            baseName + extStr,
                            estimatedSize,
                            contentType,
                            byteArrayFlowFromInputStream {
                                clipboard.getDataOrNull<InputStream>(flavor) ?: InputStream.nullInputStream()
                            }
                        )
                    )
                }

                ClipboardType.AwtImage -> {
                    log.info { "Sending AWT image via clipboard" }
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
                        if (outputStream.size() <= maxAttachmentSize) {
                            val byteArray = outputStream.toByteArray()
                            val baseName = Random.nextString(12)
                            return Result.success(
                                BasicFileDescriptor(
                                    "$baseName.png",
                                    byteArray.size.toLong(),
                                    ContentType.Image.PNG,
                                    byteArray.toByteArrayFlow(),
                                )
                            )
                        }
                    }
                }

                ClipboardType.UriList -> {
                    val data =
                        clipboard.getDataOrNull<Reader>(flavor)
                            ?: run { return Result.failure(NotPasteableException()) }

                    // TODO: Attach multiple files at once
                    val fileName = data.useLines { lines -> lines.firstOrNull() } ?: run {
                        log.info { "the selected files list is empty" }
                        return Result.failure(EmptyFileListException())
                    }
                    val uri = try {
                        URI(fileName)
                    } catch (_: URISyntaxException) {
                        null
                    }
                    if (uri?.scheme != "file") {
                        log.warn { "improperly formatted uri: $fileName" }
                        return Result.success(null)
                    }
                    return Result.success(
                        PathFileDescriptor(
                            uri.path.toPath(normalize = true),
                            fileSystem = fileSystem
                        )
                    )
                }

                ClipboardType.FileList -> {
                    log.info { "Sending fileList via clipboard" }
                    val data = clipboard.getDataOrNull<List<File>>(flavor)
                    data?.get(0)?.let {
                        return Result.success(
                            PathFileDescriptor(
                                it.path.toPath(),
                                fileSystem
                            )
                        )
                    }
                }

                ClipboardType.Text -> {
                    return Result.success(null)
                }
            }
        }
    }

    log.info { "content in clipboard was not paste-able" }
    return Result.failure(NotPasteableException())
}
