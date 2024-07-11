package de.connect2x.trixnity.messenger.viewmodel.room.timeline

import de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements.util.SizeComputations
import io.kotest.core.spec.style.ShouldSpec
import io.kotest.matchers.shouldBe

class SizeComputationsTest : ShouldSpec() {

    init {
        should("return height of image if image can be placed in bounds") {
            SizeComputations.getHeight(200, 400, 300, 400f) shouldBe 200
        }

        should("return max height if image is only too high") {
            SizeComputations.getHeight(500, 300, 400, 400f) shouldBe 300
        }

        should("return scaled down height so that image matches max width if height is already below max height") {
            SizeComputations.getHeight(200, 300, 800, 400f) shouldBe 100
        }

        should("return scaled down height so that image matches max width if height is too high") {
            SizeComputations.getHeight(400, 300, 800, 400f) shouldBe 200
        }

        should("return max height so that image matches max width if even scaled down height is too high") {
            SizeComputations.getHeight(800, 300, 800, 400f) shouldBe 300
        }

        should("return width of image if image can be placed in bounds") {
            SizeComputations.getWidth(200, 400f, 300, 400f) shouldBe 300
        }

        should("return max width if image is only too wide") {
            SizeComputations.getWidth(300, 300f, 500, 400f) shouldBe 400
        }

        should("return scaled down width so that image matches max height if width is already below max width") {
            SizeComputations.getWidth(600, 300f, 200, 400f) shouldBe 100
        }

        should("return scaled down width so that image matches max height if width is too wide") {
            SizeComputations.getWidth(600, 300f, 500, 400f) shouldBe 250
        }

        should("return max width so that image matches max height if even scaled down width is too wide") {
            SizeComputations.getWidth(600, 300f, 1_000, 400f) shouldBe 400
        }
    }

}
