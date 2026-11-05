package de.connect2x.trixnity.messenger.compose.view.room.timeline.element

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.v2.runComposeUiTest
import java.awt.image.BufferedImage
import java.io.ByteArrayOutputStream
import javax.imageio.ImageIO
import kotlin.test.Test
import kotlinx.coroutines.flow.MutableStateFlow

@OptIn(ExperimentalTestApi::class)
class MessageReactionDisplayTest {
    private val uri = "mxc://example.org/party"

    @Test
    fun rendersImageWithShortcodeAsDescription() = runComposeUiTest {
        val bytes =
            ByteArrayOutputStream()
                .apply { ImageIO.write(BufferedImage(32, 16, BufferedImage.TYPE_INT_RGB), "png", this) }
                .toByteArray()
        val image = MutableStateFlow<ByteArray?>(null)
        setContent { MaterialTheme { MessageReactionDisplay(rememberReaction(uri, ":party:", image)) } }
        onNodeWithText(":party:").assertExists()
        image.value = bytes
        waitUntil { onAllNodesWithContentDescription(":party:").fetchSemanticsNodes().isNotEmpty() }
        onNodeWithContentDescription(":party:").assertExists()
        onNodeWithText(":party:").assertDoesNotExist()
    }

    @Test
    fun fallsBackToShortcodeWhenImageIsUnavailable() = runComposeUiTest {
        setContent {
            MaterialTheme { MessageReactionDisplay(rememberReaction(uri, ":party:", MutableStateFlow(null))) }
        }
        waitForIdle()
        onNodeWithText(":party:").assertExists()
    }

    @Test
    fun fallsBackToKeyWhenImageCannotBeDecodedAndHasNoShortcode() = runComposeUiTest {
        setContent {
            MaterialTheme {
                MessageReactionDisplay(rememberReaction(uri, image = MutableStateFlow(byteArrayOf(1, 2, 3))))
            }
        }
        waitForIdle()
        onNodeWithText(uri).assertExists()
    }

    @Test
    fun unicodeReactionsIgnoreShortcodes() = runComposeUiTest {
        setContent { MaterialTheme { MessageReactionDisplay(rememberReaction("🎉", ":party:")) } }
        onNodeWithText("🎉").assertExists()
    }

    @Test
    fun externalUrlsRemainText() = runComposeUiTest {
        val url = "https://example.org/party.png"
        setContent { MaterialTheme { MessageReactionDisplay(rememberReaction(url, ":party:")) } }
        onNodeWithText(url).assertExists()
    }
}
