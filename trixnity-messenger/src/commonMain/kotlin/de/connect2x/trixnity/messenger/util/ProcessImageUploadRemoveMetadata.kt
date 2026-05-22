package de.connect2x.trixnity.messenger.util

import com.ashampoo.kim.Kim
import com.ashampoo.kim.format.jpeg.JpegRewriter
import com.ashampoo.kim.format.jpeg.iptc.IptcMetadata
import com.ashampoo.kim.format.png.PngChunkType
import com.ashampoo.kim.format.png.PngConstants
import com.ashampoo.kim.format.png.PngImageParser
import com.ashampoo.kim.format.png.PngWriter
import com.ashampoo.kim.format.png.chunk.PngTextChunk
import com.ashampoo.kim.format.tiff.write.TiffOutputSet
import com.ashampoo.kim.format.webp.WebPChunkType
import com.ashampoo.kim.format.webp.WebPImageParser
import com.ashampoo.kim.format.webp.WebPWriter
import com.ashampoo.kim.input.ByteArrayByteReader
import com.ashampoo.kim.model.ImageFormat
import com.ashampoo.kim.output.ByteArrayByteWriter
import de.connect2x.lognity.api.logger.Logger

fun removeImageMetadata(original: ByteArray): ByteArray {
    val log = Logger("de.connect2x.trixnity.messenger.util.ProcessImageUploadRemoveMetadata")

    fun jpeg(): ByteArray {
        log.debug { "Stripping JPEG metadata" }
        val emptyOutputSet = TiffOutputSet()
        emptyOutputSet.getOrCreateRootDirectory()
        val withoutExif = ByteArrayByteWriter()
        JpegRewriter.updateExifMetadataLossless(ByteArrayByteReader(original), withoutExif, emptyOutputSet)

        val withoutIPTC = ByteArrayByteWriter()
        JpegRewriter.writeIPTC(
            ByteArrayByteReader(withoutExif.toByteArray()),
            withoutIPTC,
            IptcMetadata(listOf(), listOf()),
        )

        val withoutXml = ByteArrayByteWriter()
        JpegRewriter.updateXmpXml(ByteArrayByteReader(withoutIPTC.toByteArray()), withoutXml, "")

        return withoutXml.toByteArray()
    }

    fun png(): ByteArray {
        log.debug { "Stripping PNG metadata" }

        val chunks = PngImageParser.readChunks(ByteArrayByteReader(original), null).toMutableList()
        chunks.removeAll {
            it.type == PngChunkType.EXIF ||
                it is PngTextChunk && it.getKeyword() == PngConstants.EXIF_KEYWORD ||
                it is PngTextChunk && it.getKeyword() == PngConstants.IPTC_KEYWORD ||
                it is PngTextChunk && it.getKeyword() == PngConstants.XMP_KEYWORD
        }

        val withoutMetadata = ByteArrayByteWriter()
        PngWriter.writeImage(chunks, withoutMetadata)
        return withoutMetadata.toByteArray()
    }

    fun webP(): ByteArray {
        log.debug { "Stripping WebP metadata" }

        val chunks = WebPImageParser.readChunks(ByteArrayByteReader(original), false).toMutableList()
        chunks.removeAll { it.type == WebPChunkType.EXIF || it.type == WebPChunkType.XMP }
        val withoutMetadata = ByteArrayByteWriter()
        WebPWriter.writeImage(chunks, withoutMetadata, null, null)
        return withoutMetadata.toByteArray()
    }

    val imageFormat = Kim.readMetadata(original)?.imageFormat
    return when (imageFormat) {
        ImageFormat.JPEG -> jpeg()
        ImageFormat.PNG -> png()
        ImageFormat.WEBP -> webP()

        ImageFormat.TIFF,
        ImageFormat.HEIC,
        ImageFormat.AVIF,
        ImageFormat.CR2,
        ImageFormat.CR3,
        ImageFormat.RAF,
        ImageFormat.NEF,
        ImageFormat.ARW,
        ImageFormat.RW2,
        ImageFormat.ORF,
        ImageFormat.DNG,
        ImageFormat.JXL,
        ImageFormat.GIF,
        null -> {
            log.warn { "Stripping metadata of this image format is not supported. Skipping metadata stripping" }
            original
        }
    }
}
