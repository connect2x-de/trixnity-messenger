package de.connect2x.trixnity.messenger.viewmodel.room.timeline

import de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements.util.FileNameComputations
import io.kotest.core.spec.style.ShouldSpec
import io.kotest.matchers.shouldBe
import io.ktor.http.*
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import org.kodein.mock.Mock
import org.kodein.mock.Mocker

class FileNameComputationsTest : ShouldSpec() {

    val mocker = Mocker()

    @Mock
    lateinit var clock: Clock

    init {
        beforeTest {
            mocker.reset()
            injectMocks(mocker)

            mocker.every { clock.now() } returns Instant.parse("2021-03-03T03:03:03+01:00")
        }

        should("use the name of the file in the body") {
            val cut = FileNameComputations(clock)
            cut.getOrCreateFileName("image.png", "image/png", ContentType.Image.PNG) shouldBe "image.png"
        }

        should("if body is empty: pad all values with leading zeroes if less than 2 digits") {
            val cut = FileNameComputations(clock)
            cut.getOrCreateFileName(
                "",
                "image/jpeg",
                ContentType.Image.JPEG
            ) shouldBe "Trixnity Messenger 2021-03-03 03-03.jpeg"
        }
    }
}