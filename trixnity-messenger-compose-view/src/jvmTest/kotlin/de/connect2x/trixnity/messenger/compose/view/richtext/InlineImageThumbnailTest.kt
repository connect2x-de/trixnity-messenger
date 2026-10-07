package de.connect2x.trixnity.messenger.compose.view.richtext

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.v2.runComposeUiTest
import de.connect2x.trixnity.messenger.MatrixMessengerSettingsHolderImpl
import de.connect2x.trixnity.messenger.compose.view.DI
import de.connect2x.trixnity.messenger.compose.view.i18n.I18nView
import de.connect2x.trixnity.messenger.i18n.DefaultLanguages
import de.connect2x.trixnity.messenger.i18n.GetSystemLang
import de.connect2x.trixnity.messenger.settings.SettingsStorage
import de.connect2x.trixnity.messenger.util.html.HtmlVisitor
import java.awt.image.BufferedImage
import java.io.ByteArrayOutputStream
import javax.imageio.ImageIO
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.datetime.TimeZone
import org.koin.dsl.koinApplication
import org.koin.dsl.module

@OptIn(ExperimentalTestApi::class)
class InlineImageThumbnailTest {
    @Test fun repeatedEmoticonsShareOneSmallThumbnail() = checkSharedThumbnail("data-mx-emoticon='false'", 128)

    @Test fun mixedUsesShareOneLargerThumbnail() = checkSharedThumbnail("", 800)

    private fun checkSharedThumbnail(secondImageAttributes: String, expectedSize: Int) = runComposeUiTest {
        val uri = "mxc://example.org/image"
        val document =
            HtmlVisitor.process(
                "<img src='$uri' alt='thumbnail' data-mx-emoticon><img src='$uri' alt='thumbnail' $secondImageAttributes>"
            )
        val bytes =
            ByteArrayOutputStream()
                .apply { ImageIO.write(BufferedImage(16, 16, BufferedImage.TYPE_INT_RGB), "png", this) }
                .toByteArray()
        val requests = mutableListOf<Pair<String, Int>>()
        val loadImage: suspend (String, Int) -> ByteArray? = { source, size ->
            requests.add(source to size)
            bytes
        }
        val storage =
            object : SettingsStorage {
                override suspend fun read(): String? = null

                override suspend fun write(settings: String) {}
            }
        val di = koinApplication {
            modules(
                module {
                    single {
                        I18nView(
                            DefaultLanguages,
                            MatrixMessengerSettingsHolderImpl(storage),
                            GetSystemLang { "en" },
                            TimeZone.UTC,
                        )
                    }
                }
            )
        }
        try {
            setContent {
                CompositionLocalProvider(DI provides di.koin) {
                    MaterialTheme { RichTextDisplay(document, loadImage = loadImage) }
                }
            }
            waitUntil {
                onAllNodesWithContentDescription("thumbnail", useUnmergedTree = true).fetchSemanticsNodes().size == 2
            }
            runOnIdle { assertEquals(listOf(uri to expectedSize), requests) }
        } finally {
            di.close()
        }
    }
}
