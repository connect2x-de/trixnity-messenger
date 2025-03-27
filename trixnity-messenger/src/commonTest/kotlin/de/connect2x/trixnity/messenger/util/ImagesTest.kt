package de.connect2x.trixnity.messenger.util

import io.kotest.matchers.shouldBe
import kotlinx.coroutines.test.runTest
import net.folivo.trixnity.utils.readByteArrayFlow
import okio.Path.Companion.toPath
import kotlin.test.Ignore
import kotlin.test.Test


class ImagesTest {

    // TODO js
    @Test
    @Ignore
    fun `determine image dimensions`() = runTest {
        fileSystem.readByteArrayFlow("./src/commonTest/resources/images/cat.jpg".toPath(normalize = true))?.let {
            getImageDimensions(it, Long.MAX_VALUE) shouldBe (640 to 457)
        }
    }
}
