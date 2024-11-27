package de.connect2x.trixnity.messenger.util

import io.kotest.core.spec.style.ShouldSpec
import io.kotest.matchers.shouldBe
import net.folivo.trixnity.utils.readByteArrayFlow
import okio.Path.Companion.toPath

class ImagesTest : ShouldSpec() {
    init {
        should("determine image dimensions") {
            fileSystem.readByteArrayFlow("./src/commonTest/resources/images/cat.jpg".toPath(normalize = true))?.let {
                getImageDimensions(it, Long.MAX_VALUE) shouldBe (640 to 457)
            }
        }
    }
}
