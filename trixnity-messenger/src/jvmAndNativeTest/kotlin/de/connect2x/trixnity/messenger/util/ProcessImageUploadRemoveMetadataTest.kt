package de.connect2x.trixnity.messenger.util

import com.ashampoo.kim.Kim
import de.connect2x.trixnity.utils.readByteArrayFlow
import de.connect2x.trixnity.utils.toByteArray
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import kotlin.test.Test
import kotlinx.coroutines.test.runTest
import okio.Path
import okio.Path.Companion.toPath

class ProcessImageUploadRemoveMetadataTest {
    @Test
    fun `jpg - should remove exif and IPTC and XMP metadata`() = runTest {
        testMetadataRemoval("./src/commonTest/resources/images/metadata.jpg".toPath(normalize = true))
    }

    @Test
    fun `png - should remove exif and IPTC and XMP metadata`() = runTest {
        testMetadataRemoval("./src/commonTest/resources/images/metadata.png".toPath(normalize = true))
    }

    @Test
    fun `webp - should remove exif and IPTC and XMP metadata`() = runTest {
        testMetadataRemoval("./src/commonTest/resources/images/metadata.webp".toPath(normalize = true))
    }

    private suspend fun testMetadataRemoval(path: Path) {
        val cut = ::removeImageMetadata
        fileSystem.readByteArrayFlow(path)?.let { original ->
            val stripped = cut(original.toByteArray())
            val strippedMetadata = Kim.readMetadata(stripped)
            strippedMetadata shouldNotBeNull
                {
                    val exifDirectories = exif?.directories
                    exifDirectories?.onEach { exifDirectory -> exifDirectory.entries.isEmpty() shouldBe true }
                    exif?.makerNoteDirectory shouldBe null
                    exif?.geoTiffDirectory shouldBe null
                    iptc?.records.isNullOrEmpty() shouldBe true
                    xmp shouldBe null
                }
        }
    }
}
