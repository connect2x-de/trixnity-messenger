package de.connect2x.trixnity.messenger.compose.view.richtext

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.unit.dp
import de.connect2x.trixnity.messenger.MatrixMessengerSettingsHolderImpl
import de.connect2x.trixnity.messenger.compose.view.DI
import de.connect2x.trixnity.messenger.compose.view.i18n.I18nView
import de.connect2x.trixnity.messenger.compose.view.room.timeline.element.message.TextReply
import de.connect2x.trixnity.messenger.i18n.DefaultLanguages
import de.connect2x.trixnity.messenger.i18n.GetSystemLang
import de.connect2x.trixnity.messenger.settings.SettingsStorage
import de.connect2x.trixnity.messenger.util.UriCaller
import de.connect2x.trixnity.messenger.util.html.HtmlVisitor
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements.TimelineElementMention
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements.message.RoomMessageTimelineElementViewModel
import java.awt.image.BufferedImage
import java.io.ByteArrayOutputStream
import javax.imageio.ImageIO
import kotlin.test.Test
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.datetime.TimeZone
import org.koin.dsl.koinApplication
import org.koin.dsl.module

@OptIn(ExperimentalTestApi::class)
class InlineImageReplyLayoutTest {
    @Test
    fun tallReplyImagesLeaveSurroundingTextVisible() = runComposeUiTest {
        val bytes =
            ByteArrayOutputStream()
                .apply { ImageIO.write(BufferedImage(100, 800, BufferedImage.TYPE_INT_RGB), "png", this) }
                .toByteArray()
        val element =
            object : RoomMessageTimelineElementViewModel.TextBased.Text {
                override val body = "before thumbnail after"
                override val formattedBody = "before <img src='mxc://example.org/image' alt='thumbnail'> after"
                override val formattedBodyContent = HtmlVisitor.process(formattedBody)
                override val mentionsInBody = emptyMap<IntRange, MutableStateFlow<TimelineElementMention?>>()
                override val mentionsInFormattedBody = MutableStateFlow(emptyMap<String, TimelineElementMention?>())
                override val isMentioned = false

                override fun openMention(mention: TimelineElementMention) {}

                override suspend fun loadInlineImage(uri: String, width: Int, height: Int) = bytes
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
                    single<UriCaller> { UriCaller { _, _ -> } }
                }
            )
        }
        var lineHeight = 0f
        try {
            setContent {
                CompositionLocalProvider(DI provides di.koin) {
                    MaterialTheme {
                        lineHeight = with(LocalDensity.current) { MaterialTheme.typography.bodySmall.lineHeight.toPx() }
                        Box(Modifier.width(320.dp).testTag("reply")) { TextReply(element, 2) }
                    }
                }
            }
            waitUntil {
                onAllNodesWithContentDescription("thumbnail", useUnmergedTree = true).fetchSemanticsNodes().isNotEmpty()
            }
            val layouts = mutableListOf<TextLayoutResult>()
            onNodeWithText(element.body, useUnmergedTree = true).performSemanticsAction(
                SemanticsActions.GetTextLayoutResult
            ) {
                it(layouts)
            }
            val layout = layouts.single()
            val image = assertNotNull(layout.placeholderRects.single())
            val bounds = onNodeWithTag("reply").fetchSemanticsNode().boundsInRoot
            assertTrue(
                image.height <= lineHeight + 1f,
                "Reply image height ${image.height} exceeds line height $lineHeight",
            )
            assertTrue(image.height / image.width > 7.5f, "Reply thumbnail must preserve the tall image’s aspect ratio")
            assertTrue(
                layout.getBoundingBox(0).bottom <= bounds.height,
                "Text before the image must fit within the reply preview",
            )
            assertTrue(
                layout.getBoundingBox(element.body.lastIndex).bottom <= bounds.height,
                "Text after the image must fit within the reply preview",
            )
        } finally {
            di.close()
        }
    }
}
