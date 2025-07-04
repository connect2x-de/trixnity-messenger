package de.connect2x.trixnity.messenger.util

import io.kotest.matchers.shouldBe
import io.ktor.http.*
import kotlinx.coroutines.test.runTest
import net.folivo.trixnity.utils.readByteArrayFlow
import okio.Path.Companion.toPath
import org.koin.dsl.koinApplication
import kotlin.test.Test


class ImagesTest {
    private val koin = koinApplication {
        modules(platformGetImageDimensionsModule())
    }.koin

    @Test
    fun `determine image dimensions`() = runTest {
        fileSystem.readByteArrayFlow("./src/commonTest/resources/images/cat.jpg".toPath(normalize = true))?.let {
            koin.get<GetImageDimensions>().invoke(it, Long.MAX_VALUE, ContentType.Image.JPEG) shouldBe (640 to 457)
        }
    }
}
