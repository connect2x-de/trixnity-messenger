package de.connect2x.trixnity.messenger.util

import de.connect2x.trixnity.utils.readByteArrayFlow
import io.kotest.matchers.shouldBe
import io.ktor.http.*
import kotlin.test.Test
import kotlinx.coroutines.test.runTest
import okio.Path.Companion.toPath
import org.koin.dsl.koinApplication

class ImagesTest {
    private val koin = koinApplication { modules(platformGetImageDimensionsModule()) }.koin

    @Test
    fun `determine image dimensions`() = runTest {
        fileSystem.readByteArrayFlow("./src/commonTest/resources/images/cat.jpg".toPath(normalize = true))?.let {
            koin.get<GetImageDimensions>().invoke(it, Long.MAX_VALUE, ContentType.Image.JPEG) shouldBe (640 to 457)
        }
    }
}
