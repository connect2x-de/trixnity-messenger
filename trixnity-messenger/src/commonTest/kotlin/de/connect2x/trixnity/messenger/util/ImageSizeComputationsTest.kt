package de.connect2x.trixnity.messenger.util

import io.kotest.core.spec.style.ShouldSpec
import io.kotest.matchers.shouldBe

class ImageSizeComputationsTest : ShouldSpec() {

    init {
        should("return height of image if image can be placed in bounds") {
            ImageSizeComputations.getHeight(300, 400, 200, 400) shouldBe 200
        }

        should("return max height if image is only too high") {
            ImageSizeComputations.getHeight(400, 400, 500, 300) shouldBe 300
        }

        should("return scaled down height so that image matches max width if height is already below max height") {
            ImageSizeComputations.getHeight(800, 400, 200, 300) shouldBe 100
        }

        should("return scaled down height so that image matches max width if height is too high") {
            ImageSizeComputations.getHeight(800, 400, 400, 300) shouldBe 200
        }

        should("return max height so that image matches max width if even scaled down height is too high") {
            ImageSizeComputations.getHeight(800, 400, 800, 300) shouldBe 300
        }

        should("return width of image if image can be placed in bounds") {
            ImageSizeComputations.getWidth(300, 400, 200, 400) shouldBe 300
        }

        should("return max width if image is only too wide") {
            ImageSizeComputations.getWidth(500, 400, 300, 300) shouldBe 400
        }

        should("return scaled down width so that image matches max height if width is already below max width") {
            ImageSizeComputations.getWidth(200, 400, 600, 300) shouldBe 100
        }

        should("return scaled down width so that image matches max height if width is too wide") {
            ImageSizeComputations.getWidth(500, 400, 600, 300) shouldBe 250
        }

        should("return max width so that image matches max height if even scaled down width is too wide") {
            ImageSizeComputations.getWidth(1_000, 400, 600, 300) shouldBe 400
        }
    }

}
